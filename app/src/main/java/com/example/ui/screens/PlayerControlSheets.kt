package com.example.ui.screens

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.GraphicEq
import androidx.compose.material.icons.filled.Info
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material.icons.filled.Repeat
import androidx.compose.material.icons.filled.Speed
import androidx.compose.material.icons.filled.Subtitles
import androidx.compose.material.icons.filled.Timer
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FilterChip
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import com.example.player.MediaTrackOption
import com.example.player.PlayerUiState
import com.example.util.formatDuration

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun QueueBottomSheet(
    uiState: PlayerUiState,
    onDismiss: () -> Unit,
    onSelectIndex: (Int) -> Unit,
) {
    ModalBottomSheet(onDismissRequest = onDismiss) {
        LazyColumn(
            contentPadding = PaddingValues(start = 20.dp, end = 20.dp, bottom = 32.dp),
            verticalArrangement = Arrangement.spacedBy(8.dp),
            modifier = Modifier.fillMaxWidth(),
        ) {
            item {
                Text(
                    text = "Up Next (${uiState.queue.size})",
                    style = MaterialTheme.typography.titleLarge,
                )
                Spacer(modifier = Modifier.height(6.dp))
            }
            itemsIndexed(uiState.queue, key = { idx, item -> "${idx}_${item.id}" }) { index, video ->
                val isCurrent = index == uiState.currentIndex
                Surface(
                    color = if (isCurrent) {
                        MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.55f)
                    } else {
                        MaterialTheme.colorScheme.surfaceContainer
                    },
                    shape = MaterialTheme.shapes.small,
                    modifier = Modifier
                        .fillMaxWidth()
                        .clip(MaterialTheme.shapes.small)
                        .clickable {
                            onSelectIndex(index)
                            onDismiss()
                        },
                ) {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(horizontal = 14.dp, vertical = 12.dp),
                    ) {
                        Icon(
                            imageVector = if (isCurrent) Icons.Default.GraphicEq else Icons.Default.PlayArrow,
                            contentDescription = null,
                            tint = if (isCurrent) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurfaceVariant,
                        )
                        Spacer(modifier = Modifier.width(12.dp))
                        Column(modifier = Modifier.weight(1f)) {
                            Text(
                                text = video.title,
                                style = MaterialTheme.typography.titleSmall,
                                maxLines = 1,
                                overflow = TextOverflow.Ellipsis,
                            )
                            Text(
                                text = buildString {
                                    append(video.resolutionLabel)
                                    if (video.durationMs > 0L) {
                                        append(" • ")
                                        append(formatDuration(video.durationMs))
                                    }
                                },
                                style = MaterialTheme.typography.labelSmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant,
                            )
                        }
                    }
                }
            }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun TracksBottomSheet(
    uiState: PlayerUiState,
    onDismiss: () -> Unit,
    onSelectAudio: (MediaTrackOption) -> Unit,
    onSelectSubtitle: (MediaTrackOption?) -> Unit,
) {
    ModalBottomSheet(onDismissRequest = onDismiss) {
        Column(
            verticalArrangement = Arrangement.spacedBy(12.dp),
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 20.dp)
                .padding(bottom = 32.dp),
        ) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Icon(Icons.Default.GraphicEq, contentDescription = null, tint = MaterialTheme.colorScheme.primary)
                Spacer(modifier = Modifier.width(8.dp))
                Text("Audio Tracks", style = MaterialTheme.typography.titleMedium)
            }

            if (uiState.audioTracks.isEmpty()) {
                Text(
                    text = "Default audio stream active",
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
            } else {
                uiState.audioTracks.forEach { track ->
                    TrackChoiceRow(
                        label = track.label,
                        isSelected = track.isSelected,
                        onClick = {
                            onSelectAudio(track)
                            onDismiss()
                        },
                    )
                }
            }

            HorizontalDivider()

            Row(verticalAlignment = Alignment.CenterVertically) {
                Icon(Icons.Default.Subtitles, contentDescription = null, tint = MaterialTheme.colorScheme.secondary)
                Spacer(modifier = Modifier.width(8.dp))
                Text("Subtitles / Captions", style = MaterialTheme.typography.titleMedium)
            }

            TrackChoiceRow(
                label = "Off",
                isSelected = uiState.subtitlesDisabled,
                onClick = {
                    onSelectSubtitle(null)
                    onDismiss()
                },
            )
            uiState.subtitleTracks.forEach { track ->
                TrackChoiceRow(
                    label = track.label,
                    isSelected = track.isSelected,
                    onClick = {
                        onSelectSubtitle(track)
                        onDismiss()
                    },
                )
            }
        }
    }
}

@Composable
private fun TrackChoiceRow(
    label: String,
    isSelected: Boolean,
    onClick: () -> Unit,
) {
    Row(
        verticalAlignment = Alignment.CenterVertically,
        modifier = Modifier
            .fillMaxWidth()
            .clip(MaterialTheme.shapes.small)
            .clickable(onClick = onClick)
            .padding(vertical = 10.dp, horizontal = 8.dp),
    ) {
        Text(
            text = label,
            style = MaterialTheme.typography.bodyLarge,
            color = if (isSelected) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurface,
            modifier = Modifier.weight(1f),
        )
        if (isSelected) {
            Icon(
                imageVector = Icons.Default.Check,
                contentDescription = "Selected",
                tint = MaterialTheme.colorScheme.primary,
            )
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class, ExperimentalLayoutApi::class)
@Composable
fun PlayerOptionsSheet(
    uiState: PlayerUiState,
    onDismiss: () -> Unit,
    onSetSpeed: (Float) -> Unit,
    onSetAbLoopPoint: () -> Unit,
    onClearAbLoop: () -> Unit,
    onSetSleepTimer: (Int?) -> Unit,
) {
    ModalBottomSheet(onDismissRequest = onDismiss) {
        Column(
            verticalArrangement = Arrangement.spacedBy(14.dp),
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 20.dp)
                .padding(bottom = 32.dp),
        ) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Icon(Icons.Default.Speed, contentDescription = null, tint = MaterialTheme.colorScheme.primary)
                Spacer(modifier = Modifier.width(8.dp))
                Text("Playback Speed", style = MaterialTheme.typography.titleMedium)
            }
            FlowRow(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                listOf(0.5f, 0.75f, 1.0f, 1.25f, 1.5f, 2.0f).forEach { speed ->
                    FilterChip(
                        selected = uiState.playbackSpeed == speed,
                        onClick = { onSetSpeed(speed) },
                        label = { Text("${speed}x") },
                    )
                }
            }

            HorizontalDivider()

            Row(verticalAlignment = Alignment.CenterVertically) {
                Icon(Icons.Default.Repeat, contentDescription = null, tint = MaterialTheme.colorScheme.secondary)
                Spacer(modifier = Modifier.width(8.dp))
                Text("A-B Segment Loop", style = MaterialTheme.typography.titleMedium)
            }
            Row(
                horizontalArrangement = Arrangement.spacedBy(10.dp),
                verticalAlignment = Alignment.CenterVertically,
            ) {
                val loopLabel = when {
                    uiState.abLoopStartMs == null -> "Set Point A (${formatDuration(uiState.positionMs)})"
                    uiState.abLoopEndMs == null -> "Set Point B (A=${formatDuration(uiState.abLoopStartMs)})"
                    else -> "Active: ${formatDuration(uiState.abLoopStartMs)} → ${formatDuration(uiState.abLoopEndMs)}"
                }
                OutlinedButton(onClick = onSetAbLoopPoint) {
                    Text(loopLabel)
                }
                if (uiState.abLoopStartMs != null) {
                    OutlinedButton(onClick = onClearAbLoop) {
                        Text("Clear")
                    }
                }
            }

            HorizontalDivider()

            Row(verticalAlignment = Alignment.CenterVertically) {
                Icon(Icons.Default.Timer, contentDescription = null, tint = MaterialTheme.colorScheme.tertiary)
                Spacer(modifier = Modifier.width(8.dp))
                Text(
                    text = uiState.sleepTimerRemainingSec?.let {
                        "Sleep Timer (${it / 60}m ${it % 60}s left)"
                    } ?: "Sleep Timer",
                    style = MaterialTheme.typography.titleMedium,
                )
            }
            FlowRow(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                FilterChip(
                    selected = uiState.sleepTimerRemainingSec == null,
                    onClick = { onSetSleepTimer(null) },
                    label = { Text("Off") },
                )
                listOf(15, 30, 45, 60).forEach { mins ->
                    FilterChip(
                        selected = false,
                        onClick = { onSetSleepTimer(mins) },
                        label = { Text("${mins}m") },
                    )
                }
            }

            HorizontalDivider()

            Row(verticalAlignment = Alignment.CenterVertically) {
                Icon(Icons.Default.Info, contentDescription = null, tint = MaterialTheme.colorScheme.onSurfaceVariant)
                Spacer(modifier = Modifier.width(8.dp))
                val resText = if (uiState.videoWidth > 0 && uiState.videoHeight > 0) {
                    "${uiState.videoWidth}×${uiState.videoHeight}"
                } else {
                    "Adaptive"
                }
                Text(
                    text = "Stream: $resText • ${uiState.currentVideo?.mimeType ?: "video/*"}",
                    style = MaterialTheme.typography.labelMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
            }
        }
    }
}
