package com.example.io_motion.data.preferences

import android.content.ComponentName
import android.content.Context
import android.content.pm.PackageManager
import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.stringPreferencesKey
import com.example.io_motion.core.common.models.AccentTheme
import com.example.io_motion.core.common.models.ThemeMode
import com.example.io_motion.data.di.ApplicationScope
import com.example.io_motion.data.repository.SettingsRepository
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.launch
import javax.inject.Inject

private val THEME_MODE_KEY = stringPreferencesKey("theme_mode")
private val ACCENT_THEME_KEY = stringPreferencesKey("accent_theme")
private val DEFAULT_MODEL_VARIANT_KEY = stringPreferencesKey("default_model_variant")

/** `activity-alias` names in AndroidManifest.xml, one per [AccentTheme], each with its own launcher icon. */
private val LAUNCHER_ALIASES = mapOf(
    AccentTheme.BLUE to "com.example.io_motion.LauncherBlue",
    AccentTheme.ORANGE to "com.example.io_motion.LauncherOrange",
    AccentTheme.LIME to "com.example.io_motion.LauncherLime",
)

class SettingsPreferences @Inject constructor(
    private val dataStore: DataStore<Preferences>,
    @ApplicationContext private val context: Context,
    @ApplicationScope private val appScope: CoroutineScope,
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
        // Deliberately NOT calling applyLauncherIcon here — see syncLauncherIcon() kdoc.
    }

    override val defaultModelVariant = dataStore.data.map { prefs ->
        prefs[DEFAULT_MODEL_VARIANT_KEY] ?: "FULL"
    }

    override suspend fun setDefaultModelVariant(variant: String) {
        dataStore.edit { prefs -> prefs[DEFAULT_MODEL_VARIANT_KEY] = variant }
    }

    override fun syncLauncherIcon() {
        appScope.launch {
            applyLauncherIcon(accentTheme.first())
        }
    }

    /**
     * Enables the launcher `activity-alias` matching [accent] and disables the other two, so the
     * home-screen icon follows the in-app accent-color choice. [PackageManager.DONT_KILL_APP] only
     * prevents the *process* from being killed — it does not stop the OS from finishing whichever
     * task was launched via the alias being disabled, so this must never run while that task is
     * in the foreground (hence [syncLauncherIcon] being called from `onStop`, not from
     * [setAccentTheme] directly).
     */
    private fun applyLauncherIcon(accent: AccentTheme) {
        val packageManager = context.packageManager
        LAUNCHER_ALIASES.forEach { (theme, className) ->
            val state = if (theme == accent) {
                PackageManager.COMPONENT_ENABLED_STATE_ENABLED
            } else {
                PackageManager.COMPONENT_ENABLED_STATE_DISABLED
            }
            packageManager.setComponentEnabledSetting(
                ComponentName(context.packageName, className),
                state,
                PackageManager.DONT_KILL_APP,
            )
        }
    }
}
