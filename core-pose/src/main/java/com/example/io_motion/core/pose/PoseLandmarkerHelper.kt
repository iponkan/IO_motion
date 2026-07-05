package com.example.io_motion.core.pose

import android.content.Context
import android.os.SystemClock
import android.util.Log
import com.example.io_motion.core.pose.config.PoseLandmarkerConfig
import com.example.io_motion.core.pose.model.PoseDelegate
import com.google.mediapipe.framework.image.MPImage
import com.google.mediapipe.tasks.core.BaseOptions
import com.google.mediapipe.tasks.core.Delegate as MpDelegate
import com.google.mediapipe.tasks.vision.core.RunningMode
import com.google.mediapipe.tasks.vision.poselandmarker.PoseLandmarker
import com.google.mediapipe.tasks.vision.poselandmarker.PoseLandmarkerResult

/**
 * Thin wrapper around [PoseLandmarker] for `LIVE_STREAM` mode.
 *
 * Initialization attempts GPU delegation first and automatically retries with CPU if GPU
 * fails. GPU delegate failures surface in two different ways depending on the device:
 *  - Synchronously, thrown directly out of [PoseLandmarker.createFromOptions] — handled in
 *    [setup].
 *  - Asynchronously, reported via the MediaPipe error listener at first inference — many GPU
 *    driver incompatibilities only manifest once the delegate actually runs. Handled in the
 *    error listener installed in [createLandmarker].
 *
 * [poseLandmarker], [isClosed], and [hasFallenBackToCpu] are all guarded by [lock] because they
 * can be touched from three different threads: whichever thread calls [setup]/[detectAsync]/
 * [close] (the app's analysis executor), and the MediaPipe-internal thread that invokes the
 * error listener.
 *
 * All calls to [detectAsync] are non-blocking; results arrive via [ResultListener].
 */
internal class PoseLandmarkerHelper(
    private val context: Context,
    private var config: PoseLandmarkerConfig,
    private val listener: ResultListener,
) {
    interface ResultListener {
        fun onResults(
            result: PoseLandmarkerResult,
            frameTimestampMs: Long,
            inferenceTimeMs: Long,
        )
        fun onError(message: String, isFatal: Boolean = false)
    }

    private val lock = Any()
    private var poseLandmarker: PoseLandmarker? = null
    private var isClosed = false
    private var hasFallenBackToCpu = false

    /** Creates the [PoseLandmarker]. Must be called before [detectAsync]. */
    fun setup() {
        try {
            createLandmarker(config)
        } catch (gpuException: Exception) {
            Log.w(TAG, "GPU delegate failed synchronously (${gpuException.message}), retrying with CPU")
            fallBackToCpu()
        }
    }

    /**
     * Submits a frame for asynchronous inference.
     * Results arrive via [ResultListener.onResults] on a MediaPipe-internal thread.
     *
     * @param mpImage The frame image.
     * @param frameTimestampMs Monotonically increasing timestamp in milliseconds.
     *   Passing a timestamp ≤ the previous one will cause MediaPipe to drop the frame.
     */
    fun detectAsync(mpImage: MPImage, frameTimestampMs: Long) {
        synchronized(lock) { poseLandmarker }?.detectAsync(mpImage, frameTimestampMs)
    }

    /** Closes and releases the underlying [PoseLandmarker]. Safe to call multiple times. */
    fun close() {
        synchronized(lock) {
            isClosed = true
            poseLandmarker?.close()
            poseLandmarker = null
        }
    }

    /** Returns `true` if the landmarker has been successfully initialized. */
    val isReady: Boolean get() = synchronized(lock) { poseLandmarker != null }

    // ──────────────────────────────────────────────
    // Private
    // ──────────────────────────────────────────────

    private fun createLandmarker(cfg: PoseLandmarkerConfig) {
        val mpDelegate = when (cfg.delegate) {
            PoseDelegate.GPU -> MpDelegate.GPU
            PoseDelegate.CPU -> MpDelegate.CPU
        }

        val baseOptions = BaseOptions.builder()
            .setModelAssetPath(cfg.modelVariant.assetFileName)
            .setDelegate(mpDelegate)
            .build()

        val options = PoseLandmarker.PoseLandmarkerOptions.builder()
            .setBaseOptions(baseOptions)
            .setNumPoses(cfg.numPoses)
            .setMinPoseDetectionConfidence(cfg.minPoseDetectionConfidence)
            .setMinPosePresenceConfidence(cfg.minPosePresenceConfidence)
            .setMinTrackingConfidence(cfg.minTrackingConfidence)
            .setRunningMode(RunningMode.LIVE_STREAM)
            .setResultListener { result, _ ->
                // result.timestampMs() echoes back the timestamp passed to detectAsync for this
                // specific result, so it stays correctly paired even when LIVE_STREAM results
                // arrive out of submission order under load — unlike a single shared "last
                // submitted" timestamp, which would misattribute both fields.
                val frameTimestampMs = result.timestampMs()
                val inferenceTime = SystemClock.uptimeMillis() - frameTimestampMs
                listener.onResults(result, frameTimestampMs, inferenceTime)
            }
            .setErrorListener { error ->
                val shouldFallBack = cfg.delegate == PoseDelegate.GPU && synchronized(lock) {
                    if (isClosed || hasFallenBackToCpu) {
                        false
                    } else {
                        hasFallenBackToCpu = true
                        true
                    }
                }
                if (shouldFallBack) {
                    // Run the retry on its own thread rather than inline on this MediaPipe-internal
                    // callback thread — closing and recreating the landmarker isn't something this
                    // thread should re-enter.
                    Log.w(TAG, "GPU delegate failed asynchronously (${error.message}), retrying with CPU")
                    Thread({ fallBackToCpu() }, "PoseLandmarker-CpuFallback").start()
                } else {
                    listener.onError(error.message ?: "Unknown MediaPipe error")
                }
            }
            .build()

        val created = PoseLandmarker.createFromOptions(context, options)
        synchronized(lock) {
            // close() may have run concurrently while createFromOptions() was in flight (e.g. the
            // screen was left mid-fallback); don't resurrect a landmarker after that.
            if (isClosed) created.close() else poseLandmarker = created
        }
        Log.i(TAG, "PoseLandmarker initialized: model=${cfg.modelVariant.displayName} delegate=${cfg.delegate}")
    }

    private fun fallBackToCpu() {
        synchronized(lock) {
            hasFallenBackToCpu = true
            poseLandmarker?.close()
            poseLandmarker = null
        }
        config = config.copy(delegate = PoseDelegate.CPU)
        try {
            createLandmarker(config)
        } catch (cpuException: Exception) {
            listener.onError("Failed to initialize PoseLandmarker: ${cpuException.message}", isFatal = true)
        }
    }

    private companion object {
        const val TAG = "PoseLandmarkerHelper"
    }
}
