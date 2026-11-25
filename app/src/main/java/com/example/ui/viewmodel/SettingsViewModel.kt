package com.example.ui.viewmodel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import com.example.data.prefs.AspectResizeOption
import com.example.data.prefs.PlayerPreferences
import com.example.data.prefs.PlayerPreferencesRepository
import com.example.data.repository.VideoRepository
import com.example.ui.theme.ThemeMode
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch

data class SettingsUiState(
    val preferences: PlayerPreferences = PlayerPreferences(),
    val statusMessage: String? = null,
)

class SettingsViewModel(
    private val preferencesRepository: PlayerPreferencesRepository,
    private val videoRepository: VideoRepository,
) : ViewModel() {

    private val message = MutableStateFlow<String?>(null)

    val uiState: StateFlow<SettingsUiState> = combine(
        preferencesRepository.preferences,
        message,
    ) { prefs, msg ->
        SettingsUiState(preferences = prefs, statusMessage = msg)
    }.stateIn(
        scope = viewModelScope,
        started = SharingStarted.WhileSubscribed(5_000),
        initialValue = SettingsUiState(),
    )

    fun setThemeMode(mode: ThemeMode) {
        viewModelScope.launch { preferencesRepository.setThemeMode(mode) }
    }

    fun setDefaultSpeed(speed: Float) {
        viewModelScope.launch { preferencesRepository.setDefaultSpeed(speed) }
    }

    fun setDoubleTapSeekSeconds(seconds: Int) {
        viewModelScope.launch { preferencesRepository.setDoubleTapSeekSeconds(seconds) }
    }

    fun setAutoPlayNext(enabled: Boolean) {
        viewModelScope.launch { preferencesRepository.setAutoPlayNext(enabled) }
    }

    fun setResumePlayback(enabled: Boolean) {
        viewModelScope.launch { preferencesRepository.setResumePlayback(enabled) }
    }

    fun setDefaultAspectMode(mode: AspectResizeOption) {
        viewModelScope.launch { preferencesRepository.setDefaultAspectMode(mode) }
    }

    fun clearWatchHistory() {
        viewModelScope.launch {
            videoRepository.clearWatchHistory()
            message.value = "Watch history and resume positions cleared"
        }
    }

    fun clearMessage() {
        message.value = null
    }

    class Factory(
        private val preferencesRepository: PlayerPreferencesRepository,
        private val videoRepository: VideoRepository,
    ) : ViewModelProvider.Factory {
        @Suppress("UNCHECKED_CAST")
        override fun <T : ViewModel> create(modelClass: Class<T>): T {
            return SettingsViewModel(preferencesRepository, videoRepository) as T
        }
    }
}
