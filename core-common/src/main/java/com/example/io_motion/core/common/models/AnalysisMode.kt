package com.example.io_motion.core.common.models

enum class AnalysisMode {
    LIVE,
    OFFLINE,
}

/** Uppercase label for the "LIVE · FULL" mode/variant meta line on Details/History rows. */
fun AnalysisMode.metaLabel(): String = when (this) {
    AnalysisMode.LIVE    -> "LIVE"
    AnalysisMode.OFFLINE -> "VIDEO"
}
