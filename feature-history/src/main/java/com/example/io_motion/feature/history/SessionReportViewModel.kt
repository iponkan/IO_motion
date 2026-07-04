package com.example.io_motion.feature.history

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.io_motion.core.export.SessionCsvExporter
import com.example.io_motion.core.export.SessionJsonExporter
import com.example.io_motion.data.repository.SessionRepository
import com.example.io_motion.feature.history.model.SessionReportUiState
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.channels.Channel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.receiveAsFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import javax.inject.Inject

@HiltViewModel
class SessionReportViewModel @Inject constructor(
    private val repository: SessionRepository,
) : ViewModel() {

    private val _uiState = MutableStateFlow(SessionReportUiState())
    val uiState: StateFlow<SessionReportUiState> = _uiState.asStateFlow()

    private val _shareEvents = Channel<ShareContent>(Channel.BUFFERED)
    val shareEvents = _shareEvents.receiveAsFlow()

    /** Loads the session once; subsequent calls on the same ViewModel instance are no-ops. */
    fun load(sessionId: Long) {
        if (_uiState.value.record != null) return
        viewModelScope.launch {
            val record = repository.getById(sessionId)
            _uiState.update { it.copy(record = record, isLoading = false) }
        }
    }

    fun exportJson() {
        val record = _uiState.value.record ?: return
        viewModelScope.launch {
            val content = SessionJsonExporter.export(
                id = record.id,
                recordedAt = record.recordedAt,
                analysisMode = record.analysisMode.name,
                modelVariant = record.modelVariant,
                metrics = record.metrics,
            )
            _shareEvents.send(ShareContent(content, "application/json"))
        }
    }

    fun exportCsv() {
        val record = _uiState.value.record ?: return
        viewModelScope.launch {
            val content = SessionCsvExporter.export(
                id = record.id,
                recordedAt = record.recordedAt,
                analysisMode = record.analysisMode.name,
                modelVariant = record.modelVariant,
                metrics = record.metrics,
            )
            _shareEvents.send(ShareContent(content, "text/csv"))
        }
    }
}

data class ShareContent(val text: String, val mimeType: String)
