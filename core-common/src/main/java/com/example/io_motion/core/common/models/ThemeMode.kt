package com.example.io_motion.core.common.models

enum class ThemeMode {
    LIGHT,
    DARK,
}

fun ThemeMode.displayName(): String = when (this) {
    ThemeMode.LIGHT -> "Light"
    ThemeMode.DARK  -> "Dark"
}
