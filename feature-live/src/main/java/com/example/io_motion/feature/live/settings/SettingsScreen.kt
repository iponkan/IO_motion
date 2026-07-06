package com.example.io_motion.feature.live.settings

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.outlined.ArrowBack
import androidx.compose.material.icons.outlined.Check
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import com.example.io_motion.core.common.models.AccentTheme
import com.example.io_motion.core.common.models.ThemeMode
import com.example.io_motion.core.pose.model.PoseModelVariant
import com.example.io_motion.core.ui.components.SectionLabel
import com.example.io_motion.core.ui.components.SegmentedControl
import com.example.io_motion.core.ui.theme.IOMotionTextStyles
import com.example.io_motion.core.ui.theme.accentOnColorFor
import com.example.io_motion.core.ui.theme.extendedColors
import com.example.io_motion.core.ui.theme.toColor

@Composable
fun SettingsScreen(
    onNavigateBack: () -> Unit,
    modifier: Modifier = Modifier,
    viewModel: SettingsViewModel = hiltViewModel(),
) {
    val themeMode by viewModel.themeMode.collectAsState()
    val accentTheme by viewModel.accentTheme.collectAsState()
    val modelVariant by viewModel.modelVariant.collectAsState()

    Column(
        modifier = modifier
            .fillMaxSize()
            .statusBarsPadding()
            .navigationBarsPadding()
            .verticalScroll(rememberScrollState())
            .padding(horizontal = 22.dp)
            .padding(top = 20.dp, bottom = 40.dp),
    ) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            Icon(
                imageVector = Icons.AutoMirrored.Outlined.ArrowBack,
                contentDescription = "Back",
                tint = MaterialTheme.colorScheme.onBackground,
                modifier = Modifier.size(20.dp).clickable(onClick = onNavigateBack),
            )
            Spacer(modifier = Modifier.width(16.dp))
            Text(
                text = "Settings",
                style = IOMotionTextStyles.historyTitle,
                color = MaterialTheme.colorScheme.onBackground,
            )
        }

        Spacer(modifier = Modifier.height(32.dp))

        // ── Theme ────────────────────────────────────────────────────────────
        SectionLabel(text = "THEME")
        Spacer(modifier = Modifier.height(14.dp))
        SegmentedControl(
            options = listOf("Light", "Dark"),
            selectedIndex = if (themeMode == ThemeMode.LIGHT) 0 else 1,
            onSelect = { index -> viewModel.setThemeMode(if (index == 0) ThemeMode.LIGHT else ThemeMode.DARK) },
        )

        Spacer(modifier = Modifier.height(28.dp))

        // ── Accent color ─────────────────────────────────────────────────────
        SectionLabel(text = "ACCENT COLOR")
        Spacer(modifier = Modifier.height(14.dp))
        Row(horizontalArrangement = Arrangement.spacedBy(16.dp)) {
            AccentTheme.entries.forEach { theme ->
                AccentSwatch(
                    accentTheme = theme,
                    selected = theme == accentTheme,
                    onClick = { viewModel.setAccentTheme(theme) },
                )
            }
        }

        Spacer(modifier = Modifier.height(28.dp))

        // ── Default model variant ────────────────────────────────────────────
        SectionLabel(text = "DEFAULT MODEL VARIANT")
        Spacer(modifier = Modifier.height(14.dp))
        Text(
            text = "Lite: fastest · Full: balanced · Heavy: most accurate",
            style = IOMotionTextStyles.modelVariantCaption,
            color = MaterialTheme.extendedColors.textMuted,
        )
        Spacer(modifier = Modifier.height(10.dp))
        SegmentedControl(
            options = PoseModelVariant.entries.map { it.displayName },
            selectedIndex = PoseModelVariant.entries.indexOf(modelVariant),
            onSelect = { index -> viewModel.setModelVariant(PoseModelVariant.entries[index]) },
        )
    }
}

@Composable
private fun AccentSwatch(
    accentTheme: AccentTheme,
    selected: Boolean,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val color = accentTheme.toColor()
    Box(
        modifier = modifier
            .size(44.dp)
            .clip(CircleShape)
            .background(color, CircleShape)
            .border(
                width = if (selected) 2.dp else 0.dp,
                color = if (selected) MaterialTheme.colorScheme.onBackground else Color.Transparent,
                shape = CircleShape,
            )
            .clickable(onClick = onClick),
        contentAlignment = Alignment.Center,
    ) {
        if (selected) {
            Icon(
                imageVector = Icons.Outlined.Check,
                contentDescription = "${accentTheme.name} selected",
                tint = accentOnColorFor(color),
                modifier = Modifier.size(18.dp),
            )
        }
    }
}
