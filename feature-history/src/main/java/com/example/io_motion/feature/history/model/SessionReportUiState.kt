package com.example.io_motion.feature.history.model

import com.example.io_motion.data.model.SessionRecord

data class SessionReportUiState(
    val record: SessionRecord? = null,
    val isLoading: Boolean = true,
)
