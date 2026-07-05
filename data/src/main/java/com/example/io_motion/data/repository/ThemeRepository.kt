package com.example.io_motion.data.repository

import com.example.io_motion.core.common.models.ThemeMode
import kotlinx.coroutines.flow.Flow

interface ThemeRepository {
    /** Persisted theme preference, defaulting to [ThemeMode.SYSTEM] until the user changes it. */
    val themeMode: Flow<ThemeMode>

    suspend fun setThemeMode(mode: ThemeMode)
}
