package com.example.io_motion.feature.video.gallery

import android.content.ContentUris
import android.content.Context
import android.provider.MediaStore
import dagger.hilt.android.qualifiers.ApplicationContext
import javax.inject.Inject
import javax.inject.Singleton

/**
 * Queries MediaStore directly for every video the app can see, across all albums and app
 * folders (Camera, Downloads, WhatsApp, etc.) — unlike the system Photo Picker, which only
 * surfaces a narrower, OEM-dependent subset of indexed albums.
 *
 * Requires `READ_MEDIA_VIDEO` (API 33+) or `READ_EXTERNAL_STORAGE` (below) to already be
 * granted; callers are responsible for the permission request.
 */
@Singleton
class DeviceVideoRepository @Inject constructor(
    @ApplicationContext private val context: Context,
) {
    /** Blocking MediaStore query — call from a background dispatcher. */
    fun queryVideos(): List<DeviceVideo> {
        val collection = MediaStore.Video.Media.EXTERNAL_CONTENT_URI
        val projection = arrayOf(
            MediaStore.Video.Media._ID,
            MediaStore.Video.Media.DISPLAY_NAME,
            MediaStore.Video.Media.DURATION,
            MediaStore.Video.Media.DATE_ADDED,
        )
        val videos = mutableListOf<DeviceVideo>()
        context.contentResolver.query(
            collection,
            projection,
            null,
            null,
            "${MediaStore.Video.Media.DATE_ADDED} DESC",
        )?.use { cursor ->
            val idCol = cursor.getColumnIndexOrThrow(MediaStore.Video.Media._ID)
            val nameCol = cursor.getColumnIndexOrThrow(MediaStore.Video.Media.DISPLAY_NAME)
            val durationCol = cursor.getColumnIndexOrThrow(MediaStore.Video.Media.DURATION)
            val dateCol = cursor.getColumnIndexOrThrow(MediaStore.Video.Media.DATE_ADDED)
            while (cursor.moveToNext()) {
                val id = cursor.getLong(idCol)
                videos += DeviceVideo(
                    id = id,
                    uri = ContentUris.withAppendedId(collection, id),
                    displayName = cursor.getString(nameCol) ?: "",
                    durationMs = cursor.getLong(durationCol),
                    dateAddedSec = cursor.getLong(dateCol),
                )
            }
        }
        return videos
    }
}
