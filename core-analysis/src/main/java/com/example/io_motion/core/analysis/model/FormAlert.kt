package com.example.io_motion.core.analysis.model

/** Real-time form cues surfaced to the UI while an exercise is in progress. */
enum class FormAlert {
    GO_DEEPER,
    STRAIGHTEN_BODY_LINE,
    UNEVEN_SIDES,
    PERSON_NOT_IN_FRAME,
    LOW_CONFIDENCE,
    /** Push-up: hips sagging below the shoulder–ankle line. */
    SAGGING_HIPS,
    /** Push-up: hips raised above the shoulder–ankle line. */
    PIKING_HIPS,
}
