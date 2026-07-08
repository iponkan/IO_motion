package com.example.io_motion.core.pose

import android.content.Context
import android.graphics.Bitmap
import android.media.MediaMetadataRetriever
import android.net.Uri
import android.util.Log
import com.example.io_motion.core.common.models.PoseFrame
import com.example.io_motion.core.pose.config.PoseLandmarkerConfig
import com.example.io_motion.core.pose.converter.PoseResultConverter
import com.google.mediapipe.framework.image.BitmapImageBuilder
import com.google.mediapipe.tasks.core.BaseOptions
import com.google.mediapipe.tasks.core.Delegate as MpDelegate
import com.google.mediapipe.tasks.vision.core.RunningMode
import com.google.mediapipe.tasks.vision.poselandmarker.PoseLandmarker
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.currentCoroutineContext
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.flow
import kotlinx.coroutines.flow.flowOn
import kotlinx.coroutines.isActive
import java.io.IOException
import javax.inject.Inject
import javax.inject.Singleton

/**
 * Offline video analysis pipeline using MediaPipe [RunningMode.VIDEO].
 *
 * Extracts frames at [FRAME_INTERVAL_MS] intervals via [MediaMetadataRetriever],
 * runs synchronous [PoseLandmarker.detectForVideo] on each, and emits progress events.
 *
 * GPU delegation is intentionally avoided — MediaPipe VIDEO mode requires strictly
 * monotonic timestamps which the GPU delegate does not reliably support on all devices.
 *
 * A fresh [PoseLandmarker] is created and released per [process] call, so this class
 * carries no between-call state and is safe to use as a singleton.
 */
@Singleton
class VideoAnalysisSession @Inject constructor(
    @ApplicationContext private val context: Context,
) {
    sealed interface ProgressEvent {
        /** Emitted for each extracted frame. [poseFrame] is null when no pose was detected. */
        data class Frame(val progress: Float, val poseFrame: PoseFrame?) : ProgressEvent
        data class Complete(val framesAnalyzed: Int) : ProgressEvent
        data class Error(val message: String) : ProgressEvent
    }

    fun process(uri: Uri, config: PoseLandmarkerConfig): Flow<ProgressEvent> = flow {
        val retriever = MediaMetadataRetriever()
        var landmarker: PoseLandmarker? = null
        try {
            retriever.setDataSource(context, uri)

            val durationMs = retriever
                .extractMetadata(MediaMetadataRetriever.METADATA_KEY_DURATION)
                ?.toLongOrNull() ?: 0L

            if (durationMs == 0L) {
                emit(ProgressEvent.Error("Could not read video duration"))
                return@flow
            }

            landmarker = createLandmarker(config)

            val totalFrames = (durationMs / FRAME_INTERVAL_MS).toInt().coerceAtLeast(1)
            var framesAnalyzed = 0
            var timestampMs = 0L

            while (timestampMs <= durationMs && currentCoroutineContext().isActive) {
                // OPTION_CLOSEST decodes the actual frame nearest this timestamp. OPTION_CLOSEST_SYNC
                // would instead snap to the nearest sync/key frame (typically 1-2s apart), causing
                // the same bitmap to be re-analyzed many times in a row between keyframes and making
                // rep motion invisible to the analyzer.
                val rawBitmap = retriever.getFrameAtTime(
                    timestampMs * 1_000L, // ms → µs
                    MediaMetadataRetriever.OPTION_CLOSEST,
                )
                val bitmap = rawBitmap?.ensureArgb8888()?.scaleIfNeeded()

                val poseFrame: PoseFrame? = if (bitmap != null) {
                    val mpImage = BitmapImageBuilder(bitmap).build()
                    val result = landmarker.detectForVideo(mpImage, timestampMs)
                    PoseResultConverter.convert(result, timestampMs)
                } else null

                framesAnalyzed++
                emit(
                    ProgressEvent.Frame(
                        progress = (framesAnalyzed.toFloat() / totalFrames).coerceIn(0f, 1f),
                        poseFrame = poseFrame,
                    )
                )
                timestampMs += FRAME_INTERVAL_MS
            }

            emit(ProgressEvent.Complete(framesAnalyzed))
        } catch (e: kotlinx.coroutines.CancellationException) {
            throw e
        } catch (e: Exception) {
            // e.message often exposes internal paths or MediaPipe/MediaMetadataRetriever status
            // codes that are meaningless to a user — log the real cause, surface a plain one.
            Log.e(TAG, "Video analysis failed", e)
            emit(ProgressEvent.Error("Video analysis failed. Please try a different video."))
        } finally {
            landmarker?.close()
            retriever.release()
        }
    }.flowOn(Dispatchers.Default)

    // MediaMetadataRetriever.getFrameAtTime's bitmap config depends on the device/codec (often
    // RGB_565), but MediaPipe's BitmapImageBuilder requires ARGB_8888.
    private fun Bitmap.ensureArgb8888(): Bitmap =
        if (config == Bitmap.Config.ARGB_8888) this else copy(Bitmap.Config.ARGB_8888, false)

    private fun Bitmap.scaleIfNeeded(): Bitmap {
        if (width <= MAX_BITMAP_DIM && height <= MAX_BITMAP_DIM) return this
        val scale = MAX_BITMAP_DIM.toFloat() / maxOf(width, height)
        return Bitmap.createScaledBitmap(
            this,
            (width * scale).toInt().coerceAtLeast(1),
            (height * scale).toInt().coerceAtLeast(1),
            true,
        )
    }

    private fun createLandmarker(config: PoseLandmarkerConfig): PoseLandmarker {
        // A missing model asset has been observed to crash the process with a native SIGSEGV
        // inside PoseLandmarker.createFromOptions below rather than throwing a catchable error
        // (see PoseLandmarkerHelper.assertModelAssetExists) — the outer try/catch in process()
        // can't catch a native crash, so this must fail fast with an ordinary exception first.
        try {
            context.assets.open(config.modelVariant.assetFileName).close()
        } catch (e: IOException) {
            throw IllegalStateException(
                "Model asset not found: \"${config.modelVariant.assetFileName}\". Run " +
                    "scripts/download_models.sh and confirm the .task files are under " +
                    "core-pose/src/main/assets/models/.",
                e,
            )
        }

        val baseOptions = BaseOptions.builder()
            .setModelAssetPath(config.modelVariant.assetFileName)
            .setDelegate(MpDelegate.CPU)
            .build()
        val options = PoseLandmarker.PoseLandmarkerOptions.builder()
            .setBaseOptions(baseOptions)
            .setNumPoses(config.numPoses)
            .setMinPoseDetectionConfidence(config.minPoseDetectionConfidence)
            .setMinPosePresenceConfidence(config.minPosePresenceConfidence)
            .setMinTrackingConfidence(config.minTrackingConfidence)
            .setRunningMode(RunningMode.VIDEO)
            .build()
        return PoseLandmarker.createFromOptions(context, options)
    }

    private companion object {
        const val TAG = "VideoAnalysisSession"
        const val FRAME_INTERVAL_MS = 66L  // ~15 FPS
        const val MAX_BITMAP_DIM = 720
    }
}
