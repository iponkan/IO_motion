package com.example.io_motion.feature.live

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.io_motion.core.common.util.parseEnumOrDefault
import com.example.io_motion.core.pose.model.PoseModelVariant
import com.example.io_motion.data.repository.SettingsRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.stateIn
import javax.inject.Inject

/**
 * Home no longer lets the user pick a model variant per-session (moved to Settings) — it just
 * needs the persisted default to pass along when starting an analysis.
 */
@HiltViewModel
class HomeViewModel @Inject constructor(
    settingsRepository: SettingsRepository,
) : ViewModel() {

    val modelVariant: StateFlow<PoseModelVariant> = settingsRepository.defaultModelVariant
        .map { raw -> parseEnumOrDefault(raw, PoseModelVariant.FULL) }
        .stateIn(
            scope = viewModelScope,
            started = SharingStarted.Eagerly,
            initialValue = PoseModelVariant.FULL,
        )
}
