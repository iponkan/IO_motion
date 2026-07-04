package com.example.io_motion.core.ui.overlay

import androidx.compose.foundation.Canvas
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.unit.dp
import com.example.io_motion.core.common.models.ExerciseType
import com.example.io_motion.core.common.models.Landmark
import com.example.io_motion.core.common.models.PoseFrame
import com.example.io_motion.core.common.models.PoseLandmarkIndex

private val BODY_CONNECTIONS = listOf(
    PoseLandmarkIndex.LEFT_SHOULDER  to PoseLandmarkIndex.RIGHT_SHOULDER,
    PoseLandmarkIndex.LEFT_SHOULDER  to PoseLandmarkIndex.LEFT_ELBOW,
    PoseLandmarkIndex.LEFT_ELBOW     to PoseLandmarkIndex.LEFT_WRIST,
    PoseLandmarkIndex.RIGHT_SHOULDER to PoseLandmarkIndex.RIGHT_ELBOW,
    PoseLandmarkIndex.RIGHT_ELBOW    to PoseLandmarkIndex.RIGHT_WRIST,
    PoseLandmarkIndex.LEFT_SHOULDER  to PoseLandmarkIndex.LEFT_HIP,
    PoseLandmarkIndex.RIGHT_SHOULDER to PoseLandmarkIndex.RIGHT_HIP,
    PoseLandmarkIndex.LEFT_HIP       to PoseLandmarkIndex.RIGHT_HIP,
    PoseLandmarkIndex.LEFT_HIP       to PoseLandmarkIndex.LEFT_KNEE,
    PoseLandmarkIndex.LEFT_KNEE      to PoseLandmarkIndex.LEFT_ANKLE,
    PoseLandmarkIndex.RIGHT_HIP      to PoseLandmarkIndex.RIGHT_KNEE,
    PoseLandmarkIndex.RIGHT_KNEE     to PoseLandmarkIndex.RIGHT_ANKLE,
)

private val JOINT_INDICES = listOf(
    PoseLandmarkIndex.NOSE,
    PoseLandmarkIndex.LEFT_SHOULDER,  PoseLandmarkIndex.RIGHT_SHOULDER,
    PoseLandmarkIndex.LEFT_ELBOW,     PoseLandmarkIndex.RIGHT_ELBOW,
    PoseLandmarkIndex.LEFT_WRIST,     PoseLandmarkIndex.RIGHT_WRIST,
    PoseLandmarkIndex.LEFT_HIP,       PoseLandmarkIndex.RIGHT_HIP,
    PoseLandmarkIndex.LEFT_KNEE,      PoseLandmarkIndex.RIGHT_KNEE,
    PoseLandmarkIndex.LEFT_ANKLE,     PoseLandmarkIndex.RIGHT_ANKLE,
)

private fun primaryJointsFor(exerciseType: ExerciseType?): Set<Int> = when (exerciseType) {
    ExerciseType.SQUAT -> setOf(
        PoseLandmarkIndex.LEFT_HIP,   PoseLandmarkIndex.RIGHT_HIP,
        PoseLandmarkIndex.LEFT_KNEE,  PoseLandmarkIndex.RIGHT_KNEE,
        PoseLandmarkIndex.LEFT_ANKLE, PoseLandmarkIndex.RIGHT_ANKLE,
    )
    ExerciseType.PUSH_UP -> setOf(
        PoseLandmarkIndex.LEFT_SHOULDER,  PoseLandmarkIndex.RIGHT_SHOULDER,
        PoseLandmarkIndex.LEFT_ELBOW,     PoseLandmarkIndex.RIGHT_ELBOW,
        PoseLandmarkIndex.LEFT_WRIST,     PoseLandmarkIndex.RIGHT_WRIST,
    )
    ExerciseType.SIT_UP -> setOf(
        PoseLandmarkIndex.LEFT_SHOULDER, PoseLandmarkIndex.RIGHT_SHOULDER,
        PoseLandmarkIndex.LEFT_HIP,      PoseLandmarkIndex.RIGHT_HIP,
    )
    ExerciseType.PLANK -> setOf(
        PoseLandmarkIndex.LEFT_SHOULDER, PoseLandmarkIndex.RIGHT_SHOULDER,
        PoseLandmarkIndex.LEFT_HIP,      PoseLandmarkIndex.RIGHT_HIP,
        PoseLandmarkIndex.LEFT_ANKLE,    PoseLandmarkIndex.RIGHT_ANKLE,
    )
    null -> emptySet()
}

/**
 * Canvas-based skeleton overlay for the live camera preview.
 *
 * Joints belonging to the active exercise's primary measurement are drawn in yellow;
 * all others are white. Landmarks below [visibilityThreshold] are skipped so the skeleton
 * gracefully degrades when body parts leave the frame.
 *
 * @param poseFrame Current detection result. Null or invalid frames render nothing.
 * @param modifier Should fill the same bounds as the [androidx.camera.view.PreviewView].
 * @param exerciseType Highlights exercise-specific joints in a distinct color.
 * @param isMirrored Mirror x-coordinates for front-camera to match PreviewView's default mirror.
 * @param visibilityThreshold Minimum MediaPipe landmark visibility to draw a joint/connection.
 */
@Composable
fun SkeletonOverlay(
    poseFrame: PoseFrame?,
    modifier: Modifier = Modifier,
    exerciseType: ExerciseType? = null,
    isMirrored: Boolean = false,
    visibilityThreshold: Float = 0.5f,
) {
    val primaryJoints = remember(exerciseType) { primaryJointsFor(exerciseType) }

    Canvas(modifier = modifier) {
        val frame = poseFrame?.takeIf { it.isValid } ?: return@Canvas
        val landmarks = frame.normalizedLandmarks

        fun lmX(lm: Landmark): Float = if (isMirrored) (1f - lm.x) * size.width else lm.x * size.width
        fun lmY(lm: Landmark): Float = lm.y * size.height
        fun Landmark.visible(): Boolean = visibility >= visibilityThreshold

        val defaultStroke = 3.dp.toPx()
        val primaryStroke = 5.dp.toPx()
        val defaultRadius = 4.dp.toPx()
        val primaryRadius = 7.dp.toPx()

        for ((startIdx, endIdx) in BODY_CONNECTIONS) {
            val start = landmarks.getOrNull(startIdx)?.takeIf { it.visible() } ?: continue
            val end   = landmarks.getOrNull(endIdx)?.takeIf { it.visible() } ?: continue
            val isPrimary = startIdx in primaryJoints || endIdx in primaryJoints
            drawLine(
                color = if (isPrimary) SkeletonColors.primaryLine else SkeletonColors.defaultLine,
                start = Offset(lmX(start), lmY(start)),
                end   = Offset(lmX(end),   lmY(end)),
                strokeWidth = if (isPrimary) primaryStroke else defaultStroke,
                cap = StrokeCap.Round,
            )
        }

        for (idx in JOINT_INDICES) {
            val lm = landmarks.getOrNull(idx)?.takeIf { it.visible() } ?: continue
            val isPrimary = idx in primaryJoints
            drawCircle(
                color  = if (isPrimary) SkeletonColors.primaryJoint else SkeletonColors.defaultJoint,
                radius = if (isPrimary) primaryRadius else defaultRadius,
                center = Offset(lmX(lm), lmY(lm)),
            )
        }
    }
}
