package com.example.ui.viewmodel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import com.example.data.model.PlaylistWithVideos
import com.example.data.model.VideoItem
import com.example.data.repository.VideoRepository
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch

data class PlaylistsUiState(
    val playlists: List<PlaylistWithVideos> = emptyList(),
    val allVideos: List<VideoItem> = emptyList(),
)

class PlaylistsViewModel(
    private val repository: VideoRepository,
) : ViewModel() {

    val uiState: StateFlow<PlaylistsUiState> = combine(
        repository.playlists,
        repository.allVideos,
    ) { playlists, videos ->
        PlaylistsUiState(playlists = playlists, allVideos = videos)
    }.stateIn(
        scope = viewModelScope,
        started = SharingStarted.WhileSubscribed(5_000),
        initialValue = PlaylistsUiState(),
    )

    fun createPlaylist(name: String) {
        viewModelScope.launch {
            repository.createPlaylist(name)
        }
    }

    fun renamePlaylist(playlistId: Long, name: String) {
        viewModelScope.launch {
            repository.renamePlaylist(playlistId, name)
        }
    }

    fun deletePlaylist(playlistId: Long) {
        viewModelScope.launch {
            repository.deletePlaylist(playlistId)
        }
    }

    fun addVideoToPlaylist(playlistId: Long, videoId: Long) {
        viewModelScope.launch {
            repository.addVideoToPlaylist(playlistId, videoId)
        }
    }

    fun removeVideoFromPlaylist(playlistId: Long, videoId: Long) {
        viewModelScope.launch {
            repository.removeVideoFromPlaylist(playlistId, videoId)
        }
    }

    class Factory(private val repository: VideoRepository) : ViewModelProvider.Factory {
        @Suppress("UNCHECKED_CAST")
        override fun <T : ViewModel> create(modelClass: Class<T>): T {
            return PlaylistsViewModel(repository) as T
        }
    }
}
