package com.example.io_motion.feature.history

import android.content.Context
import android.net.Uri
import androidx.core.content.FileProvider
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.io_motion.core.export.SessionCsvExporter
import com.example.io_motion.core.export.SessionJsonExporter
import com.example.io_motion.data.repository.SessionRepository
import com.example.io_motion.feature.history.model.SessionReportUiState
import dagger.hilt.android.lifecycle.HiltViewModel
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.channels.Channel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.receiveAsFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.io.File
import javax.inject.Inject

@HiltViewModel
class SessionReportViewModel @Inject constructor(
    private val repository: SessionRepository,
    @ApplicationContext private val context: Context,
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
            share(content, "session_${record.id}.json", "application/json")
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
            share(content, "session_${record.id}.csv", "text/csv")
        }
    }

    /**
     * Writes [content] to a cache file and shares it as a FileProvider `content://` Uri.
     *
     * Plain ACTION_SEND with only EXTRA_TEXT (the previous approach) is unreliable for
     * non-text/plain MIME types: many receivers (Drive, Gmail attachments, file managers) read
     * EXTRA_STREAM instead and show empty content when only EXTRA_TEXT is set.
     */
    private suspend fun share(content: String, fileName: String, mimeType: String) {
        val uri = withContext(Dispatchers.IO) {
            val exportsDir = File(context.cacheDir, "exports").apply { mkdirs() }
            val file = File(exportsDir, fileName)
            file.writeText(content)
            FileProvider.getUriForFile(context, "${context.packageName}.fileprovider", file)
        }
        _shareEvents.send(ShareContent(uri, mimeType))
    }
}

data class ShareContent(val uri: Uri, val mimeType: String)
