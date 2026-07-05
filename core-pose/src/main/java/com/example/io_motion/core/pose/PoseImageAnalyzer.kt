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
 *
 * `planes[0].rowStride` is not always `width * pixelStride` — some devices pad each row to an
 * alignment boundary — so [analyze] decodes into the padded width and crops rather than
 * assuming a tightly packed buffer, which would otherwise skew the image diagonally.
 *
 * A fresh [Bitmap] is allocated for every frame handed to [BitmapImageBuilder] — it must not be
 * reused across calls. `BitmapImageBuilder` wraps the [Bitmap] *by reference*, and MediaPipe's
 * `LIVE_STREAM` graph converts it to its internal image format on its own thread, which can run
 * after `detectAsync` returns. Reusing the same `Bitmap` object for the next frame would let
 * `copyPixelsFromBuffer` overwrite pixels MediaPipe is still reading, corrupting memory (this
 * previously caused a native SIGSEGV within the first few frames of opening the Live screen).
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

        val plane = imageProxy.planes[0]
        val pixelStride = plane.pixelStride // bytes per pixel; 4 for RGBA_8888
        val rowStride = plane.rowStride
        val rowPaddingBytes = rowStride - pixelStride * imageProxy.width

        val bitmapBuffer = if (rowPaddingBytes == 0) {
            Bitmap.createBitmap(imageProxy.width, imageProxy.height, Bitmap.Config.ARGB_8888).also {
                it.copyPixelsFromBuffer(plane.buffer)
            }
        } else {
            // rowStride includes padding: decode into a bitmap wide enough for the padded rows,
            // then crop to the true image width so downstream consumers never see the padding.
            val paddedWidth = rowStride / pixelStride
            val padded = Bitmap.createBitmap(paddedWidth, imageProxy.height, Bitmap.Config.ARGB_8888).also {
                it.copyPixelsFromBuffer(plane.buffer)
            }
            Bitmap.createBitmap(padded, 0, 0, imageProxy.width, imageProxy.height).also { padded.recycle() }
        }

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
