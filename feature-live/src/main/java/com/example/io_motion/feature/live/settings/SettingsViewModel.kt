package com.example.io_motion.feature.live.settings

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.io_motion.core.common.models.AccentTheme
import com.example.io_motion.core.common.models.ThemeMode
import com.example.io_motion.core.common.util.parseEnumOrDefault
import com.example.io_motion.core.pose.model.PoseModelVariant
import com.example.io_motion.data.repository.SettingsRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import javax.inject.Inject

@HiltViewModel
class SettingsViewModel @Inject constructor(
    private val settingsRepository: SettingsRepository,
) : ViewModel() {

    val themeMode: StateFlow<ThemeMode> = settingsRepository.themeMode.stateIn(
        scope = viewModelScope,
        started = SharingStarted.Eagerly,
        initialValue = ThemeMode.DARK,
    )

    val accentTheme: StateFlow<AccentTheme> = settingsRepository.accentTheme.stateIn(
        scope = viewModelScope,
        started = SharingStarted.Eagerly,
        initialValue = AccentTheme.BLUE,
    )

    val modelVariant: StateFlow<PoseModelVariant> = settingsRepository.defaultModelVariant
        .map { raw -> parseEnumOrDefault(raw, PoseModelVariant.FULL) }
        .stateIn(
            scope = viewModelScope,
            started = SharingStarted.Eagerly,
            initialValue = PoseModelVariant.FULL,
        )

    fun setThemeMode(mode: ThemeMode) {
        viewModelScope.launch { settingsRepository.setThemeMode(mode) }
    }

    fun setAccentTheme(accent: AccentTheme) {
        viewModelScope.launch { settingsRepository.setAccentTheme(accent) }
    }

    fun setModelVariant(variant: PoseModelVariant) {
        viewModelScope.launch { settingsRepository.setDefaultModelVariant(variant.name) }
    }
}
