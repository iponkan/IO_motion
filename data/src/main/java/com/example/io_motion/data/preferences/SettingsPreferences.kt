package com.example.io_motion.data.preferences

import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.stringPreferencesKey
import com.example.io_motion.core.common.models.AccentTheme
import com.example.io_motion.core.common.models.ThemeMode
import com.example.io_motion.data.repository.SettingsRepository
import kotlinx.coroutines.flow.map
import javax.inject.Inject

private val THEME_MODE_KEY = stringPreferencesKey("theme_mode")
private val ACCENT_THEME_KEY = stringPreferencesKey("accent_theme")
private val DEFAULT_MODEL_VARIANT_KEY = stringPreferencesKey("default_model_variant")

class SettingsPreferences @Inject constructor(
    private val dataStore: DataStore<Preferences>,
) : SettingsRepository {

    override val themeMode = dataStore.data.map { prefs ->
        prefs[THEME_MODE_KEY]?.let { name ->
            runCatching { ThemeMode.valueOf(name) }.getOrNull()
        } ?: ThemeMode.DARK
    }

    override suspend fun setThemeMode(mode: ThemeMode) {
        dataStore.edit { prefs -> prefs[THEME_MODE_KEY] = mode.name }
    }

    override val accentTheme = dataStore.data.map { prefs ->
        prefs[ACCENT_THEME_KEY]?.let { name ->
            runCatching { AccentTheme.valueOf(name) }.getOrNull()
        } ?: AccentTheme.BLUE
    }

    override suspend fun setAccentTheme(accent: AccentTheme) {
        dataStore.edit { prefs -> prefs[ACCENT_THEME_KEY] = accent.name }
    }

    override val defaultModelVariant = dataStore.data.map { prefs ->
        prefs[DEFAULT_MODEL_VARIANT_KEY] ?: "FULL"
    }

    override suspend fun setDefaultModelVariant(variant: String) {
        dataStore.edit { prefs -> prefs[DEFAULT_MODEL_VARIANT_KEY] = variant }
    }
}
