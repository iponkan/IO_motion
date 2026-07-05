package com.example.io_motion.core.common.models

enum class ThemeMode {
    LIGHT,
    DARK,
}

fun ThemeMode.displayName(): String = when (this) {
    ThemeMode.LIGHT -> "Light"
    ThemeMode.DARK  -> "Dark"
}

fun ThemeMode.toggled(): ThemeMode = when (this) {
    ThemeMode.LIGHT -> ThemeMode.DARK
    ThemeMode.DARK  -> ThemeMode.LIGHT
}
