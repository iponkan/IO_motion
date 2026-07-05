package com.example.io_motion.feature.live

import android.Manifest
import android.content.pm.PackageManager
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.camera.view.PreviewView
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalLifecycleOwner
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.viewinterop.AndroidView
import androidx.core.content.ContextCompat
import androidx.hilt.navigation.compose.hiltViewModel
import com.example.io_motion.core.analysis.model.AnalyzerState
import com.example.io_motion.core.analysis.model.FormAlert
import com.example.io_motion.core.pose.model.PoseModelVariant
import com.example.io_motion.core.ui.components.FpsLatencyBadge
import com.example.io_motion.core.ui.components.FormAlertBanner
import com.example.io_motion.core.ui.components.MetricGauge
import com.example.io_motion.core.ui.components.RepCounter
import com.example.io_motion.core.ui.overlay.SkeletonOverlay

@Composable
fun LiveScreen(
    onNavigateBack: () -> Unit,
    modifier: Modifier = Modifier,
    viewModel: LiveViewModel = hiltViewModel(),
) {
    val context = LocalContext.current
    val lifecycleOwner = LocalLifecycleOwner.current
    val uiState by viewModel.uiState.collectAsState()

    var hasCameraPermission by remember {
        mutableStateOf(
            ContextCompat.checkSelfPermission(context, Manifest.permission.CAMERA) == PackageManager.PERMISSION_GRANTED
        )
    }
    val permissionLauncher = rememberLauncherForActivityResult(
        ActivityResultContracts.RequestPermission()
    ) { granted -> hasCameraPermission = granted }

    LaunchedEffect(Unit) {
        if (!hasCameraPermission) permissionLauncher.launch(Manifest.permission.CAMERA)
    }

    Box(modifier = modifier.fillMaxSize().background(Color.Black)) {

        if (hasCameraPermission) {
            AndroidView(
                factory = { ctx ->
                    PreviewView(ctx).apply {
                        scaleType = PreviewView.ScaleType.FILL_CENTER
                        implementationMode = PreviewView.ImplementationMode.COMPATIBLE
                    }.also { previewView ->
                        viewModel.bindCamera(lifecycleOwner, previewView.surfaceProvider)
                    }
                },
                modifier = Modifier.fillMaxSize(),
            )
            SkeletonOverlay(
                poseFrame = uiState.poseFrame,
                exerciseType = uiState.exerciseType,
                isMirrored = true,
                modifier = Modifier.fillMaxSize(),
            )
        }

        // ── Top bar ────────────────────────────────────────────────────────────
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .statusBarsPadding()
                .padding(horizontal = 4.dp, vertical = 4.dp),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically,
        ) {
            TextButton(onClick = {
                if (uiState.isSessionActive) viewModel.stopSession()
                onNavigateBack()
            }) {
                Text("← Back", color = Color.White, style = MaterialTheme.typography.labelLarge)
            }

            FpsLatencyBadge(fps = uiState.fps, inferenceTimeMs = uiState.inferenceTimeMs)

            ModelChips(selected = uiState.modelVariant, onSelect = viewModel::selectModelVariant)
        }

        // ── Center state overlay ───────────────────────────────────────────────
        val overlayMessage: String? = when {
            !hasCameraPermission -> "Camera permission required"
            uiState.fatalErrorMessage != null -> uiState.fatalErrorMessage
            !uiState.isSessionActive -> "Tap Start Session to begin"
            uiState.analyzerState is AnalyzerState.AwaitingStart -> "Get into starting position"
            uiState.analyzerState is AnalyzerState.LowConfidence -> "Person not fully in frame"
            else -> null
        }
        AnimatedVisibility(
            visible = overlayMessage != null,
            enter = fadeIn(),
            exit = fadeOut(),
            modifier = Modifier.align(Alignment.Center),
        ) {
            if (overlayMessage != null) {
                Surface(
                    color = Color.Black.copy(alpha = 0.65f),
                    shape = MaterialTheme.shapes.medium,
                ) {
                    Text(
                        text = overlayMessage,
                        color = Color.White,
                        style = MaterialTheme.typography.titleMedium,
                        modifier = Modifier.padding(horizontal = 20.dp, vertical = 12.dp),
                    )
                }
            }
        }

        if (!hasCameraPermission) {
            Button(
                onClick = { permissionLauncher.launch(Manifest.permission.CAMERA) },
                modifier = Modifier.align(Alignment.Center).padding(top = 80.dp),
            ) { Text("Grant Camera Permission") }
        }

        // ── Bottom panel ───────────────────────────────────────────────────────
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .align(Alignment.BottomCenter)
                .background(Color.Black.copy(alpha = 0.55f))
                .navigationBarsPadding()
                .padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp),
        ) {
            FormAlertBanner(alerts = uiState.analyzerState.toDisplayStrings())

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceEvenly,
                verticalAlignment = Alignment.CenterVertically,
            ) {
                when (val s = uiState.analyzerState) {
                    is AnalyzerState.Tracking -> {
                        RepCounter(repCount = s.repCount, contentColor = Color.White)
                        AngleReadout(angle = s.primaryAngle, label = "ANGLE")
                        MetricGauge(value = uiState.liveFormScore, label = "FORM", contentColor = Color.White)
                    }
                    is AnalyzerState.HoldTracking -> {
                        HoldDuration(validHoldMs = s.validHoldMs)
                        AngleReadout(angle = s.bodyLineAngle, label = "BODY LINE")
                        MetricGauge(value = uiState.liveFormScore, label = "FORM", contentColor = Color.White)
                    }
                    else -> Spacer(modifier = Modifier.height(80.dp))
                }
            }

            Button(
                onClick = {
                    if (uiState.isSessionActive) viewModel.stopSession() else viewModel.startSession()
                },
                modifier = Modifier.fillMaxWidth().height(52.dp),
                colors = ButtonDefaults.buttonColors(
                    containerColor = if (uiState.isSessionActive) MaterialTheme.colorScheme.error
                                     else MaterialTheme.colorScheme.primary,
                ),
            ) {
                Text(
                    text = if (uiState.isSessionActive) "Stop Session" else "Start Session",
                    style = MaterialTheme.typography.titleSmall.copy(fontWeight = FontWeight.Bold),
                )
            }
        }
    }
}

// ── Private composables ────────────────────────────────────────────────────────

@Composable
private fun AngleReadout(angle: Double, label: String) {
    Column(horizontalAlignment = Alignment.CenterHorizontally) {
        Text(text = label, style = MaterialTheme.typography.labelMedium, color = Color.White.copy(alpha = 0.7f))
        Text(
            text = "${angle.toInt()}°",
            style = MaterialTheme.typography.headlineMedium.copy(fontWeight = FontWeight.Bold),
            color = Color.White,
        )
    }
}

@Composable
private fun HoldDuration(validHoldMs: Long) {
    val totalSeconds = validHoldMs / 1_000L
    Column(horizontalAlignment = Alignment.CenterHorizontally) {
        Text(text = "HOLD", style = MaterialTheme.typography.labelMedium, color = Color.White.copy(alpha = 0.7f))
        Text(
            text = "%d:%02d".format(totalSeconds / 60L, totalSeconds % 60L),
            style = MaterialTheme.typography.displaySmall.copy(fontWeight = FontWeight.Bold),
            color = Color.White,
        )
    }
}

@Composable
private fun ModelChips(selected: PoseModelVariant, onSelect: (PoseModelVariant) -> Unit) {
    Row(horizontalArrangement = Arrangement.spacedBy(4.dp)) {
        PoseModelVariant.entries.forEach { variant ->
            Surface(
                onClick = { onSelect(variant) },
                color = if (variant == selected) MaterialTheme.colorScheme.primary else Color.White.copy(alpha = 0.2f),
                shape = MaterialTheme.shapes.extraSmall,
            ) {
                Text(
                    text = variant.displayName,
                    style = MaterialTheme.typography.labelSmall,
                    color = Color.White,
                    modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp),
                )
            }
        }
    }
}

// ── Extension helpers ──────────────────────────────────────────────────────────

private fun AnalyzerState.toDisplayStrings(): List<String> = when (this) {
    is AnalyzerState.Tracking     -> alerts.map { it.toDisplayString() }
    is AnalyzerState.HoldTracking -> alerts.map { it.toDisplayString() }
    else -> emptyList()
}

private fun FormAlert.toDisplayString(): String = when (this) {
    FormAlert.GO_DEEPER            -> "Go deeper"
    FormAlert.STRAIGHTEN_BODY_LINE -> "Keep body straight"
    FormAlert.UNEVEN_SIDES         -> "Keep sides even"
    FormAlert.PERSON_NOT_IN_FRAME  -> "Move back — not fully in frame"
    FormAlert.LOW_CONFIDENCE       -> "Hold position"
    FormAlert.SAGGING_HIPS         -> "Don't let hips sag"
    FormAlert.PIKING_HIPS          -> "Don't raise hips"
}
