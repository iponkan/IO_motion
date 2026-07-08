package com.example.io_motion.feature.video

import android.Manifest
import android.content.pm.PackageManager
import android.os.Build
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.animation.AnimatedContent
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.slideInVertically
import androidx.compose.animation.togetherWith
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.ErrorOutline
import androidx.compose.material.icons.filled.Movie
import androidx.compose.material3.Button
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.core.content.ContextCompat
import androidx.hilt.navigation.compose.hiltViewModel
import com.example.io_motion.core.common.models.ExerciseType
import com.example.io_motion.core.common.models.displayName
import com.example.io_motion.core.pose.model.PoseModelVariant
import com.example.io_motion.core.ui.components.MetricGauge
import com.example.io_motion.core.ui.components.PlankMetricsGrid
import com.example.io_motion.core.ui.components.RepCard
import com.example.io_motion.core.ui.components.RepMetricsGrid
import com.example.io_motion.core.ui.overlay.SkeletonOverlay
import com.example.io_motion.feature.video.model.VideoUiState
import kotlin.math.roundToInt

@Composable
fun VideoScreen(
    initialExerciseType: ExerciseType,
    initialModelVariant: PoseModelVariant,
    onNavigateBack: () -> Unit,
    modifier: Modifier = Modifier,
    viewModel: VideoViewModel = hiltViewModel(),
) {
    val uiState by viewModel.uiState.collectAsState()
    val context = LocalContext.current

    val videoPermission = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
        Manifest.permission.READ_MEDIA_VIDEO
    } else {
        Manifest.permission.READ_EXTERNAL_STORAGE
    }
    var permissionDenied by remember { mutableStateOf(false) }

    val permissionLauncher = rememberLauncherForActivityResult(
        ActivityResultContracts.RequestPermission()
    ) { granted ->
        permissionDenied = !granted
        if (granted) viewModel.openGallery()
    }

    fun requestGalleryAccess() {
        val alreadyGranted = ContextCompat.checkSelfPermission(context, videoPermission) ==
            PackageManager.PERMISSION_GRANTED
        if (alreadyGranted) {
            permissionDenied = false
            viewModel.openGallery()
        } else {
            permissionLauncher.launch(videoPermission)
        }
    }

    Surface(modifier = modifier.fillMaxSize()) {
        AnimatedContent(
            targetState = uiState,
            contentKey = { state ->
                when (state) {
                    is VideoUiState.Idle       -> 0
                    is VideoUiState.Gallery    -> 1
                    is VideoUiState.Processing -> 2
                    is VideoUiState.Result     -> 3
                    is VideoUiState.Error      -> 4
                }
            },
            transitionSpec = {
                (fadeIn(tween(300)) + slideInVertically { it / 8 })
                    .togetherWith(fadeOut(tween(200)))
            },
            label = "VideoState",
            modifier = Modifier.fillMaxSize(),
        ) { state ->
            when (state) {
                is VideoUiState.Idle -> IdleContent(
                    exerciseType = initialExerciseType,
                    modelVariant = initialModelVariant,
                    permissionDenied = permissionDenied,
                    onPickVideo = ::requestGalleryAccess,
                    onNavigateBack = onNavigateBack,
                )
                is VideoUiState.Gallery -> GalleryContent(
                    videos = state.videos,
                    isLoading = state.isLoading,
                    onVideoSelected = { viewModel.processVideo(it.uri) },
                    onBack = viewModel::closeGallery,
                )
                is VideoUiState.Processing -> ProcessingContent(
                    state = state,
                    onCancel = viewModel::cancelProcessing,
                )
                is VideoUiState.Result -> ResultContent(
                    state = state,
                    onAnalyzeAnother = viewModel::reset,
                    onNavigateBack = onNavigateBack,
                )
                is VideoUiState.Error -> ErrorContent(
                    message = state.message,
                    onRetry = viewModel::reset,
                    onNavigateBack = onNavigateBack,
                )
            }
        }
    }
}

// ── Idle ───────────────────────────────────────────────────────────────────────

@Composable
private fun IdleContent(
    exerciseType: ExerciseType,
    modelVariant: PoseModelVariant,
    permissionDenied: Boolean,
    onPickVideo: () -> Unit,
    onNavigateBack: () -> Unit,
) {
    Column(
        modifier = Modifier
            .fillMaxSize()
            .statusBarsPadding()
            .navigationBarsPadding(),
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 4.dp, vertical = 4.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            IconButton(onClick = onNavigateBack) {
                Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Back")
            }
            Spacer(Modifier.weight(1f))
            StatBadge(exerciseType.displayName())
            Spacer(Modifier.width(6.dp))
            StatBadge(modelVariant.displayName)
        }

        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(horizontal = 28.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.Center,
        ) {
            Icon(
                imageVector = Icons.Filled.Movie,
                contentDescription = null,
                tint = MaterialTheme.colorScheme.primary,
                modifier = Modifier.size(56.dp),
            )
            Spacer(Modifier.height(20.dp))
            Text(
                text = "Offline Video Analysis",
                style = MaterialTheme.typography.headlineMedium.copy(fontWeight = FontWeight.Bold),
                textAlign = TextAlign.Center,
            )
            Spacer(Modifier.height(10.dp))
            Text(
                text = "Select a recorded ${exerciseType.displayName()} video. " +
                    "Frames are analyzed at ~15 FPS using the ${modelVariant.displayName} model on CPU.",
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.65f),
                textAlign = TextAlign.Center,
            )
            Spacer(Modifier.height(44.dp))
            Button(
                onClick = onPickVideo,
                modifier = Modifier.fillMaxWidth().height(56.dp),
            ) {
                Text(
                    text = "Select Video from Gallery",
                    style = MaterialTheme.typography.titleSmall.copy(fontWeight = FontWeight.SemiBold),
                )
            }
            if (permissionDenied) {
                Spacer(Modifier.height(12.dp))
                Text(
                    text = "Video access was denied. Enable it in system Settings to browse " +
                        "your gallery.",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.error,
                    textAlign = TextAlign.Center,
                )
            }
        }
    }
}

// ── Processing ─────────────────────────────────────────────────────────────────

@Composable
private fun ProcessingContent(
    state: VideoUiState.Processing,
    onCancel: () -> Unit,
) {
    Column(
        modifier = Modifier
            .fillMaxSize()
            .statusBarsPadding()
            .navigationBarsPadding()
            .padding(horizontal = 24.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
    ) {
        Row(
            modifier = Modifier.fillMaxWidth().padding(top = 8.dp),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Text(
                text = "Analyzing ${state.exerciseType.displayName()}…",
                style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.SemiBold),
            )
            TextButton(onClick = onCancel) {
                Text("Cancel", color = MaterialTheme.colorScheme.error)
            }
        }

        Spacer(Modifier.height(20.dp))

        Text(
            text = "${(state.progress * 100).roundToInt()}%",
            style = MaterialTheme.typography.displaySmall.copy(fontWeight = FontWeight.Bold),
            color = MaterialTheme.colorScheme.primary,
        )
        Spacer(Modifier.height(10.dp))
        LinearProgressIndicator(
            progress = { state.progress },
            modifier = Modifier
                .fillMaxWidth()
                .height(8.dp)
                .clip(RoundedCornerShape(4.dp)),
        )
        Spacer(Modifier.height(8.dp))
        Text(
            text = "${state.framesProcessed} frames analyzed",
            style = MaterialTheme.typography.bodySmall,
            color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.55f),
        )

        Spacer(Modifier.height(28.dp))

        // Live skeleton preview of the last detected frame
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .aspectRatio(3f / 4f)
                .clip(MaterialTheme.shapes.large)
                .background(Color(0xFF111111)),
            contentAlignment = Alignment.Center,
        ) {
            if (state.lastPoseFrame != null) {
                SkeletonOverlay(
                    poseFrame = state.lastPoseFrame,
                    exerciseType = state.exerciseType,
                    isMirrored = false,
                    modifier = Modifier.fillMaxSize(),
                )
            } else {
                Text(
                    text = "Scanning for pose…",
                    style = MaterialTheme.typography.bodyMedium,
                    color = Color.White.copy(alpha = 0.35f),
                )
            }
        }
    }
}

// ── Result ─────────────────────────────────────────────────────────────────────

@Composable
private fun ResultContent(
    state: VideoUiState.Result,
    onAnalyzeAnother: () -> Unit,
    onNavigateBack: () -> Unit,
) {
    val metrics = state.metrics
    val isPlank = state.exerciseType == ExerciseType.PLANK

    LazyColumn(
        modifier = Modifier
            .fillMaxSize()
            .statusBarsPadding()
            .navigationBarsPadding(),
        contentPadding = PaddingValues(bottom = 32.dp),
    ) {
        item {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(start = 4.dp, end = 16.dp, top = 8.dp),
                verticalAlignment = Alignment.CenterVertically,
            ) {
                IconButton(onClick = onNavigateBack) {
                    Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Back")
                }
                Spacer(Modifier.weight(1f))
                Column(horizontalAlignment = Alignment.End) {
                    Text(
                        text = state.exerciseType.displayName(),
                        style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold),
                    )
                    Text(
                        text = "Analysis Complete",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.primary,
                    )
                }
            }
        }

        item {
            Column(
                modifier = Modifier.fillMaxWidth().padding(top = 12.dp),
                horizontalAlignment = Alignment.CenterHorizontally,
            ) {
                MetricGauge(value = metrics.sessionQualityScore, label = "SESSION QUALITY")
            }
        }

        item {
            Spacer(Modifier.height(16.dp))
            if (isPlank) {
                PlankMetricsGrid(metrics, Modifier.padding(horizontal = 16.dp))
            } else {
                RepMetricsGrid(metrics, Modifier.padding(horizontal = 16.dp))
            }
        }

        if (!isPlank && metrics.reps.isNotEmpty()) {
            item {
                Spacer(Modifier.height(24.dp))
                Text(
                    text = "Per-Rep Breakdown",
                    style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.SemiBold),
                    modifier = Modifier.padding(horizontal = 20.dp),
                )
                Spacer(Modifier.height(8.dp))
            }
            itemsIndexed(metrics.reps) { index, rep ->
                RepCard(
                    repNumber = index + 1,
                    rep = rep,
                    modifier = Modifier.padding(horizontal = 16.dp, vertical = 4.dp),
                )
            }
        }

        item {
            Spacer(Modifier.height(28.dp))
            Button(
                onClick = onAnalyzeAnother,
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 16.dp)
                    .height(52.dp),
            ) {
                Text(
                    text = "Analyze Another Video",
                    style = MaterialTheme.typography.titleSmall.copy(fontWeight = FontWeight.SemiBold),
                )
            }
        }
    }
}

// ── Error ──────────────────────────────────────────────────────────────────────

@Composable
private fun ErrorContent(
    message: String,
    onRetry: () -> Unit,
    onNavigateBack: () -> Unit,
) {
    Column(
        modifier = Modifier
            .fillMaxSize()
            .statusBarsPadding()
            .navigationBarsPadding()
            .padding(28.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center,
    ) {
        Icon(
            imageVector = Icons.Filled.ErrorOutline,
            contentDescription = null,
            tint = MaterialTheme.colorScheme.error,
            modifier = Modifier.size(48.dp),
        )
        Spacer(Modifier.height(16.dp))
        Text(
            text = "Analysis Failed",
            style = MaterialTheme.typography.headlineSmall.copy(fontWeight = FontWeight.Bold),
        )
        Spacer(Modifier.height(8.dp))
        Text(
            text = message,
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.65f),
            textAlign = TextAlign.Center,
        )
        Spacer(Modifier.height(36.dp))
        Button(onClick = onRetry, modifier = Modifier.fillMaxWidth().height(52.dp)) {
            Text("Try Another Video")
        }
        Spacer(Modifier.height(12.dp))
        OutlinedButton(onClick = onNavigateBack, modifier = Modifier.fillMaxWidth().height(52.dp)) {
            Text("Back to Home")
        }
    }
}

// ── Shared composables ─────────────────────────────────────────────────────────

@Composable
private fun StatBadge(text: String) {
    Surface(
        color = MaterialTheme.colorScheme.secondaryContainer,
        shape = MaterialTheme.shapes.extraSmall,
    ) {
        Text(
            text = text,
            style = MaterialTheme.typography.labelMedium,
            color = MaterialTheme.colorScheme.onSecondaryContainer,
            modifier = Modifier.padding(horizontal = 10.dp, vertical = 4.dp),
        )
    }
}
