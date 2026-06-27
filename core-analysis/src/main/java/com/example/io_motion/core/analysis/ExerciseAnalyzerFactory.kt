package com.example.io_motion.core.analysis

import com.example.io_motion.core.analysis.analyzer.PlankAnalyzer
import com.example.io_motion.core.analysis.analyzer.PushUpAnalyzer
import com.example.io_motion.core.analysis.analyzer.SitUpAnalyzer
import com.example.io_motion.core.analysis.analyzer.SquatAnalyzer
import com.example.io_motion.core.common.models.ExerciseType

/** Creates the appropriate [ExerciseAnalyzer] for the given exercise type with default config. */
object ExerciseAnalyzerFactory {

    fun create(exerciseType: ExerciseType): ExerciseAnalyzer = when (exerciseType) {
        ExerciseType.SQUAT -> SquatAnalyzer()
        ExerciseType.SIT_UP -> SitUpAnalyzer()
        ExerciseType.PUSH_UP -> PushUpAnalyzer()
        ExerciseType.PLANK -> PlankAnalyzer()
    }
}
