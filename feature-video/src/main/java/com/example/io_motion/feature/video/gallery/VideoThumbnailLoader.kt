package com.example.io_motion.feature.video.gallery

import android.content.Context
import android.graphics.Bitmap
import android.os.Build
import android.provider.MediaStore
import android.util.Size
import java.io.IOException

/**
 * Decodes a small preview [Bitmap] for [video], using the modern MediaStore thumbnail API on
 * API 29+ and falling back to the legacy [MediaStore.Video.Thumbnails] table below that (still
 * functional, just superseded). Returns `null` if the file is unreadable or was deleted after
 * being indexed.
 */
internal fun loadVideoThumbnail(context: Context, video: DeviceVideo): Bitmap? = try {
    if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
        context.contentResolver.loadThumbnail(video.uri, Size(THUMBNAIL_DIM, THUMBNAIL_DIM), null)
    } else {
        @Suppress("DEPRECATION")
        MediaStore.Video.Thumbnails.getThumbnail(
            context.contentResolver,
            video.id,
            MediaStore.Video.Thumbnails.MINI_KIND,
            null,
        )
    }
} catch (e: IOException) {
    null
} catch (e: SecurityException) {
    null
}

private const val THUMBNAIL_DIM = 300
