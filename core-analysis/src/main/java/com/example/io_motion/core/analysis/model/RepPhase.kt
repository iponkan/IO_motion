package com.example.io_motion.core.analysis.model

/** Current position within a rep cycle as seen by the FSM. */
enum class RepPhase {
    /** At or returning toward the start/extension position (e.g., standing in squat, arms extended in push-up). */
    EXTENDED,
    /** At or moving toward the bottom/flexion position (e.g., squat depth, push-up lowered). */
    FLEXED,
}
