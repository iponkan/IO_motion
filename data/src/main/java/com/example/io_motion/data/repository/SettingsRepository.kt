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

    /** Daily calorie target for Diet Planning; config (not hardcoded UI), default 2200 kcal. */
    val calorieTarget: Flow<Int>

    suspend fun setCalorieTarget(kcal: Int)

    /** Daily water target in cups for Diet Planning; config, default 8 cups. */
    val waterTargetCups: Flow<Int>

    suspend fun setWaterTargetCups(cups: Int)

    /**
     * Applies the persisted [accentTheme] to the launcher icon (enables the matching
     * `activity-alias`, disables the other two). Callers must only invoke this while the app is
     * backgrounded — disabling the alias that launched the currently-foregrounded task kills it
     * immediately, regardless of [android.content.pm.PackageManager.DONT_KILL_APP]. See
     * `MainActivity`'s `onStop`-triggered call site.
     */
    fun syncLauncherIcon()
}
