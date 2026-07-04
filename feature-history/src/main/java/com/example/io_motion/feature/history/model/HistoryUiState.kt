package com.example.io_motion.feature.history.model

import com.example.io_motion.data.model.SessionRecord

data class HistoryUiState(
    val sessions: List<SessionRecord> = emptyList(),
    val isLoading: Boolean = true,
)
