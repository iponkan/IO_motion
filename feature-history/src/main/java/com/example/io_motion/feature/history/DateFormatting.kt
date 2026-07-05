package com.example.io_motion.feature.history

import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

/** Previously duplicated identically in HistoryScreen.kt and SessionReportScreen.kt. */
internal fun Long.toDateString(): String =
    SimpleDateFormat("MMM d, yyyy  HH:mm", Locale.getDefault()).format(Date(this))
