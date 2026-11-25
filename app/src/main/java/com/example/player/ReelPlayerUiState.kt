package com.example.player

import com.example.data.model.VideoItem
import com.example.data.prefs.AspectResizeOption

data class MediaTrackOption(
    val groupIndex: Int,
    val trackIndex: Int,
    val label: String,
    val language: String?,
    val isSelected: Boolean,
)

enum class RepeatModeOption(val label: String) {
    OFF("Off"),
    ONE("Loop One"),
    ALL("Loop Queue"),
}

data class PlayerUiState(
    val queue: List<VideoItem> = emptyList(),
    val currentIndex: Int = 0,
    val currentVideo: VideoItem? = null,
    val isPlaying: Boolean = false,
    val isBuffering: Boolean = false,
    val isEnded: Boolean = false,
    val positionMs: Long = 0L,
    val bufferedPositionMs: Long = 0L,
    val durationMs: Long = 0L,
    val playbackSpeed: Float = 1.0f,
    val repeatMode: RepeatModeOption = RepeatModeOption.OFF,
    val aspectMode: AspectResizeOption = AspectResizeOption.FIT,
    val videoWidth: Int = 0,
    val videoHeight: Int = 0,
    val audioTracks: List<MediaTrackOption> = emptyList(),
    val subtitleTracks: List<MediaTrackOption> = emptyList(),
    val subtitlesDisabled: Boolean = true,
    val abLoopStartMs: Long? = null,
    val abLoopEndMs: Long? = null,
    val sleepTimerRemainingSec: Int? = null,
    val errorMessage: String? = null,
) {
    val hasNext: Boolean get() = currentIndex < queue.lastIndex
    val hasPrevious: Boolean get() = currentIndex > 0 || positionMs > 3000L
}
