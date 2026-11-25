package com.example.ui.components

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.PlaylistAdd
import androidx.compose.material.icons.filled.DeleteOutline
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material.icons.filled.Favorite
import androidx.compose.material.icons.filled.FavoriteBorder
import androidx.compose.material.icons.filled.MoreVert
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material.icons.filled.Public
import androidx.compose.material.icons.filled.VideoFile
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.minimumInteractiveComponentSize
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import coil.compose.AsyncImage
import com.example.data.model.VideoItem
import com.example.data.model.VideoSourceType
import com.example.util.formatDuration
import java.io.File

@Composable
fun VideoThumbnailArt(
    video: VideoItem,
    modifier: Modifier = Modifier,
) {
    Box(
        modifier = modifier
            .background(MaterialTheme.colorScheme.surfaceContainerHighest),
    ) {
        if (!video.thumbnailPath.isNullOrBlank()) {
            AsyncImage(
                model = File(video.thumbnailPath),
                contentDescription = video.title,
                contentScale = ContentScale.Crop,
                modifier = Modifier.fillMaxSize(),
            )
        } else {
            val accentPrimary = MaterialTheme.colorScheme.primary
            val accentSecondary = MaterialTheme.colorScheme.secondary
            val accentTertiary = MaterialTheme.colorScheme.tertiary
            val surfaceBase = MaterialTheme.colorScheme.surfaceContainerHigh
            Canvas(modifier = Modifier.fillMaxSize()) {
                val isStream = video.sourceType == VideoSourceType.STREAM_URL ||
                    video.sourceType == VideoSourceType.DEMO_STREAM
                drawRect(
                    brush = Brush.linearGradient(
                        colors = listOf(
                            surfaceBase,
                            if (isStream) accentTertiary.copy(alpha = 0.22f) else accentPrimary.copy(alpha = 0.25f),
                            accentSecondary.copy(alpha = 0.16f),
                        ),
                        start = Offset.Zero,
                        end = Offset(size.width, size.height),
                    )
                )
                drawCircle(
                    brush = Brush.radialGradient(
                        colors = listOf(
                            (if (isStream) accentTertiary else accentPrimary).copy(alpha = 0.28f),
                            Color.Transparent,
                        ),
                        center = Offset(size.width * 0.78f, size.height * 0.28f),
                        radius = size.minDimension * 0.65f,
                    ),
                    center = Offset(size.width * 0.78f, size.height * 0.28f),
                    radius = size.minDimension * 0.65f,
                )
            }
            Icon(
                imageVector = if (video.sourceType == VideoSourceType.STREAM_URL ||
                    video.sourceType == VideoSourceType.DEMO_STREAM
                ) {
                    Icons.Default.Public
                } else {
                    Icons.Default.VideoFile
                },
                contentDescription = null,
                tint = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.45f),
                modifier = Modifier
                    .size(34.dp)
                    .align(Alignment.Center),
            )
        }

        // Subtle dark vignette at bottom for crisp badges
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .height(44.dp)
                .align(Alignment.BottomCenter)
                .background(
                    Brush.verticalGradient(
                        colors = listOf(Color.Transparent, Color.Black.copy(alpha = 0.72f))
                    )
                )
        )

        // Resolution chip top-left
        Surface(
            color = Color.Black.copy(alpha = 0.68f),
            shape = RoundedCornerShape(6.dp),
            modifier = Modifier
                .align(Alignment.TopStart)
                .padding(8.dp),
        ) {
            Text(
                text = video.resolutionLabel,
                style = MaterialTheme.typography.labelSmall,
                color = Color.White,
                modifier = Modifier.padding(horizontal = 6.dp, vertical = 3.dp),
            )
        }

        // Play indicator center-right or subtle circle
        Surface(
            color = MaterialTheme.colorScheme.primary.copy(alpha = 0.92f),
            shape = CircleShape,
            modifier = Modifier
                .align(Alignment.Center)
                .size(38.dp),
        ) {
            Box(contentAlignment = Alignment.Center) {
                Icon(
                    imageVector = Icons.Default.PlayArrow,
                    contentDescription = "Play ${video.title}",
                    tint = MaterialTheme.colorScheme.onPrimary,
                    modifier = Modifier.size(22.dp),
                )
            }
        }

        // Duration badge bottom-right
        if (video.durationMs > 0L) {
            Surface(
                color = Color.Black.copy(alpha = 0.76f),
                shape = RoundedCornerShape(6.dp),
                modifier = Modifier
                    .align(Alignment.BottomEnd)
                    .padding(8.dp),
            ) {
                Text(
                    text = formatDuration(video.durationMs),
                    style = MaterialTheme.typography.labelSmall,
                    color = Color.White,
                    modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp),
                )
            }
        }

        if (video.progressFraction > 0.02f) {
            LinearProgressIndicator(
                progress = { video.progressFraction },
                color = MaterialTheme.colorScheme.primary,
                trackColor = Color.White.copy(alpha = 0.22f),
                modifier = Modifier
                    .fillMaxWidth()
                    .height(3.dp)
                    .align(Alignment.BottomCenter),
            )
        }
    }
}

@Composable
fun VideoGridCard(
    video: VideoItem,
    onPlay: () -> Unit,
    onToggleFavorite: () -> Unit,
    onAddToPlaylist: () -> Unit,
    onRename: () -> Unit,
    onDelete: () -> Unit,
    modifier: Modifier = Modifier,
) {
    var menuExpanded by remember { mutableStateOf(false) }

    Card(
        shape = MaterialTheme.shapes.medium,
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.surfaceContainer,
        ),
        modifier = modifier
            .fillMaxWidth()
            .clip(MaterialTheme.shapes.medium)
            .clickable(onClick = onPlay)
            .testTag("video_card_${video.id}"),
    ) {
        Column {
            VideoThumbnailArt(
                video = video,
                modifier = Modifier
                    .fillMaxWidth()
                    .aspectRatio(16f / 9f),
            )
            Row(
                verticalAlignment = Alignment.CenterVertically,
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(start = 12.dp, end = 4.dp, top = 8.dp, bottom = 8.dp),
            ) {
                Column(modifier = Modifier.weight(1f)) {
                    Text(
                        text = video.title,
                        style = MaterialTheme.typography.titleSmall,
                        color = MaterialTheme.colorScheme.onSurface,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis,
                    )
                    Spacer(modifier = Modifier.height(2.dp))
                    val sourceLabel = when (video.sourceType) {
                        VideoSourceType.LOCAL_PICKER, VideoSourceType.LOCAL_FILE -> "Device Video"
                        VideoSourceType.STREAM_URL -> "Network Stream"
                        VideoSourceType.DEMO_STREAM -> "Sample Reel"
                    }
                    Text(
                        text = if (video.lastPositionMs > 2000L && video.durationMs > 0L) {
                            "$sourceLabel • ${formatDuration(video.lastPositionMs)} watched"
                        } else {
                            sourceLabel
                        },
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis,
                    )
                }

                IconButton(
                    onClick = onToggleFavorite,
                    modifier = Modifier
                        .minimumInteractiveComponentSize()
                        .testTag("favorite_button_${video.id}"),
                ) {
                    Icon(
                        imageVector = if (video.isFavorite) Icons.Default.Favorite else Icons.Default.FavoriteBorder,
                        contentDescription = if (video.isFavorite) "Remove favorite" else "Mark favorite",
                        tint = if (video.isFavorite) {
                            MaterialTheme.colorScheme.primary
                        } else {
                            MaterialTheme.colorScheme.onSurfaceVariant
                        },
                    )
                }

                Box {
                    IconButton(
                        onClick = { menuExpanded = true },
                        modifier = Modifier
                            .minimumInteractiveComponentSize()
                            .testTag("video_menu_${video.id}"),
                    ) {
                        Icon(
                            imageVector = Icons.Default.MoreVert,
                            contentDescription = "More options for ${video.title}",
                            tint = MaterialTheme.colorScheme.onSurfaceVariant,
                        )
                    }
                    VideoOptionsMenu(
                        expanded = menuExpanded,
                        onDismiss = { menuExpanded = false },
                        onPlay = onPlay,
                        onAddToPlaylist = onAddToPlaylist,
                        onRename = onRename,
                        onDelete = onDelete,
                    )
                }
            }
        }
    }
}

@Composable
fun VideoListRow(
    video: VideoItem,
    onPlay: () -> Unit,
    onToggleFavorite: () -> Unit,
    onAddToPlaylist: () -> Unit,
    onRename: () -> Unit,
    onDelete: () -> Unit,
    modifier: Modifier = Modifier,
) {
    var menuExpanded by remember { mutableStateOf(false) }

    Card(
        shape = MaterialTheme.shapes.medium,
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.surfaceContainer,
        ),
        modifier = modifier
            .fillMaxWidth()
            .clip(MaterialTheme.shapes.medium)
            .clickable(onClick = onPlay)
            .testTag("video_row_${video.id}"),
    ) {
        Row(
            verticalAlignment = Alignment.CenterVertically,
            modifier = Modifier
                .fillMaxWidth()
                .padding(10.dp),
        ) {
            VideoThumbnailArt(
                video = video,
                modifier = Modifier
                    .width(128.dp)
                    .aspectRatio(16f / 9f)
                    .clip(MaterialTheme.shapes.small),
            )
            Spacer(modifier = Modifier.width(12.dp))
            Column(
                verticalArrangement = Arrangement.spacedBy(4.dp),
                modifier = Modifier.weight(1f),
            ) {
                Text(
                    text = video.title,
                    style = MaterialTheme.typography.titleSmall,
                    color = MaterialTheme.colorScheme.onSurface,
                    maxLines = 2,
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
            IconButton(
                onClick = onToggleFavorite,
                modifier = Modifier.minimumInteractiveComponentSize(),
            ) {
                Icon(
                    imageVector = if (video.isFavorite) Icons.Default.Favorite else Icons.Default.FavoriteBorder,
                    contentDescription = if (video.isFavorite) "Remove favorite" else "Mark favorite",
                    tint = if (video.isFavorite) {
                        MaterialTheme.colorScheme.primary
                    } else {
                        MaterialTheme.colorScheme.onSurfaceVariant
                    },
                )
            }
            Box {
                IconButton(
                    onClick = { menuExpanded = true },
                    modifier = Modifier.minimumInteractiveComponentSize(),
                ) {
                    Icon(
                        imageVector = Icons.Default.MoreVert,
                        contentDescription = "More options",
                        tint = MaterialTheme.colorScheme.onSurfaceVariant,
                    )
                }
                VideoOptionsMenu(
                    expanded = menuExpanded,
                    onDismiss = { menuExpanded = false },
                    onPlay = onPlay,
                    onAddToPlaylist = onAddToPlaylist,
                    onRename = onRename,
                    onDelete = onDelete,
                )
            }
        }
    }
}

@Composable
private fun VideoOptionsMenu(
    expanded: Boolean,
    onDismiss: () -> Unit,
    onPlay: () -> Unit,
    onAddToPlaylist: () -> Unit,
    onRename: () -> Unit,
    onDelete: () -> Unit,
) {
    DropdownMenu(expanded = expanded, onDismissRequest = onDismiss) {
        DropdownMenuItem(
            text = { Text("Play now") },
            leadingIcon = { Icon(Icons.Default.PlayArrow, contentDescription = null) },
            onClick = {
                onDismiss()
                onPlay()
            },
        )
        DropdownMenuItem(
            text = { Text("Add to playlist") },
            leadingIcon = { Icon(Icons.AutoMirrored.Filled.PlaylistAdd, contentDescription = null) },
            onClick = {
                onDismiss()
                onAddToPlaylist()
            },
        )
        DropdownMenuItem(
            text = { Text("Rename") },
            leadingIcon = { Icon(Icons.Default.Edit, contentDescription = null) },
            onClick = {
                onDismiss()
                onRename()
            },
        )
        DropdownMenuItem(
            text = { Text("Remove from library") },
            leadingIcon = { Icon(Icons.Default.DeleteOutline, contentDescription = null) },
            onClick = {
                onDismiss()
                onDelete()
            },
        )
    }
}
