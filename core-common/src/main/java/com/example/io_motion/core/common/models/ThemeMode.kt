package com.example.io_motion.core.common.models

enum class ThemeMode {
    LIGHT,
    DARK,
    SYSTEM,
}

fun ThemeMode.displayName(): String = when (this) {
    ThemeMode.LIGHT  -> "Light"
    ThemeMode.DARK   -> "Dark"
    ThemeMode.SYSTEM -> "Auto"
}
