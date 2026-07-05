package com.example.io_motion.core.pose

import android.graphics.Bitmap
import android.graphics.Matrix
import android.os.SystemClock
import androidx.camera.core.ImageAnalysis
import androidx.camera.core.ImageProxy
import com.google.mediapipe.framework.image.BitmapImageBuilder

/**
 * CameraX [ImageAnalysis.Analyzer] that converts each RGBA_8888 [ImageProxy] into a
 * [com.google.mediapipe.framework.image.MPImage] and submits it to [PoseLandmarkerHelper].
 *
 * The `ImageAnalysis` use case must be configured with
 * `setOutputImageFormat(ImageAnalysis.OUTPUT_IMAGE_FORMAT_RGBA_8888)` so that
 * `planes[0].buffer` contains packed ARGB_8888 pixel data that can be loaded directly
 * into a [Bitmap] without a YUV conversion step. This avoids the `@ExperimentalGetImage`
 * requirement that would come with accessing `imageProxy.image`.
 */
internal class PoseImageAnalyzer(
    private val landmarkerHelperProvider: () -> PoseLandmarkerHelper?,
) : ImageAnalysis.Analyzer {

    override fun analyze(imageProxy: ImageProxy) {
        val landmarkerHelper = landmarkerHelperProvider()
        if (landmarkerHelper == null || !landmarkerHelper.isReady) {
            imageProxy.close()
            return
        }

        // Copy pixel data into a Bitmap before closing the proxy.
        val bitmapBuffer = Bitmap.createBitmap(
            imageProxy.width, imageProxy.height, Bitmap.Config.ARGB_8888
        )
        bitmapBuffer.copyPixelsFromBuffer(imageProxy.planes[0].buffer)

        val rotationDegrees = imageProxy.imageInfo.rotationDegrees
        imageProxy.close()  // safe to close after pixel copy

        val finalBitmap = if (rotationDegrees != 0) {
            Bitmap.createBitmap(
                bitmapBuffer, 0, 0, bitmapBuffer.width, bitmapBuffer.height,
                Matrix().apply { postRotate(rotationDegrees.toFloat()) },
                true,
            ).also { bitmapBuffer.recycle() }
        } else {
            bitmapBuffer
        }

        val mpImage = BitmapImageBuilder(finalBitmap).build()
        // Monotonically increasing timestamp in ms for LIVE_STREAM mode.
        landmarkerHelper.detectAsync(mpImage, SystemClock.uptimeMillis())
    }
}
