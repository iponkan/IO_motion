package com.example.io_motion.core.common.models

enum class AccentTheme {
    BLUE,
    ORANGE,
    LIME,
}

fun AccentTheme.displayName(): String = when (this) {
    AccentTheme.BLUE   -> "Blue"
    AccentTheme.ORANGE -> "Orange"
    AccentTheme.LIME   -> "Lime"
}
