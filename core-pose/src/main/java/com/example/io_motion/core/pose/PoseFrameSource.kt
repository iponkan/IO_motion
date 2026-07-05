package com.example.io_motion.core.pose

import android.content.Context
import android.util.Log
import androidx.camera.core.CameraSelector
import androidx.camera.core.ImageAnalysis
import androidx.camera.core.Preview
import androidx.camera.lifecycle.ProcessCameraProvider
import androidx.core.content.ContextCompat
import androidx.lifecycle.LifecycleOwner
import com.example.io_motion.core.pose.config.PoseLandmarkerConfig
import com.example.io_motion.core.pose.converter.PoseResultConverter
import com.example.io_motion.core.pose.model.PoseError
import com.example.io_motion.core.pose.model.PoseFrameResult
import com.google.mediapipe.tasks.vision.poselandmarker.PoseLandmarkerResult
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.channels.BufferOverflow
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.SharedFlow
import kotlinx.coroutines.flow.asSharedFlow
import java.util.concurrent.Executors
import javax.inject.Inject

/**
 * Combines CameraX and [PoseLandmarkerHelper] into a single observable data source.
 *
 * ## Usage
 * 1. Inject or create this class in your ViewModel.
 * 2. Call [bindCamera] once the UI is ready (pass the [LifecycleOwner] and optional
 *    [Preview.SurfaceProvider] from the `PreviewView`).
 * 3. Collect [frames] in the ViewModel or directly in the composable via `collectAsState`.
 * 4. Call [unbindCamera] or let the [LifecycleOwner] do so via CameraX lifecycle management.
 * 5. Call [updateConfig] to switch model variant or delegate at runtime without rebinding.
 *
 * All inference runs on a dedicated single-thread executor. Results are emitted on that thread
 * and delivered to [frames] — downstream collectors receive them on whatever dispatcher they use.
 */
class PoseFrameSource @Inject constructor(
    @ApplicationContext private val context: Context,
) {
    private val _frames = MutableSharedFlow<PoseFrameResult>(
        replay = 0,
        extraBufferCapacity = 1,
        onBufferOverflow = BufferOverflow.DROP_OLDEST,
    )

    /** Shared flow of [PoseFrameResult] emitted at the camera frame rate. */
    val frames: SharedFlow<PoseFrameResult> = _frames.asSharedFlow()

    private val _errors = MutableSharedFlow<PoseError>(
        replay = 0,
        extraBufferCapacity = 4,
        onBufferOverflow = BufferOverflow.DROP_OLDEST,
    )

    /**
     * Shared flow of [PoseError]s from the underlying landmarker (e.g. failed to initialize
     * on both GPU and CPU, or a runtime detection error). Previously these were only [Log.e]'d,
     * so a fatal setup failure left the user staring at a camera preview with no skeleton and
     * no explanation — collect this to surface it.
     */
    val errors: SharedFlow<PoseError> = _errors.asSharedFlow()

    private val analysisExecutor = Executors.newSingleThreadExecutor()
    private val fpsTracker = FpsTracker()

    // Written only on [analysisExecutor] (serialized with [PoseImageAnalyzer.analyze]) after the
    // initial synchronous setup in [bindUseCases]; @Volatile gives the analyzer thread a
    // consistent view without needing to read through the executor itself.
    @Volatile private var poseLandmarkerHelper: PoseLandmarkerHelper? = null
    private var cameraProvider: ProcessCameraProvider? = null
    private var currentConfig: PoseLandmarkerConfig = PoseLandmarkerConfig()

    // Result listener is stored so it survives config updates without rebinding the camera.
    private val resultListener = object : PoseLandmarkerHelper.ResultListener {
        override fun onResults(
            result: PoseLandmarkerResult,
            frameTimestampMs: Long,
            inferenceTimeMs: Long,
        ) {
            val poseFrame = PoseResultConverter.convert(result, frameTimestampMs)
            fpsTracker.onFrame(frameTimestampMs)
            _frames.tryEmit(PoseFrameResult(poseFrame, inferenceTimeMs, fpsTracker.fps))
        }

        override fun onError(message: String, isFatal: Boolean) {
            Log.e(TAG, "PoseLandmarker error (fatal=$isFatal): $message")
            _errors.tryEmit(PoseError(message, isFatal))
        }
    }

    /**
     * Binds the camera and starts streaming [PoseFrameResult] into [frames].
     *
     * Must be called on the main thread (CameraX requirement for `bindToLifecycle`).
     *
     * @param lifecycleOwner Ties camera and pose processing to this lifecycle. CameraX will
     *   unbind automatically on [LifecycleOwner.getLifecycle] stop.
     * @param config Initial [PoseLandmarkerConfig] for this session.
     * @param surfaceProvider If non-null, a `Preview` use case is also bound so the camera
     *   feed appears in the UI. Obtain via `PreviewView.surfaceProvider`.
     * @param lensFacing [CameraSelector.LENS_FACING_FRONT] or [CameraSelector.LENS_FACING_BACK].
     */
    fun bindCamera(
        lifecycleOwner: LifecycleOwner,
        config: PoseLandmarkerConfig = PoseLandmarkerConfig(),
        surfaceProvider: Preview.SurfaceProvider? = null,
        lensFacing: Int = CameraSelector.LENS_FACING_FRONT,
    ) {
        currentConfig = config
        val cameraProviderFuture = ProcessCameraProvider.getInstance(context)
        cameraProviderFuture.addListener({
            try {
                val provider = cameraProviderFuture.get()
                cameraProvider = provider
                bindUseCases(lifecycleOwner, provider, surfaceProvider, lensFacing)
            } catch (e: Exception) {
                Log.e(TAG, "Camera provider failed: ${e.message}", e)
            }
        }, ContextCompat.getMainExecutor(context))
    }

    /**
     * Switches the active model variant or delegate without rebinding the camera.
     * The previous landmarker is closed and a new one is created with [config].
     *
     * Runs on [analysisExecutor] so it is serialized with in-flight [PoseImageAnalyzer.analyze]
     * calls — otherwise closing the old helper here (main thread) could race a `detectAsync`
     * call still running on the analyzer thread.
     */
    fun updateConfig(config: PoseLandmarkerConfig) {
        currentConfig = config
        analysisExecutor.execute { recreateLandmarker(config) }
    }

    /** Unbinds the camera and releases the landmarker. */
    fun unbindCamera() {
        cameraProvider?.unbindAll()
        analysisExecutor.execute {
            poseLandmarkerHelper?.close()
            poseLandmarkerHelper = null
        }
        fpsTracker.reset()
    }

    // ──────────────────────────────────────────────
    // Private
    // ──────────────────────────────────────────────

    private fun bindUseCases(
        lifecycleOwner: LifecycleOwner,
        provider: ProcessCameraProvider,
        surfaceProvider: Preview.SurfaceProvider?,
        lensFacing: Int,
    ) {
        recreateLandmarker(currentConfig)

        val cameraSelector = CameraSelector.Builder()
            .requireLensFacing(lensFacing)
            .build()

        val imageAnalysis = ImageAnalysis.Builder()
            .setBackpressureStrategy(ImageAnalysis.STRATEGY_KEEP_ONLY_LATEST)
            .setOutputImageFormat(ImageAnalysis.OUTPUT_IMAGE_FORMAT_RGBA_8888)
            .build()
            .also { analysis ->
                analysis.setAnalyzer(analysisExecutor, PoseImageAnalyzer { poseLandmarkerHelper })
            }

        val useCases = buildList {
            if (surfaceProvider != null) {
                add(Preview.Builder().build().also { it.setSurfaceProvider(surfaceProvider) })
            }
            add(imageAnalysis)
        }

        provider.unbindAll()
        provider.bindToLifecycle(lifecycleOwner, cameraSelector, *useCases.toTypedArray())
        Log.i(TAG, "Camera bound: lens=$lensFacing model=${currentConfig.modelVariant.displayName}")
    }

    private fun recreateLandmarker(config: PoseLandmarkerConfig) {
        poseLandmarkerHelper?.close()
        val helper = PoseLandmarkerHelper(context, config, resultListener)
        helper.setup()
        poseLandmarkerHelper = helper
    }

    private companion object {
        const val TAG = "PoseFrameSource"
    }
}
