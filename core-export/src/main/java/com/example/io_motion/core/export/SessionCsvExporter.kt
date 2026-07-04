package com.example.io_motion.core.export

import com.example.io_motion.core.analysis.model.SessionMetrics

/**
 * Produces two CSV sections for a session:
 *  - KPI summary: one header row + one data row covering all session-level metrics.
 *  - Per-rep breakdown: one header row + one row per valid rep.
 *
 * Both sections are combined in [export] with a `# comment` separator.
 * Alerts in the rep section are pipe-separated within a quoted field to avoid CSV ambiguity.
 */
object SessionCsvExporter {

    private const val SUMMARY_HEADER =
        "id,recordedAt,exerciseType,analysisMode,modelVariant," +
        "totalDurationMs,repCount,rejectedRepCount,tempoRpm,rhythmConsistency," +
        "avgRomDegrees,sessionQualityScore,validHoldMs,avgBodyLineAngle"

    private const val REP_HEADER =
        "sessionId,repNumber,durationMs,minAngle,maxAngle,romDegrees,qualityScore,alerts"

    /** Returns a single string with the KPI summary and per-rep sections combined. */
    fun export(
        id: Long,
        recordedAt: Long,
        analysisMode: String,
        modelVariant: String,
        metrics: SessionMetrics,
    ): String = buildString {
        appendLine("# KPI Summary")
        appendLine(SUMMARY_HEADER)
        appendLine(summaryRow(id, recordedAt, analysisMode, modelVariant, metrics))
        appendLine()
        appendLine("# Per-Rep Breakdown")
        appendLine(REP_HEADER)
        metrics.reps.forEach { rep ->
            appendLine(
                listOf(
                    id,
                    rep.repNumber,
                    rep.durationMs,
                    "%.2f".format(rep.minAngle),
                    "%.2f".format(rep.maxAngle),
                    "%.2f".format(rep.rom),
                    rep.qualityScore,
                    "\"${rep.alerts.joinToString("|") { it.name }}\"",
                ).joinToString(",")
            )
        }
    }

    private fun summaryRow(
        id: Long,
        recordedAt: Long,
        analysisMode: String,
        modelVariant: String,
        m: SessionMetrics,
    ) = listOf(
        id, recordedAt, m.exerciseType.name, analysisMode, modelVariant,
        m.totalDurationMs, m.repCount, m.rejectedRepCount,
        "%.3f".format(m.tempoRpm), m.rhythmConsistency,
        "%.2f".format(m.avgRomDegrees), m.sessionQualityScore,
        m.validHoldMs, "%.2f".format(m.avgBodyLineAngle),
    ).joinToString(",")
}
