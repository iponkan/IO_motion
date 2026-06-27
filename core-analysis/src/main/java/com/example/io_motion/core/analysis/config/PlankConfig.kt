package com.example.io_motion.core.analysis.config

import com.example.io_motion.core.analysis.filter.OneEuroFilterConfig

/**
 * Threshold configuration for [com.example.io_motion.core.analysis.analyzer.PlankAnalyzer].
 *
 * @param bodyLineTolerance How far (degrees) the shoulder–hip–ankle angle can deviate from 180°
 *   before the hold is considered "out of form" and accumulation pauses.
 * @param visibilityThreshold Minimum landmark visibility for body-line computation.
 * @param filterConfig One-Euro filter settings for the body-line angle signal.
 */
data class PlankConfig(
    val bodyLineTolerance: Double = 15.0,
    val visibilityThreshold: Float = 0.5f,
    val filterConfig: OneEuroFilterConfig = OneEuroFilterConfig(minCutoff = 0.5, beta = 0.001),
)
