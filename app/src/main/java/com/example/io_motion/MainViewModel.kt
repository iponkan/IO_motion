package com.example.io_motion

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.io_motion.core.common.models.AccentTheme
import com.example.io_motion.core.common.models.ThemeMode
import com.example.io_motion.data.repository.SettingsRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.stateIn
import javax.inject.Inject

@HiltViewModel
class MainViewModel @Inject constructor(
    settingsRepository: SettingsRepository,
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
}
