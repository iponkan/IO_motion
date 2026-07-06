package com.example.io_motion

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.runtime.SideEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.platform.LocalView
import androidx.core.view.WindowCompat
import androidx.hilt.navigation.compose.hiltViewModel
import com.example.io_motion.core.common.models.ThemeMode
import com.example.io_motion.core.ui.theme.IO_motionTheme
import com.example.io_motion.navigation.AppNavHost
import dagger.hilt.android.AndroidEntryPoint

@AndroidEntryPoint
class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContent {
            val mainViewModel: MainViewModel = hiltViewModel()
            val themeMode by mainViewModel.themeMode.collectAsState()
            val accentTheme by mainViewModel.accentTheme.collectAsState()
            val darkTheme = themeMode == ThemeMode.DARK

            // In-app Light/Dark overrides must re-color the system bar icons too — the system
            // setting alone (which enableEdgeToEdge reads once at startup) isn't enough once the
            // user picks a mode that disagrees with it.
            val view = LocalView.current
            SideEffect {
                val controller = WindowCompat.getInsetsController(window, view)
                controller.isAppearanceLightStatusBars = !darkTheme
                controller.isAppearanceLightNavigationBars = !darkTheme
            }

            IO_motionTheme(themeMode = themeMode, accentTheme = accentTheme) {
                AppNavHost()
            }
        }
    }
}
