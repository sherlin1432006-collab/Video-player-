package com.example.ui.screens

import androidx.compose.foundation.clickable
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
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.PlaylistAdd
import androidx.compose.material.icons.automirrored.filled.PlaylistPlay
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.DeleteOutline
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material.icons.filled.MoreVert
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material.icons.filled.RemoveCircleOutline
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FloatingActionButton
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.minimumInteractiveComponentSize
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import com.example.R
import com.example.data.model.PlaylistWithVideos
import com.example.data.model.VideoItem
import com.example.ui.components.RenameDialog
import com.example.ui.components.VideoThumbnailArt
import com.example.ui.viewmodel.PlaylistsUiState
import com.example.ui.viewmodel.PlaylistsViewModel
import com.example.util.formatDuration

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun PlaylistsScreen(
    uiState: PlaylistsUiState,
    viewModel: PlaylistsViewModel,
    onPlayFromPlaylist: (videoId: Long, playlistId: Long) -> Unit,
    modifier: Modifier = Modifier,
) {
    var showCreateDialog by remember { mutableStateOf(false) }
    var playlistToRename by remember { mutableStateOf<PlaylistWithVideos?>(null) }
    var selectedPlaylistForSheet by remember { mutableStateOf<PlaylistWithVideos?>(null) }

    Scaffold(
        modifier = modifier.fillMaxSize(),
        containerColor = Color.Transparent,
        floatingActionButton = {
            FloatingActionButton(
                onClick = { showCreateDialog = true },
                containerColor = MaterialTheme.colorScheme.primary,
                contentColor = MaterialTheme.colorScheme.onPrimary,
                modifier = Modifier.testTag("create_playlist_fab"),
            ) {
                Icon(Icons.Default.Add, contentDescription = "Create playlist")
            }
        },
    ) { innerPadding ->
        LazyColumn(
            contentPadding = PaddingValues(
                start = 16.dp,
                end = 16.dp,
                top = innerPadding.calculateTopPadding() + 16.dp,
                bottom = innerPadding.calculateBottomPadding() + 96.dp,
            ),
            verticalArrangement = Arrangement.spacedBy(14.dp),
            modifier = Modifier.fillMaxSize(),
        ) {
            item(key = "playlists_header") {
                Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
                    Text(
                        text = stringResource(R.string.nav_playlists),
                        style = MaterialTheme.typography.headlineMedium,
                    )
                    Text(
                        text = "Organize local clips and network streams into seamless playback queues.",
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                    )
                }
            }

            if (uiState.playlists.isEmpty()) {
                item(key = "playlists_empty") {
                    Card(
                        shape = MaterialTheme.shapes.large,
                        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceContainer),
                        modifier = Modifier
                            .fillMaxWidth()
                            .widthIn(max = 640.dp),
                    ) {
                        Column(
                            horizontalAlignment = Alignment.CenterHorizontally,
                            verticalArrangement = Arrangement.spacedBy(12.dp),
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(28.dp),
                        ) {
                            Surface(
                                color = MaterialTheme.colorScheme.secondaryContainer,
                                shape = CircleShape,
                                modifier = Modifier.size(60.dp),
                            ) {
                                Box(contentAlignment = Alignment.Center) {
                                    Icon(
                                        imageVector = Icons.AutoMirrored.Filled.PlaylistPlay,
                                        contentDescription = null,
                                        tint = MaterialTheme.colorScheme.onSecondaryContainer,
                                        modifier = Modifier.size(30.dp),
                                    )
                                }
                            }
                            Text(
                                text = stringResource(R.string.empty_playlists_title),
                                style = MaterialTheme.typography.titleLarge,
                            )
                            Text(
                                text = stringResource(R.string.empty_playlists_body),
                                style = MaterialTheme.typography.bodyMedium,
                                color = MaterialTheme.colorScheme.onSurfaceVariant,
                            )
                            Button(
                                onClick = { showCreateDialog = true },
                                modifier = Modifier.testTag("empty_create_playlist_button"),
                            ) {
                                Icon(Icons.Default.Add, contentDescription = null)
                                Spacer(modifier = Modifier.width(6.dp))
                                Text("Create Playlist")
                            }
                        }
                    }
                }
            } else {
                items(uiState.playlists, key = { it.playlist.id }) { item ->
                    PlaylistCard(
                        playlistWithVideos = item,
                        onPlayAll = {
                            item.videos.firstOrNull()?.let { first ->
                                onPlayFromPlaylist(first.id, item.playlist.id)
                            }
                        },
                        onManage = { selectedPlaylistForSheet = item },
                        onRename = { playlistToRename = item },
                        onDelete = { viewModel.deletePlaylist(item.playlist.id) },
                    )
                }
            }
        }
    }

    if (showCreateDialog) {
        var newName by remember { mutableStateOf("") }
        AlertDialog(
            onDismissRequest = { showCreateDialog = false },
            title = { Text(stringResource(R.string.dialog_create_playlist_title)) },
            text = {
                OutlinedTextField(
                    value = newName,
                    onValueChange = { newName = it },
                    label = { Text(stringResource(R.string.dialog_playlist_name_label)) },
                    singleLine = true,
                    modifier = Modifier
                        .fillMaxWidth()
                        .testTag("create_playlist_name_input"),
                )
            },
            confirmButton = {
                Button(
                    onClick = {
                        viewModel.createPlaylist(newName)
                        showCreateDialog = false
                    },
                    enabled = newName.isNotBlank(),
                    modifier = Modifier.testTag("create_playlist_confirm_button"),
                ) {
                    Text("Create")
                }
            },
            dismissButton = {
                TextButton(onClick = { showCreateDialog = false }) {
                    Text("Cancel")
                }
            },
        )
    }

    playlistToRename?.let { target ->
        RenameDialog(
            title = "Rename Playlist",
            initialValue = target.playlist.name,
            label = "Playlist Name",
            onDismiss = { playlistToRename = null },
            onConfirm = { name ->
                viewModel.renamePlaylist(target.playlist.id, name)
                playlistToRename = null
            },
        )
    }

    selectedPlaylistForSheet?.let { selected ->
        val latest = uiState.playlists.find { it.playlist.id == selected.playlist.id } ?: selected
        PlaylistManageSheet(
            playlistWithVideos = latest,
            libraryVideos = uiState.allVideos,
            onDismiss = { selectedPlaylistForSheet = null },
            onPlayVideo = { video ->
                selectedPlaylistForSheet = null
                onPlayFromPlaylist(video.id, latest.playlist.id)
            },
            onAddVideo = { video ->
                viewModel.addVideoToPlaylist(latest.playlist.id, video.id)
            },
            onRemoveVideo = { video ->
                viewModel.removeVideoFromPlaylist(latest.playlist.id, video.id)
            },
        )
    }
}

@Composable
private fun PlaylistCard(
    playlistWithVideos: PlaylistWithVideos,
    onPlayAll: () -> Unit,
    onManage: () -> Unit,
    onRename: () -> Unit,
    onDelete: () -> Unit,
) {
    var menuOpen by remember { mutableStateOf(false) }
    val totalDurationMs = remember(playlistWithVideos.videos) {
        playlistWithVideos.videos.sumOf { it.durationMs }
    }

    Card(
        shape = MaterialTheme.shapes.medium,
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceContainer),
        modifier = Modifier
            .fillMaxWidth()
            .widthIn(max = 760.dp)
            .clip(MaterialTheme.shapes.medium)
            .clickable(onClick = onManage)
            .testTag("playlist_card_${playlistWithVideos.playlist.id}"),
    ) {
        Row(
            verticalAlignment = Alignment.CenterVertically,
            modifier = Modifier
                .fillMaxWidth()
                .padding(12.dp),
        ) {
            val coverVideo = playlistWithVideos.videos.firstOrNull()
            if (coverVideo != null) {
                VideoThumbnailArt(
                    video = coverVideo,
                    modifier = Modifier
                        .width(120.dp)
                        .aspectRatio(16f / 9f)
                        .clip(MaterialTheme.shapes.small),
                )
            } else {
                Surface(
                    color = MaterialTheme.colorScheme.surfaceContainerHigh,
                    shape = MaterialTheme.shapes.small,
                    modifier = Modifier
                        .width(120.dp)
                        .aspectRatio(16f / 9f),
                ) {
                    Box(contentAlignment = Alignment.Center) {
                        Icon(
                            imageVector = Icons.AutoMirrored.Filled.PlaylistPlay,
                            contentDescription = null,
                            tint = MaterialTheme.colorScheme.onSurfaceVariant,
                            modifier = Modifier.size(32.dp),
                        )
                    }
                }
            }

            Spacer(modifier = Modifier.width(14.dp))

            Column(
                verticalArrangement = Arrangement.spacedBy(4.dp),
                modifier = Modifier.weight(1f),
            ) {
                Text(
                    text = playlistWithVideos.playlist.name,
                    style = MaterialTheme.typography.titleMedium,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                )
                Text(
                    text = buildString {
                        append("${playlistWithVideos.videos.size} clips")
                        if (totalDurationMs > 0L) {
                            append(" • ")
                            append(formatDuration(totalDurationMs))
                        }
                    },
                    style = MaterialTheme.typography.labelSmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
            }

            if (playlistWithVideos.videos.isNotEmpty()) {
                IconButton(
                    onClick = onPlayAll,
                    modifier = Modifier
                        .minimumInteractiveComponentSize()
                        .testTag("play_playlist_${playlistWithVideos.playlist.id}"),
                ) {
                    Icon(
                        imageVector = Icons.Default.PlayArrow,
                        contentDescription = "Play ${playlistWithVideos.playlist.name}",
                        tint = MaterialTheme.colorScheme.primary,
                    )
                }
            }

            Box {
                IconButton(
                    onClick = { menuOpen = true },
                    modifier = Modifier.minimumInteractiveComponentSize(),
                ) {
                    Icon(
                        imageVector = Icons.Default.MoreVert,
                        contentDescription = "Playlist options",
                    )
                }
                DropdownMenu(expanded = menuOpen, onDismissRequest = { menuOpen = false }) {
                    DropdownMenuItem(
                        text = { Text("Manage clips") },
                        leadingIcon = { Icon(Icons.AutoMirrored.Filled.PlaylistAdd, contentDescription = null) },
                        onClick = {
                            menuOpen = false
                            onManage()
                        },
                    )
                    DropdownMenuItem(
                        text = { Text("Rename") },
                        leadingIcon = { Icon(Icons.Default.Edit, contentDescription = null) },
                        onClick = {
                            menuOpen = false
                            onRename()
                        },
                    )
                    DropdownMenuItem(
                        text = { Text("Delete playlist") },
                        leadingIcon = { Icon(Icons.Default.DeleteOutline, contentDescription = null) },
                        onClick = {
                            menuOpen = false
                            onDelete()
                        },
                    )
                }
            }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun PlaylistManageSheet(
    playlistWithVideos: PlaylistWithVideos,
    libraryVideos: List<VideoItem>,
    onDismiss: () -> Unit,
    onPlayVideo: (VideoItem) -> Unit,
    onAddVideo: (VideoItem) -> Unit,
    onRemoveVideo: (VideoItem) -> Unit,
) {
    val existingIds = remember(playlistWithVideos.videos) {
        playlistWithVideos.videos.map { it.id }.toSet()
    }
    val availableToAdd = remember(libraryVideos, existingIds) {
        libraryVideos.filter { it.id !in existingIds }
    }

    ModalBottomSheet(onDismissRequest = onDismiss) {
        LazyColumn(
            contentPadding = PaddingValues(start = 20.dp, end = 20.dp, bottom = 32.dp),
            verticalArrangement = Arrangement.spacedBy(10.dp),
            modifier = Modifier.fillMaxWidth(),
        ) {
            item {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.SpaceBetween,
                    modifier = Modifier.fillMaxWidth(),
                ) {
                    Text(
                        text = playlistWithVideos.playlist.name,
                        style = MaterialTheme.typography.titleLarge,
                    )
                    if (playlistWithVideos.videos.isNotEmpty()) {
                        OutlinedButton(onClick = { onPlayVideo(playlistWithVideos.videos.first()) }) {
                            Icon(Icons.Default.PlayArrow, contentDescription = null)
                            Spacer(modifier = Modifier.width(4.dp))
                            Text("Play Queue")
                        }
                    }
                }
            }

            if (playlistWithVideos.videos.isEmpty()) {
                item {
                    Text(
                        text = "No videos in this playlist yet. Add clips from your library below.",
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                    )
                }
            } else {
                items(playlistWithVideos.videos, key = { "in_${it.id}" }) { video ->
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        modifier = Modifier
                            .fillMaxWidth()
                            .clip(MaterialTheme.shapes.small)
                            .clickable { onPlayVideo(video) }
                            .padding(vertical = 6.dp),
                    ) {
                        Icon(
                            imageVector = Icons.Default.PlayArrow,
                            contentDescription = null,
                            tint = MaterialTheme.colorScheme.primary,
                        )
                        Spacer(modifier = Modifier.width(10.dp))
                        Text(
                            text = video.title,
                            style = MaterialTheme.typography.bodyLarge,
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis,
                            modifier = Modifier.weight(1f),
                        )
                        IconButton(onClick = { onRemoveVideo(video) }) {
                            Icon(
                                imageVector = Icons.Default.RemoveCircleOutline,
                                contentDescription = "Remove ${video.title}",
                            )
                        }
                    }
                }
            }

            if (availableToAdd.isNotEmpty()) {
                item {
                    Spacer(modifier = Modifier.height(6.dp))
                    HorizontalDivider()
                    Spacer(modifier = Modifier.height(8.dp))
                    Text(
                        text = "Add from Library",
                        style = MaterialTheme.typography.titleMedium,
                    )
                }
                items(availableToAdd, key = { "avail_${it.id}" }) { video ->
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        modifier = Modifier
                            .fillMaxWidth()
                            .clip(MaterialTheme.shapes.small)
                            .clickable { onAddVideo(video) }
                            .padding(vertical = 8.dp),
                    ) {
                        Icon(
                            imageVector = Icons.Default.Add,
                            contentDescription = null,
                            tint = MaterialTheme.colorScheme.secondary,
                        )
                        Spacer(modifier = Modifier.width(10.dp))
                        Text(
                            text = video.title,
                            style = MaterialTheme.typography.bodyMedium,
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis,
                            modifier = Modifier.weight(1f),
                        )
                    }
                }
            }
        }
    }
}
