package com.example.ui.screens

import android.app.Activity
import android.content.Context
import android.content.ContextWrapper
import android.content.pm.ActivityInfo
import android.media.AudioManager
import androidx.annotation.OptIn
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.foundation.background
import androidx.compose.foundation.gestures.awaitEachGesture
import androidx.compose.foundation.gestures.awaitFirstDown
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.safeDrawing
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.windowInsetsPadding
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.automirrored.filled.QueueMusic
import androidx.compose.material.icons.automirrored.filled.VolumeUp
import androidx.compose.material.icons.filled.AspectRatio
import androidx.compose.material.icons.filled.BrightnessHigh
import androidx.compose.material.icons.filled.Favorite
import androidx.compose.material.icons.filled.FavoriteBorder
import androidx.compose.material.icons.filled.Forward10
import androidx.compose.material.icons.filled.Lock
import androidx.compose.material.icons.filled.LockOpen
import androidx.compose.material.icons.filled.MoreVert
import androidx.compose.material.icons.filled.Pause
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material.icons.filled.Repeat
import androidx.compose.material.icons.filled.RepeatOne
import androidx.compose.material.icons.filled.Replay10
import androidx.compose.material.icons.filled.ScreenRotation
import androidx.compose.material.icons.filled.SkipNext
import androidx.compose.material.icons.filled.SkipPrevious
import androidx.compose.material.icons.filled.Subtitles
import androidx.compose.material3.Button
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Slider
import androidx.compose.material3.SliderDefaults
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.minimumInteractiveComponentSize
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableLongStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.input.pointer.positionChange
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalView
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.core.view.WindowCompat
import androidx.core.view.WindowInsetsCompat
import androidx.core.view.WindowInsetsControllerCompat
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.media3.common.util.UnstableApi
import androidx.media3.ui.compose.ContentFrame
import com.example.R
import com.example.data.prefs.AspectResizeOption
import com.example.player.PlayerViewModel
import com.example.player.RepeatModeOption
import com.example.util.formatDuration
import com.example.util.formatSignedSeconds
import kotlinx.coroutines.delay
import kotlin.math.abs

private enum class DragGestureMode {
    NONE,
    HORIZONTAL_SEEK,
    LEFT_BRIGHTNESS,
    RIGHT_VOLUME,
}

@OptIn(UnstableApi::class)
@Composable
fun PlayerScreen(
    videoId: Long,
    playlistId: Long?,
    doubleTapSeekSeconds: Int,
    playerViewModel: PlayerViewModel,
    onBack: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val uiState by playerViewModel.uiState.collectAsStateWithLifecycle()
    val context = LocalContext.current
    val view = LocalView.current
    val activity = remember(context) { context.findActivity() }
    val audioManager = remember(context) {
        context.getSystemService(Context.AUDIO_SERVICE) as AudioManager
    }

    var controlsVisible by remember { mutableStateOf(true) }
    var controlsLocked by remember { mutableStateOf(false) }
    var isLandscapeForced by remember { mutableStateOf(false) }

    var showQueueSheet by remember { mutableStateOf(false) }
    var showTracksSheet by remember { mutableStateOf(false) }
    var showOptionsSheet by remember { mutableStateOf(false) }

    // Scrubbing & gesture indicator states
    var isSliderDragging by remember { mutableStateOf(false) }
    var sliderDragPositionMs by remember { mutableLongStateOf(0L) }
    var gestureSeekPreviewMs by remember { mutableStateOf<Long?>(null) }
    var gestureSeekDeltaSec by remember { mutableIntStateOf(0) }
    var gestureBrightnessLevel by remember { mutableStateOf<Float?>(null) }
    var gestureVolumeLevel by remember { mutableStateOf<Float?>(null) }
    var doubleTapFeedbackText by remember { mutableStateOf<String?>(null) }

    LaunchedEffect(videoId, playlistId) {
        playerViewModel.startPlayback(videoId, playlistId)
    }

    // Auto-hide HUD after 3.5 seconds while playing
    LaunchedEffect(controlsVisible, uiState.isPlaying, controlsLocked, showQueueSheet, showTracksSheet, showOptionsSheet) {
        if (controlsVisible && uiState.isPlaying && !controlsLocked && !showQueueSheet && !showTracksSheet && !showOptionsSheet) {
            delay(3_500L)
            controlsVisible = false
        }
    }

    // Clear double-tap visual pill after 700ms
    LaunchedEffect(doubleTapFeedbackText) {
        if (doubleTapFeedbackText != null) {
            delay(700L)
            doubleTapFeedbackText = null
        }
    }

    // Immersive system bars control
    LaunchedEffect(controlsVisible) {
        val window = activity?.window ?: return@LaunchedEffect
        val controller = WindowCompat.getInsetsController(window, view)
        controller.systemBarsBehavior = WindowInsetsControllerCompat.BEHAVIOR_SHOW_TRANSIENT_BARS_BY_SWIPE
        if (controlsVisible) {
            controller.show(WindowInsetsCompat.Type.systemBars())
        } else {
            controller.hide(WindowInsetsCompat.Type.systemBars())
        }
    }

    // Restore orientation, system bars, and persist progress on exit
    DisposableEffect(Unit) {
        onDispose {
            playerViewModel.pauseAndSave()
            activity?.let { act ->
                act.requestedOrientation = ActivityInfo.SCREEN_ORIENTATION_UNSPECIFIED
                val window = act.window
                WindowCompat.getInsetsController(window, view).show(WindowInsetsCompat.Type.systemBars())
                val lp = window.attributes
                if (lp.screenBrightness >= 0f) {
                    lp.screenBrightness = -1f
                    window.attributes = lp
                }
            }
        }
    }

    val contentScale = when (uiState.aspectMode) {
        AspectResizeOption.FIT -> ContentScale.Fit
        AspectResizeOption.FILL -> ContentScale.Crop
    }

    val activePositionMs = when {
        gestureSeekPreviewMs != null -> gestureSeekPreviewMs!!
        isSliderDragging -> sliderDragPositionMs
        else -> uiState.positionMs
    }

    Box(
        modifier = modifier
            .fillMaxSize()
            .background(Color.Black),
    ) {
        // Media3 Compose Video Surface
        ContentFrame(
            player = playerViewModel.player,
            contentScale = contentScale,
            modifier = Modifier.fillMaxSize(),
        )

        // Gesture Detection Layer
        Box(
            modifier = Modifier
                .fillMaxSize()
                .pointerInput(controlsLocked, doubleTapSeekSeconds) {
                    detectTapGestures(
                        onTap = {
                            if (controlsLocked) {
                                controlsVisible = !controlsVisible
                            } else {
                                controlsVisible = !controlsVisible
                            }
                        },
                        onDoubleTap = { offset ->
                            if (controlsLocked) return@detectTapGestures
                            val width = size.width.toFloat().coerceAtLeast(1f)
                            when {
                                offset.x < width * 0.36f -> {
                                    playerViewModel.seekBy(-doubleTapSeekSeconds * 1000L)
                                    doubleTapFeedbackText = "-${doubleTapSeekSeconds}s"
                                }
                                offset.x > width * 0.64f -> {
                                    playerViewModel.seekBy(doubleTapSeekSeconds * 1000L)
                                    doubleTapFeedbackText = "+${doubleTapSeekSeconds}s"
                                }
                                else -> {
                                    playerViewModel.togglePlayPause()
                                }
                            }
                        },
                    )
                }
                .pointerInput(controlsLocked, uiState.durationMs) {
                    if (controlsLocked) return@pointerInput
                    awaitEachGesture {
                        val down = awaitFirstDown(requireUnconsumed = false)
                        val startX = down.position.x
                        val viewWidth = size.width.toFloat().coerceAtLeast(1f)
                        val viewHeight = size.height.toFloat().coerceAtLeast(1f)

                        var mode = DragGestureMode.NONE
                        var totalDx = 0f
                        var totalDy = 0f
                        val startPosMs = uiState.positionMs

                        val maxVol = audioManager.getStreamMaxVolume(AudioManager.STREAM_MUSIC).coerceAtLeast(1)
                        val startVolNorm = audioManager.getStreamVolume(AudioManager.STREAM_MUSIC).toFloat() / maxVol
                        var volAccum = startVolNorm

                        val window = activity?.window
                        val initialBrightness = window?.attributes?.screenBrightness?.let {
                            if (it < 0f) 0.5f else it
                        } ?: 0.5f
                        var brightAccum = initialBrightness

                        while (true) {
                            val event = awaitPointerEvent()
                            val change = event.changes.firstOrNull { it.id == down.id } ?: break
                            if (!change.pressed) break

                            val delta = change.positionChange()
                            totalDx += delta.x
                            totalDy += delta.y

                            if (mode == DragGestureMode.NONE) {
                                val slop = 18f
                                if (abs(totalDx) > slop || abs(totalDy) > slop) {
                                    mode = if (abs(totalDx) > abs(totalDy)) {
                                        DragGestureMode.HORIZONTAL_SEEK
                                    } else if (startX < viewWidth / 2f) {
                                        DragGestureMode.LEFT_BRIGHTNESS
                                    } else {
                                        DragGestureMode.RIGHT_VOLUME
                                    }
                                }
                            }

                            when (mode) {
                                DragGestureMode.HORIZONTAL_SEEK -> {
                                    change.consume()
                                    val seekSpanMs = minOf(maxOf(uiState.durationMs, 60_000L), 180_000L)
                                    val deltaMs = ((totalDx / viewWidth) * seekSpanMs).toLong()
                                    val target = (startPosMs + deltaMs).coerceIn(
                                        0L,
                                        uiState.durationMs.takeIf { it > 0L } ?: Long.MAX_VALUE,
                                    )
                                    gestureSeekPreviewMs = target
                                    gestureSeekDeltaSec = ((target - startPosMs) / 1000L).toInt()
                                }
                                DragGestureMode.LEFT_BRIGHTNESS -> {
                                    change.consume()
                                    brightAccum = (brightAccum - (delta.y / viewHeight) * 1.4f).coerceIn(0.02f, 1f)
                                    gestureBrightnessLevel = brightAccum
                                    window?.let { w ->
                                        val lp = w.attributes
                                        lp.screenBrightness = brightAccum
                                        w.attributes = lp
                                    }
                                }
                                DragGestureMode.RIGHT_VOLUME -> {
                                    change.consume()
                                    volAccum = (volAccum - (delta.y / viewHeight) * 1.4f).coerceIn(0f, 1f)
                                    gestureVolumeLevel = volAccum
                                    val targetVol = (volAccum * maxVol).toInt().coerceIn(0, maxVol)
                                    audioManager.setStreamVolume(AudioManager.STREAM_MUSIC, targetVol, 0)
                                }
                                DragGestureMode.NONE -> Unit
                            }
                        }

                        if (mode == DragGestureMode.HORIZONTAL_SEEK && gestureSeekPreviewMs != null) {
                            playerViewModel.seekTo(gestureSeekPreviewMs!!)
                        }
                        gestureSeekPreviewMs = null
                        gestureBrightnessLevel = null
                        gestureVolumeLevel = null
                    }
                },
        )

        // Buffering Spinner
        if (uiState.isBuffering && uiState.errorMessage == null) {
            CircularProgressIndicator(
                color = MaterialTheme.colorScheme.primary,
                modifier = Modifier.align(Alignment.Center),
            )
        }

        // Gesture Feedback Badges (Seek / Brightness / Volume / Double-Tap)
        gestureSeekPreviewMs?.let { previewMs ->
            Surface(
                color = Color.Black.copy(alpha = 0.78f),
                shape = RoundedCornerShape(16.dp),
                modifier = Modifier.align(Alignment.Center),
            ) {
                Column(
                    horizontalAlignment = Alignment.CenterHorizontally,
                    modifier = Modifier.padding(horizontal = 24.dp, vertical = 14.dp),
                ) {
                    Text(
                        text = formatDuration(previewMs),
                        style = MaterialTheme.typography.headlineMedium,
                        color = Color.White,
                    )
                    Text(
                        text = formatSignedSeconds(gestureSeekDeltaSec),
                        style = MaterialTheme.typography.labelMedium,
                        color = MaterialTheme.colorScheme.secondary,
                    )
                }
            }
        }

        gestureBrightnessLevel?.let { level ->
            GestureMeterPill(
                icon = Icons.Default.BrightnessHigh,
                label = "${(level * 100).toInt()}%",
                modifier = Modifier.align(Alignment.Center),
            )
        }

        gestureVolumeLevel?.let { level ->
            GestureMeterPill(
                icon = Icons.AutoMirrored.Filled.VolumeUp,
                label = "${(level * 100).toInt()}%",
                modifier = Modifier.align(Alignment.Center),
            )
        }

        doubleTapFeedbackText?.let { feedback ->
            Surface(
                color = Color.Black.copy(alpha = 0.72f),
                shape = RoundedCornerShape(999.dp),
                modifier = Modifier
                    .align(if (feedback.startsWith("-")) Alignment.CenterStart else Alignment.CenterEnd)
                    .padding(horizontal = 42.dp),
            ) {
                Text(
                    text = feedback,
                    style = MaterialTheme.typography.titleLarge,
                    color = Color.White,
                    modifier = Modifier.padding(horizontal = 18.dp, vertical = 10.dp),
                )
            }
        }

        // Error State Overlay
        uiState.errorMessage?.let { errorMsg ->
            Surface(
                color = Color.Black.copy(alpha = 0.85f),
                shape = MaterialTheme.shapes.large,
                modifier = Modifier
                    .align(Alignment.Center)
                    .padding(24.dp),
            ) {
                Column(
                    horizontalAlignment = Alignment.CenterHorizontally,
                    verticalArrangement = Arrangement.spacedBy(12.dp),
                    modifier = Modifier.padding(24.dp),
                ) {
                    Text(
                        text = "Playback Error",
                        style = MaterialTheme.typography.titleLarge,
                        color = Color.White,
                    )
                    Text(
                        text = errorMsg,
                        style = MaterialTheme.typography.bodyMedium,
                        color = Color.White.copy(alpha = 0.78f),
                    )
                    Button(
                        onClick = { playerViewModel.retryPlayback() },
                        modifier = Modifier.testTag("retry_playback_button"),
                    ) {
                        Icon(Icons.Default.Refresh, contentDescription = null)
                        Spacer(modifier = Modifier.width(6.dp))
                        Text("Retry")
                    }
                }
            }
        }

        // Locked Controls Minimal Unlock Pill
        AnimatedVisibility(
            visible = controlsLocked && controlsVisible,
            enter = fadeIn(),
            exit = fadeOut(),
            modifier = Modifier
                .align(Alignment.TopStart)
                .windowInsetsPadding(WindowInsets.safeDrawing)
                .padding(16.dp),
        ) {
            Surface(
                color = Color.Black.copy(alpha = 0.75f),
                shape = RoundedCornerShape(999.dp),
                onClick = { controlsLocked = false },
                modifier = Modifier.testTag("unlock_controls_button"),
            ) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    modifier = Modifier.padding(horizontal = 14.dp, vertical = 10.dp),
                ) {
                    Icon(Icons.Default.Lock, contentDescription = null, tint = MaterialTheme.colorScheme.secondary)
                    Spacer(modifier = Modifier.width(8.dp))
                    Column {
                        Text(
                            text = stringResource(R.string.player_controls_locked),
                            style = MaterialTheme.typography.labelLarge,
                            color = Color.White,
                        )
                        Text(
                            text = stringResource(R.string.player_tap_to_unlock),
                            style = MaterialTheme.typography.bodySmall,
                            color = Color.White.copy(alpha = 0.7f),
                        )
                    }
                }
            }
        }

        // Full HUD Controls Overlay
        AnimatedVisibility(
            visible = controlsVisible && !controlsLocked,
            enter = fadeIn(),
            exit = fadeOut(),
            modifier = Modifier.fillMaxSize(),
        ) {
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .background(
                        Brush.verticalGradient(
                            colors = listOf(
                                Color.Black.copy(alpha = 0.74f),
                                Color.Black.copy(alpha = 0.16f),
                                Color.Black.copy(alpha = 0.16f),
                                Color.Black.copy(alpha = 0.84f),
                            )
                        )
                    )
                    .windowInsetsPadding(WindowInsets.safeDrawing),
            ) {
                // Top Bar
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    modifier = Modifier
                        .fillMaxWidth()
                        .align(Alignment.TopCenter)
                        .padding(horizontal = 12.dp, vertical = 8.dp),
                ) {
                    IconButton(
                        onClick = onBack,
                        modifier = Modifier
                            .minimumInteractiveComponentSize()
                            .testTag("player_back_button"),
                    ) {
                        Icon(
                            imageVector = Icons.AutoMirrored.Filled.ArrowBack,
                            contentDescription = "Back",
                            tint = Color.White,
                        )
                    }
                    Column(modifier = Modifier.weight(1f)) {
                        Text(
                            text = uiState.currentVideo?.title ?: "Playing Video",
                            style = MaterialTheme.typography.titleMedium,
                            color = Color.White,
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis,
                        )
                        val subtitleMeta = buildString {
                            if (uiState.videoWidth > 0 && uiState.videoHeight > 0) {
                                append("${uiState.videoWidth}×${uiState.videoHeight}")
                            } else {
                                append(uiState.currentVideo?.resolutionLabel ?: "VIDEO")
                            }
                            if (uiState.abLoopStartMs != null) {
                                append(" • A-B Loop")
                            }
                            uiState.sleepTimerRemainingSec?.let { sec ->
                                append(" • Sleep ${sec / 60}m")
                            }
                        }
                        Text(
                            text = subtitleMeta,
                            style = MaterialTheme.typography.labelSmall,
                            color = Color.White.copy(alpha = 0.75f),
                        )
                    }

                    IconButton(
                        onClick = { controlsLocked = true },
                        modifier = Modifier
                            .minimumInteractiveComponentSize()
                            .testTag("lock_controls_button"),
                    ) {
                        Icon(Icons.Default.LockOpen, contentDescription = "Lock controls", tint = Color.White)
                    }
                    IconButton(
                        onClick = { playerViewModel.toggleAspectMode() },
                        modifier = Modifier
                            .minimumInteractiveComponentSize()
                            .testTag("aspect_mode_button"),
                    ) {
                        Icon(Icons.Default.AspectRatio, contentDescription = "Toggle fit/fill", tint = Color.White)
                    }
                    IconButton(
                        onClick = {
                            isLandscapeForced = !isLandscapeForced
                            activity?.requestedOrientation = if (isLandscapeForced) {
                                ActivityInfo.SCREEN_ORIENTATION_SENSOR_LANDSCAPE
                            } else {
                                ActivityInfo.SCREEN_ORIENTATION_UNSPECIFIED
                            }
                        },
                        modifier = Modifier
                            .minimumInteractiveComponentSize()
                            .testTag("rotate_orientation_button"),
                    ) {
                        Icon(Icons.Default.ScreenRotation, contentDescription = "Toggle orientation", tint = Color.White)
                    }
                    IconButton(
                        onClick = { showTracksSheet = true },
                        modifier = Modifier
                            .minimumInteractiveComponentSize()
                            .testTag("tracks_button"),
                    ) {
                        Icon(Icons.Default.Subtitles, contentDescription = "Audio and subtitles", tint = Color.White)
                    }
                    IconButton(
                        onClick = { showOptionsSheet = true },
                        modifier = Modifier
                            .minimumInteractiveComponentSize()
                            .testTag("more_options_button"),
                    ) {
                        Icon(Icons.Default.MoreVert, contentDescription = "More player options", tint = Color.White)
                    }
                }

                // Center Transport Controls
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(20.dp),
                    modifier = Modifier.align(Alignment.Center),
                ) {
                    IconButton(
                        onClick = { playerViewModel.skipPrevious() },
                        enabled = uiState.hasPrevious,
                        modifier = Modifier
                            .size(52.dp)
                            .testTag("previous_video_button"),
                    ) {
                        Icon(
                            imageVector = Icons.Default.SkipPrevious,
                            contentDescription = "Previous video",
                            tint = if (uiState.hasPrevious) Color.White else Color.White.copy(alpha = 0.35f),
                            modifier = Modifier.size(32.dp),
                        )
                    }

                    IconButton(
                        onClick = { playerViewModel.seekBy(-doubleTapSeekSeconds * 1000L) },
                        modifier = Modifier
                            .size(54.dp)
                            .testTag("rewind_button"),
                    ) {
                        Icon(
                            imageVector = Icons.Default.Replay10,
                            contentDescription = "Rewind $doubleTapSeekSeconds seconds",
                            tint = Color.White,
                            modifier = Modifier.size(34.dp),
                        )
                    }

                    Surface(
                        color = MaterialTheme.colorScheme.primary,
                        shape = CircleShape,
                        onClick = { playerViewModel.togglePlayPause() },
                        modifier = Modifier
                            .size(72.dp)
                            .testTag("play_pause_button"),
                    ) {
                        Box(contentAlignment = Alignment.Center) {
                            Icon(
                                imageVector = if (uiState.isPlaying) Icons.Default.Pause else Icons.Default.PlayArrow,
                                contentDescription = if (uiState.isPlaying) "Pause" else "Play",
                                tint = MaterialTheme.colorScheme.onPrimary,
                                modifier = Modifier.size(40.dp),
                            )
                        }
                    }

                    IconButton(
                        onClick = { playerViewModel.seekBy(doubleTapSeekSeconds * 1000L) },
                        modifier = Modifier
                            .size(54.dp)
                            .testTag("forward_button"),
                    ) {
                        Icon(
                            imageVector = Icons.Default.Forward10,
                            contentDescription = "Forward $doubleTapSeekSeconds seconds",
                            tint = Color.White,
                            modifier = Modifier.size(34.dp),
                        )
                    }

                    IconButton(
                        onClick = { playerViewModel.skipNext() },
                        enabled = uiState.hasNext,
                        modifier = Modifier
                            .size(52.dp)
                            .testTag("next_video_button"),
                    ) {
                        Icon(
                            imageVector = Icons.Default.SkipNext,
                            contentDescription = "Next video",
                            tint = if (uiState.hasNext) Color.White else Color.White.copy(alpha = 0.35f),
                            modifier = Modifier.size(32.dp),
                        )
                    }
                }

                // Bottom Scrubber & Quick Utility Bar
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .align(Alignment.BottomCenter)
                        .padding(horizontal = 16.dp, vertical = 10.dp),
                ) {
                    val safeDuration = uiState.durationMs.coerceAtLeast(1L)
                    Slider(
                        value = activePositionMs.toFloat().coerceIn(0f, safeDuration.toFloat()),
                        onValueChange = { newVal ->
                            isSliderDragging = true
                            sliderDragPositionMs = newVal.toLong()
                        },
                        onValueChangeFinished = {
                            playerViewModel.seekTo(sliderDragPositionMs)
                            isSliderDragging = false
                        },
                        valueRange = 0f..safeDuration.toFloat(),
                        colors = SliderDefaults.colors(
                            thumbColor = MaterialTheme.colorScheme.primary,
                            activeTrackColor = MaterialTheme.colorScheme.primary,
                            inactiveTrackColor = Color.White.copy(alpha = 0.28f),
                        ),
                        modifier = Modifier
                            .fillMaxWidth()
                            .testTag("player_seek_slider"),
                    )

                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.SpaceBetween,
                        modifier = Modifier.fillMaxWidth(),
                    ) {
                        Text(
                            text = "${formatDuration(activePositionMs)} / ${formatDuration(uiState.durationMs)}",
                            style = MaterialTheme.typography.labelMedium,
                            color = Color.White,
                        )

                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(4.dp),
                        ) {
                            Surface(
                                color = Color.White.copy(alpha = 0.14f),
                                shape = RoundedCornerShape(999.dp),
                                onClick = {
                                    val nextSpeed = when (uiState.playbackSpeed) {
                                        1.0f -> 1.25f
                                        1.25f -> 1.5f
                                        1.5f -> 2.0f
                                        2.0f -> 0.75f
                                        else -> 1.0f
                                    }
                                    playerViewModel.setPlaybackSpeed(nextSpeed)
                                },
                                modifier = Modifier.testTag("speed_pill_button"),
                            ) {
                                Text(
                                    text = "${uiState.playbackSpeed}x",
                                    style = MaterialTheme.typography.labelMedium,
                                    color = Color.White,
                                    modifier = Modifier.padding(horizontal = 12.dp, vertical = 6.dp),
                                )
                            }

                            IconButton(
                                onClick = { playerViewModel.cycleRepeatMode() },
                                modifier = Modifier
                                    .minimumInteractiveComponentSize()
                                    .testTag("repeat_mode_button"),
                            ) {
                                Icon(
                                    imageVector = if (uiState.repeatMode == RepeatModeOption.ONE) {
                                        Icons.Default.RepeatOne
                                    } else {
                                        Icons.Default.Repeat
                                    },
                                    contentDescription = "Repeat mode: ${uiState.repeatMode.label}",
                                    tint = if (uiState.repeatMode == RepeatModeOption.OFF) {
                                        Color.White.copy(alpha = 0.72f)
                                    } else {
                                        MaterialTheme.colorScheme.secondary
                                    },
                                )
                            }

                            IconButton(
                                onClick = { playerViewModel.toggleCurrentFavorite() },
                                modifier = Modifier
                                    .minimumInteractiveComponentSize()
                                    .testTag("player_favorite_button"),
                            ) {
                                val fav = uiState.currentVideo?.isFavorite == true
                                Icon(
                                    imageVector = if (fav) Icons.Default.Favorite else Icons.Default.FavoriteBorder,
                                    contentDescription = "Toggle favorite",
                                    tint = if (fav) MaterialTheme.colorScheme.primary else Color.White,
                                )
                            }

                            if (uiState.queue.size > 1) {
                                IconButton(
                                    onClick = { showQueueSheet = true },
                                    modifier = Modifier
                                        .minimumInteractiveComponentSize()
                                        .testTag("queue_button"),
                                ) {
                                    Icon(
                                        imageVector = Icons.AutoMirrored.Filled.QueueMusic,
                                        contentDescription = "Show playback queue",
                                        tint = Color.White,
                                    )
                                }
                            }
                        }
                    }
                }
            }
        }
    }

    if (showQueueSheet) {
        QueueBottomSheet(
            uiState = uiState,
            onDismiss = { showQueueSheet = false },
            onSelectIndex = { idx -> playerViewModel.playQueueItem(idx) },
        )
    }

    if (showTracksSheet) {
        TracksBottomSheet(
            uiState = uiState,
            onDismiss = { showTracksSheet = false },
            onSelectAudio = { playerViewModel.selectAudioTrack(it) },
            onSelectSubtitle = { playerViewModel.selectSubtitleTrack(it) },
        )
    }

    if (showOptionsSheet) {
        PlayerOptionsSheet(
            uiState = uiState,
            onDismiss = { showOptionsSheet = false },
            onSetSpeed = { playerViewModel.setPlaybackSpeed(it) },
            onSetAbLoopPoint = { playerViewModel.setAbLoopPoint() },
            onClearAbLoop = { playerViewModel.clearAbLoop() },
            onSetSleepTimer = { playerViewModel.setSleepTimerMinutes(it) },
        )
    }
}

@Composable
private fun GestureMeterPill(
    icon: androidx.compose.ui.graphics.vector.ImageVector,
    label: String,
    modifier: Modifier = Modifier,
) {
    Surface(
        color = Color.Black.copy(alpha = 0.78f),
        shape = RoundedCornerShape(16.dp),
        modifier = modifier,
    ) {
        Row(
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(10.dp),
            modifier = Modifier.padding(horizontal = 20.dp, vertical = 12.dp),
        ) {
            Icon(icon, contentDescription = null, tint = MaterialTheme.colorScheme.secondary)
            Text(
                text = label,
                style = MaterialTheme.typography.titleMedium,
                color = Color.White,
            )
        }
    }
}

private tailrec fun Context.findActivity(): Activity? = when (this) {
    is Activity -> this
    is ContextWrapper -> baseContext.findActivity()
    else -> null
}
