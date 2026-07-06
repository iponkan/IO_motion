package com.example.io_motion.data.repository

import com.example.io_motion.core.common.models.AccentTheme
import com.example.io_motion.core.common.models.ThemeMode
import kotlinx.coroutines.flow.Flow

interface SettingsRepository {
    /** Persisted theme preference, defaulting to [ThemeMode.DARK] until the user changes it. */
    val themeMode: Flow<ThemeMode>

    suspend fun setThemeMode(mode: ThemeMode)

    /** Persisted accent color, defaulting to [AccentTheme.BLUE] until the user changes it. */
    val accentTheme: Flow<AccentTheme>

    suspend fun setAccentTheme(accent: AccentTheme)

    /** Persisted default pose-model variant name (e.g. "FULL"), used to pre-select analysis settings. */
    val defaultModelVariant: Flow<String>

    suspend fun setDefaultModelVariant(variant: String)
}
