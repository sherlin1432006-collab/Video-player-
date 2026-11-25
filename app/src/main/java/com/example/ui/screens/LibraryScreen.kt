package com.example.ui.screens

import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.PickVisualMediaRequest
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
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
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ViewList
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Clear
import androidx.compose.material.icons.filled.FolderOpen
import androidx.compose.material.icons.filled.GridView
import androidx.compose.material.icons.filled.Link
import androidx.compose.material.icons.filled.Movie
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material.icons.filled.Search
import androidx.compose.material.icons.filled.VideoLibrary
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FilledTonalButton
import androidx.compose.material3.FilterChip
import androidx.compose.material3.FilterChipDefaults
import androidx.compose.material3.FloatingActionButton
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.minimumInteractiveComponentSize
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import com.example.R
import com.example.data.model.VideoItem
import com.example.data.model.VideoSourceType
import com.example.ui.components.AddStreamDialog
import com.example.ui.components.AddToPlaylistSheet
import com.example.ui.components.RenameDialog
import com.example.ui.components.VideoGridCard
import com.example.ui.components.VideoListRow
import com.example.ui.components.VideoThumbnailArt
import com.example.ui.viewmodel.LibraryFilter
import com.example.ui.viewmodel.LibraryUiState
import com.example.ui.viewmodel.LibraryViewModel
import com.example.ui.viewmodel.PlaylistsViewModel
import com.example.util.formatDuration

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun LibraryScreen(
    uiState: LibraryUiState,
    libraryViewModel: LibraryViewModel,
    playlistsViewModel: PlaylistsViewModel,
    onPlayVideo: (Long) -> Unit,
    modifier: Modifier = Modifier,
) {
    val snackbarHostState = remember { SnackbarHostState() }
    var showAddStreamDialog by remember { mutableStateOf(false) }
    var showFabMenu by remember { mutableStateOf(false) }
    var videoToRename by remember { mutableStateOf<VideoItem?>(null) }
    var videoForPlaylist by remember { mutableStateOf<VideoItem?>(null) }

    val photoPickerLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.PickMultipleVisualMedia(20),
    ) { uris ->
        if (uris.isNotEmpty()) {
            libraryViewModel.importLocalVideos(uris, VideoSourceType.LOCAL_PICKER)
        }
    }

    val openDocumentLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.OpenDocument(),
    ) { uri ->
        if (uri != null) {
            libraryViewModel.importLocalVideos(
                uris = listOf(uri),
                sourceType = VideoSourceType.LOCAL_FILE,
                onImportedFirst = { videoId -> onPlayVideo(videoId) },
            )
        }
    }

    LaunchedEffect(uiState.message) {
        val msg = uiState.message
        if (msg != null) {
            snackbarHostState.showSnackbar(msg)
            libraryViewModel.clearMessage()
        }
    }

    Scaffold(
        modifier = modifier.fillMaxSize(),
        containerColor = Color.Transparent,
        snackbarHost = { SnackbarHost(hostState = snackbarHostState) },
        floatingActionButton = {
            Box {
                FloatingActionButton(
                    onClick = { showFabMenu = true },
                    containerColor = MaterialTheme.colorScheme.primary,
                    contentColor = MaterialTheme.colorScheme.onPrimary,
                    modifier = Modifier.testTag("add_video_fab"),
                ) {
                    Icon(Icons.Default.Add, contentDescription = "Add video")
                }

                DropdownMenu(
                    expanded = showFabMenu,
                    onDismissRequest = { showFabMenu = false },
                ) {
                    DropdownMenuItem(
                        text = { Text(stringResource(R.string.action_pick_video)) },
                        leadingIcon = { Icon(Icons.Default.VideoLibrary, contentDescription = null) },
                        onClick = {
                            showFabMenu = false
                            photoPickerLauncher.launch(
                                PickVisualMediaRequest(ActivityResultContracts.PickVisualMedia.VideoOnly)
                            )
                        },
                    )
                    DropdownMenuItem(
                        text = { Text(stringResource(R.string.action_browse_files)) },
                        leadingIcon = { Icon(Icons.Default.FolderOpen, contentDescription = null) },
                        onClick = {
                            showFabMenu = false
                            openDocumentLauncher.launch(arrayOf("video/*"))
                        },
                    )
                    DropdownMenuItem(
                        text = { Text(stringResource(R.string.action_add_stream)) },
                        leadingIcon = { Icon(Icons.Default.Link, contentDescription = null) },
                        onClick = {
                            showFabMenu = false
                            showAddStreamDialog = true
                        },
                    )
                    DropdownMenuItem(
                        text = { Text(stringResource(R.string.action_load_samples)) },
                        leadingIcon = { Icon(Icons.Default.Movie, contentDescription = null) },
                        onClick = {
                            showFabMenu = false
                            libraryViewModel.loadSampleReels()
                        },
                    )
                }
            }
        },
    ) { innerPadding ->
        BoxWithConstraints(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding),
        ) {
            val columnCount = when {
                !uiState.isGridMode -> 1
                maxWidth >= 880.dp -> 3
                maxWidth >= 560.dp -> 2
                else -> 2
            }

            val videoRows = remember(uiState.videos, columnCount) {
                uiState.videos.chunked(columnCount)
            }

            LazyColumn(
                contentPadding = PaddingValues(bottom = 96.dp),
                verticalArrangement = Arrangement.spacedBy(16.dp),
                modifier = Modifier.fillMaxSize(),
            ) {
                item(key = "hero_banner") {
                    CinemaHeroBanner(
                        onPickVideo = {
                            photoPickerLauncher.launch(
                                PickVisualMediaRequest(ActivityResultContracts.PickVisualMedia.VideoOnly)
                            )
                        },
                        onBrowseFiles = {
                            openDocumentLauncher.launch(arrayOf("video/*"))
                        },
                        onOpenStream = { showAddStreamDialog = true },
                        modifier = Modifier.padding(horizontal = 16.dp, vertical = 8.dp),
                    )
                }

                item(key = "importing_indicator") {
                    AnimatedVisibility(visible = uiState.isImporting) {
                        LinearProgressIndicator(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(horizontal = 16.dp),
                        )
                    }
                }

                if (uiState.recentlyPlayed.isNotEmpty() && uiState.searchQuery.isBlank()) {
                    item(key = "continue_watching_header") {
                        Text(
                            text = stringResource(R.string.continue_watching),
                            style = MaterialTheme.typography.titleLarge,
                            modifier = Modifier.padding(horizontal = 16.dp),
                        )
                    }

                    item(key = "continue_watching_rail") {
                        LazyRow(
                            contentPadding = PaddingValues(horizontal = 16.dp),
                            horizontalArrangement = Arrangement.spacedBy(12.dp),
                        ) {
                            items(uiState.recentlyPlayed, key = { "recent_${it.id}" }) { video ->
                                ContinueWatchingCard(
                                    video = video,
                                    onClick = { onPlayVideo(video.id) },
                                )
                            }
                        }
                    }
                }

                item(key = "search_and_filters") {
                    Column(
                        verticalArrangement = Arrangement.spacedBy(12.dp),
                        modifier = Modifier.padding(horizontal = 16.dp),
                    ) {
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.SpaceBetween,
                            modifier = Modifier.fillMaxWidth(),
                        ) {
                            Text(
                                text = stringResource(R.string.all_videos),
                                style = MaterialTheme.typography.titleLarge,
                            )
                            IconButton(
                                onClick = { libraryViewModel.toggleGridMode() },
                                modifier = Modifier
                                    .minimumInteractiveComponentSize()
                                    .testTag("toggle_layout_button"),
                            ) {
                                Icon(
                                    imageVector = if (uiState.isGridMode) {
                                        Icons.AutoMirrored.Filled.ViewList
                                    } else {
                                        Icons.Default.GridView
                                    },
                                    contentDescription = if (uiState.isGridMode) "Switch to list view" else "Switch to grid view",
                                )
                            }
                        }

                        OutlinedTextField(
                            value = uiState.searchQuery,
                            onValueChange = { libraryViewModel.setSearchQuery(it) },
                            placeholder = { Text(stringResource(R.string.search_videos_hint)) },
                            leadingIcon = { Icon(Icons.Default.Search, contentDescription = null) },
                            trailingIcon = {
                                if (uiState.searchQuery.isNotEmpty()) {
                                    IconButton(onClick = { libraryViewModel.setSearchQuery("") }) {
                                        Icon(Icons.Default.Clear, contentDescription = "Clear search")
                                    }
                                }
                            },
                            singleLine = true,
                            shape = MaterialTheme.shapes.medium,
                            colors = OutlinedTextFieldDefaults.colors(
                                unfocusedContainerColor = MaterialTheme.colorScheme.surfaceContainer,
                                focusedContainerColor = MaterialTheme.colorScheme.surfaceContainerHigh,
                            ),
                            modifier = Modifier
                                .fillMaxWidth()
                                .testTag("library_search_input"),
                        )

                        LazyRow(
                            horizontalArrangement = Arrangement.spacedBy(8.dp),
                        ) {
                            items(LibraryFilter.entries) { filter ->
                                val selected = uiState.activeFilter == filter
                                FilterChip(
                                    selected = selected,
                                    onClick = { libraryViewModel.setFilter(filter) },
                                    label = { Text(filter.label) },
                                    colors = FilterChipDefaults.filterChipColors(
                                        selectedContainerColor = MaterialTheme.colorScheme.primaryContainer,
                                        selectedLabelColor = MaterialTheme.colorScheme.onPrimaryContainer,
                                    ),
                                    modifier = Modifier.testTag("filter_chip_${filter.name.lowercase()}"),
                                )
                            }
                        }
                    }
                }

                if (uiState.videos.isEmpty()) {
                    item(key = "empty_state") {
                        LibraryEmptyState(
                            hasSearchOrFilter = uiState.searchQuery.isNotBlank() || uiState.activeFilter != LibraryFilter.ALL,
                            onClearFilters = {
                                libraryViewModel.setSearchQuery("")
                                libraryViewModel.setFilter(LibraryFilter.ALL)
                            },
                            onPickVideo = {
                                photoPickerLauncher.launch(
                                    PickVisualMediaRequest(ActivityResultContracts.PickVisualMedia.VideoOnly)
                                )
                            },
                            onBrowseFiles = {
                                openDocumentLauncher.launch(arrayOf("video/*"))
                            },
                            onLoadSamples = { libraryViewModel.loadSampleReels() },
                            onOpenStream = { showAddStreamDialog = true },
                            modifier = Modifier.padding(horizontal = 16.dp),
                        )
                    }
                } else {
                    items(
                        items = videoRows,
                        key = { row -> row.joinToString("_") { it.id.toString() } },
                    ) { rowItems ->
                        Row(
                            horizontalArrangement = Arrangement.spacedBy(12.dp),
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(horizontal = 16.dp),
                        ) {
                            rowItems.forEach { video ->
                                Box(modifier = Modifier.weight(1f)) {
                                    if (uiState.isGridMode) {
                                        VideoGridCard(
                                            video = video,
                                            onPlay = { onPlayVideo(video.id) },
                                            onToggleFavorite = { libraryViewModel.toggleFavorite(video) },
                                            onAddToPlaylist = { videoForPlaylist = video },
                                            onRename = { videoToRename = video },
                                            onDelete = { libraryViewModel.deleteVideo(video) },
                                        )
                                    } else {
                                        VideoListRow(
                                            video = video,
                                            onPlay = { onPlayVideo(video.id) },
                                            onToggleFavorite = { libraryViewModel.toggleFavorite(video) },
                                            onAddToPlaylist = { videoForPlaylist = video },
                                            onRename = { videoToRename = video },
                                            onDelete = { libraryViewModel.deleteVideo(video) },
                                        )
                                    }
                                }
                            }
                            repeat(columnCount - rowItems.size) {
                                Spacer(modifier = Modifier.weight(1f))
                            }
                        }
                    }
                }
            }
        }
    }

    if (showAddStreamDialog) {
        AddStreamDialog(
            onDismiss = { showAddStreamDialog = false },
            onAddAndPlay = { url, title ->
                showAddStreamDialog = false
                libraryViewModel.addStreamUrl(url, title) { addedId ->
                    onPlayVideo(addedId)
                }
            },
        )
    }

    videoToRename?.let { target ->
        RenameDialog(
            title = "Rename Video",
            initialValue = target.title,
            label = "Video Title",
            onDismiss = { videoToRename = null },
            onConfirm = { updated ->
                libraryViewModel.renameVideo(target.id, updated)
                videoToRename = null
            },
        )
    }

    videoForPlaylist?.let { target ->
        AddToPlaylistSheet(
            playlists = uiState.playlists,
            onDismiss = { videoForPlaylist = null },
            onSelectPlaylist = { playlist ->
                libraryViewModel.addVideoToPlaylist(
                    playlistId = playlist.playlist.id,
                    videoId = target.id,
                    playlistName = playlist.playlist.name,
                )
                videoForPlaylist = null
            },
            onCreatePlaylist = { name ->
                playlistsViewModel.createPlaylist(name)
            },
        )
    }
}

@Composable
private fun CinemaHeroBanner(
    onPickVideo: () -> Unit,
    onBrowseFiles: () -> Unit,
    onOpenStream: () -> Unit,
    modifier: Modifier = Modifier,
) {
    Card(
        shape = MaterialTheme.shapes.large,
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceContainerHigh),
        modifier = modifier
            .fillMaxWidth()
            .widthIn(max = 820.dp),
    ) {
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .height(210.dp),
        ) {
            Image(
                painter = painterResource(id = R.drawable.img_cinema_hero),
                contentDescription = "ReelFlow cinema screening banner",
                contentScale = ContentScale.Crop,
                modifier = Modifier.fillMaxSize(),
            )
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .background(
                        Brush.horizontalGradient(
                            colors = listOf(
                                Color(0xE60B0D14),
                                Color(0xB30B0D14),
                                Color(0x4D0B0D14),
                            )
                        )
                    )
            )
            Column(
                verticalArrangement = Arrangement.SpaceBetween,
                modifier = Modifier
                    .fillMaxSize()
                    .padding(20.dp),
            ) {
                Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
                    Surface(
                        color = MaterialTheme.colorScheme.primary.copy(alpha = 0.2f),
                        shape = RoundedCornerShape(999.dp),
                    ) {
                        Text(
                            text = stringResource(R.string.hero_kicker),
                            style = MaterialTheme.typography.labelSmall,
                            color = MaterialTheme.colorScheme.secondary,
                            modifier = Modifier.padding(horizontal = 10.dp, vertical = 4.dp),
                        )
                    }
                    Text(
                        text = stringResource(R.string.hero_title),
                        style = MaterialTheme.typography.headlineMedium,
                        color = Color.White,
                        maxLines = 2,
                    )
                    Text(
                        text = stringResource(R.string.hero_subtitle),
                        style = MaterialTheme.typography.bodySmall,
                        color = Color.White.copy(alpha = 0.78f),
                        maxLines = 2,
                    )
                }

                Row(
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                    verticalAlignment = Alignment.CenterVertically,
                ) {
                    Button(
                        onClick = onPickVideo,
                        colors = ButtonDefaults.buttonColors(
                            containerColor = MaterialTheme.colorScheme.primary,
                            contentColor = MaterialTheme.colorScheme.onPrimary,
                        ),
                        contentPadding = PaddingValues(horizontal = 14.dp, vertical = 8.dp),
                        modifier = Modifier.testTag("hero_pick_video_button"),
                    ) {
                        Icon(Icons.Default.VideoLibrary, contentDescription = null, modifier = Modifier.size(18.dp))
                        Spacer(modifier = Modifier.width(6.dp))
                        Text(stringResource(R.string.action_pick_video))
                    }
                    FilledTonalButton(
                        onClick = onOpenStream,
                        contentPadding = PaddingValues(horizontal = 14.dp, vertical = 8.dp),
                        modifier = Modifier.testTag("hero_stream_url_button"),
                    ) {
                        Icon(Icons.Default.Link, contentDescription = null, modifier = Modifier.size(18.dp))
                        Spacer(modifier = Modifier.width(6.dp))
                        Text(stringResource(R.string.action_add_stream))
                    }
                    Surface(
                        color = Color.White.copy(alpha = 0.14f),
                        shape = CircleShape,
                        modifier = Modifier
                            .size(48.dp)
                            .clip(CircleShape)
                            .clickable(onClick = onBrowseFiles)
                            .testTag("hero_browse_files_button"),
                    ) {
                        Box(contentAlignment = Alignment.Center) {
                            Icon(
                                imageVector = Icons.Default.FolderOpen,
                                contentDescription = stringResource(R.string.action_browse_files),
                                tint = Color.White,
                                modifier = Modifier.size(20.dp),
                            )
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun ContinueWatchingCard(
    video: VideoItem,
    onClick: () -> Unit,
) {
    Card(
        shape = MaterialTheme.shapes.medium,
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceContainer),
        modifier = Modifier
            .width(216.dp)
            .clip(MaterialTheme.shapes.medium)
            .clickable(onClick = onClick)
            .testTag("continue_card_${video.id}"),
    ) {
        Column {
            VideoThumbnailArt(
                video = video,
                modifier = Modifier
                    .fillMaxWidth()
                    .aspectRatio(16f / 9f),
            )
            Column(
                modifier = Modifier.padding(horizontal = 10.dp, vertical = 8.dp),
            ) {
                Text(
                    text = video.title,
                    style = MaterialTheme.typography.titleSmall,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                )
                Text(
                    text = if (video.lastPositionMs > 0L) {
                        "Resume at ${formatDuration(video.lastPositionMs)}"
                    } else {
                        "Play again"
                    },
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.secondary,
                )
            }
        }
    }
}

@Composable
private fun LibraryEmptyState(
    hasSearchOrFilter: Boolean,
    onClearFilters: () -> Unit,
    onPickVideo: () -> Unit,
    onBrowseFiles: () -> Unit,
    onLoadSamples: () -> Unit,
    onOpenStream: () -> Unit,
    modifier: Modifier = Modifier,
) {
    Card(
        shape = MaterialTheme.shapes.large,
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceContainer),
        modifier = modifier.fillMaxWidth(),
    ) {
        Column(
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.spacedBy(12.dp),
            modifier = Modifier
                .fillMaxWidth()
                .padding(24.dp),
        ) {
            Surface(
                color = MaterialTheme.colorScheme.primaryContainer,
                shape = CircleShape,
                modifier = Modifier.size(64.dp),
            ) {
                Box(contentAlignment = Alignment.Center) {
                    Icon(
                        imageVector = Icons.Default.Movie,
                        contentDescription = null,
                        tint = MaterialTheme.colorScheme.onPrimaryContainer,
                        modifier = Modifier.size(32.dp),
                    )
                }
            }

            Text(
                text = if (hasSearchOrFilter) "No matching videos" else stringResource(R.string.empty_library_title),
                style = MaterialTheme.typography.titleLarge,
            )
            Text(
                text = if (hasSearchOrFilter) {
                    "Try clearing your search query or switching filter tabs."
                } else {
                    stringResource(R.string.empty_library_body)
                },
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )

            if (hasSearchOrFilter) {
                Button(onClick = onClearFilters) {
                    Text("Reset Filters")
                }
            } else {
                Row(
                    horizontalArrangement = Arrangement.spacedBy(10.dp),
                    modifier = Modifier.padding(top = 4.dp),
                ) {
                    Button(
                        onClick = onBrowseFiles,
                        modifier = Modifier.testTag("empty_browse_files_button"),
                    ) {
                        Icon(Icons.Default.FolderOpen, contentDescription = null)
                        Spacer(modifier = Modifier.width(6.dp))
                        Text(stringResource(R.string.action_browse_files))
                    }
                    OutlinedButton(onClick = onPickVideo) {
                        Text(stringResource(R.string.action_pick_video))
                    }
                }
                Row(
                    horizontalArrangement = Arrangement.spacedBy(10.dp),
                ) {
                    OutlinedButton(
                        onClick = onLoadSamples,
                        modifier = Modifier.testTag("load_samples_button"),
                    ) {
                        Icon(Icons.Default.PlayArrow, contentDescription = null)
                        Spacer(modifier = Modifier.width(6.dp))
                        Text(stringResource(R.string.action_load_samples))
                    }
                    OutlinedButton(onClick = onOpenStream) {
                        Icon(Icons.Default.Link, contentDescription = null)
                        Spacer(modifier = Modifier.width(6.dp))
                        Text(stringResource(R.string.action_add_stream))
                    }
                }
            }
        }
    }
}
