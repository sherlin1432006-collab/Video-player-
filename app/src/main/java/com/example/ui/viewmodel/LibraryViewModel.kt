package com.example.ui.viewmodel

import android.net.Uri
import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import com.example.data.model.PlaylistWithVideos
import com.example.data.model.VideoItem
import com.example.data.model.VideoSourceType
import com.example.data.repository.VideoRepository
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

enum class LibraryFilter(val label: String) {
    ALL("All"),
    FAVORITES("Favorites"),
    LOCAL("Device"),
    STREAMS("Streams"),
}

data class LibraryUiState(
    val videos: List<VideoItem> = emptyList(),
    val recentlyPlayed: List<VideoItem> = emptyList(),
    val playlists: List<PlaylistWithVideos> = emptyList(),
    val searchQuery: String = "",
    val activeFilter: LibraryFilter = LibraryFilter.ALL,
    val isGridMode: Boolean = true,
    val isImporting: Boolean = false,
    val message: String? = null,
)

class LibraryViewModel(
    private val repository: VideoRepository,
) : ViewModel() {

    private val searchQuery = MutableStateFlow("")
    private val activeFilter = MutableStateFlow(LibraryFilter.ALL)
    private val isGridMode = MutableStateFlow(true)
    private val isImporting = MutableStateFlow(false)
    private val statusMessage = MutableStateFlow<String?>(null)

    private val filterControls = combine(
        searchQuery,
        activeFilter,
        isGridMode,
        isImporting,
        statusMessage,
    ) { query, filter, grid, importing, msg ->
        FilterControls(query, filter, grid, importing, msg)
    }

    val uiState: StateFlow<LibraryUiState> = combine(
        repository.allVideos,
        repository.recentlyPlayed,
        repository.playlists,
        filterControls,
    ) { all, recent, playlists, controls ->
        val filtered = all.filter { video ->
            val matchesFilter = when (controls.filter) {
                LibraryFilter.ALL -> true
                LibraryFilter.FAVORITES -> video.isFavorite
                LibraryFilter.LOCAL -> video.sourceType == VideoSourceType.LOCAL_PICKER ||
                    video.sourceType == VideoSourceType.LOCAL_FILE
                LibraryFilter.STREAMS -> video.sourceType == VideoSourceType.STREAM_URL ||
                    video.sourceType == VideoSourceType.DEMO_STREAM
            }
            val matchesSearch = controls.query.isBlank() ||
                video.title.contains(controls.query, ignoreCase = true) ||
                video.uri.contains(controls.query, ignoreCase = true) ||
                video.resolutionLabel.contains(controls.query, ignoreCase = true)
            matchesFilter && matchesSearch
        }

        LibraryUiState(
            videos = filtered,
            recentlyPlayed = recent,
            playlists = playlists,
            searchQuery = controls.query,
            activeFilter = controls.filter,
            isGridMode = controls.grid,
            isImporting = controls.importing,
            message = controls.message,
        )
    }.stateIn(
        scope = viewModelScope,
        started = SharingStarted.WhileSubscribed(5_000),
        initialValue = LibraryUiState(),
    )

    fun setSearchQuery(query: String) {
        searchQuery.value = query
    }

    fun setFilter(filter: LibraryFilter) {
        activeFilter.value = filter
    }

    fun toggleGridMode() {
        isGridMode.update { !it }
    }

    fun clearMessage() {
        statusMessage.value = null
    }

    fun importLocalVideos(uris: List<Uri>, sourceType: VideoSourceType, onImportedFirst: ((Long) -> Unit)? = null) {
        if (uris.isEmpty()) return
        viewModelScope.launch {
            isImporting.value = true
            val imported = repository.importLocalUris(uris, sourceType)
            isImporting.value = false
            statusMessage.value = if (imported.size == 1) {
                "Added \"${imported.first().title}\""
            } else if (imported.isNotEmpty()) {
                "Added ${imported.size} videos to Library"
            } else {
                "Could not read selected video"
            }
            imported.firstOrNull()?.let { first ->
                onImportedFirst?.invoke(first.id)
            }
        }
    }

    fun addStreamUrl(url: String, title: String, onSuccess: (Long) -> Unit) {
        viewModelScope.launch {
            val result = repository.addStreamUrl(url, title)
            result.onSuccess { item ->
                statusMessage.value = "Added stream \"${item.title}\""
                onSuccess(item.id)
            }.onFailure { err ->
                statusMessage.value = err.message ?: "Invalid stream URL"
            }
        }
    }

    fun loadSampleReels() {
        viewModelScope.launch {
            isImporting.value = true
            val count = repository.loadSampleCinemaReels()
            isImporting.value = false
            statusMessage.value = if (count > 0) {
                "Loaded $count Creative Commons sample reels"
            } else {
                "Sample reels are already in your library"
            }
        }
    }

    fun toggleFavorite(video: VideoItem) {
        viewModelScope.launch {
            repository.setFavorite(video.id, !video.isFavorite)
        }
    }

    fun renameVideo(videoId: Long, newTitle: String) {
        viewModelScope.launch {
            repository.renameVideo(videoId, newTitle)
        }
    }

    fun deleteVideo(video: VideoItem) {
        viewModelScope.launch {
            repository.deleteVideo(video)
            statusMessage.value = "Removed \"${video.title}\""
        }
    }

    fun addVideoToPlaylist(playlistId: Long, videoId: Long, playlistName: String) {
        viewModelScope.launch {
            repository.addVideoToPlaylist(playlistId, videoId)
            statusMessage.value = "Added to $playlistName"
        }
    }

    private data class FilterControls(
        val query: String,
        val filter: LibraryFilter,
        val grid: Boolean,
        val importing: Boolean,
        val message: String?,
    )

    class Factory(private val repository: VideoRepository) : ViewModelProvider.Factory {
        @Suppress("UNCHECKED_CAST")
        override fun <T : ViewModel> create(modelClass: Class<T>): T {
            return LibraryViewModel(repository) as T
        }
    }
}
