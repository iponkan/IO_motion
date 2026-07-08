package com.example.io_motion.feature.video.gallery

import android.net.Uri

/** A single video entry read from [MediaStore.Video][android.provider.MediaStore.Video]. */
data class DeviceVideo(
    val id: Long,
    val uri: Uri,
    val displayName: String,
    val durationMs: Long,
    val dateAddedSec: Long,
)
