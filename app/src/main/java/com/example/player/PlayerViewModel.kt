package com.example.player

import android.app.Application
import android.net.Uri
import androidx.annotation.OptIn
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import androidx.media3.common.AudioAttributes
import androidx.media3.common.C
import androidx.media3.common.MediaItem
import androidx.media3.common.MediaMetadata
import androidx.media3.common.PlaybackException
import androidx.media3.common.PlaybackParameters
import androidx.media3.common.Player
import androidx.media3.common.TrackSelectionOverride
import androidx.media3.common.Tracks
import androidx.media3.common.VideoSize
import androidx.media3.common.util.UnstableApi
import androidx.media3.exoplayer.ExoPlayer
import com.example.data.model.VideoItem
import com.example.data.model.VideoSourceType
import com.example.data.prefs.AspectResizeOption
import com.example.data.prefs.PlayerPreferencesRepository
import com.example.data.repository.VideoRepository
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.isActive
import kotlinx.coroutines.launch

@OptIn(UnstableApi::class)
class PlayerViewModel(
    application: Application,
    private val videoRepository: VideoRepository,
    private val preferencesRepository: PlayerPreferencesRepository,
) : AndroidViewModel(application) {

    private val _uiState = MutableStateFlow(PlayerUiState())
    val uiState: StateFlow<PlayerUiState> = _uiState.asStateFlow()

    val player: ExoPlayer = ExoPlayer.Builder(application).build().apply {
        setAudioAttributes(
            AudioAttributes.Builder()
                .setUsage(C.USAGE_MEDIA)
                .setContentType(C.AUDIO_CONTENT_TYPE_MOVIE)
                .build(),
            true,
        )
        setHandleAudioBecomingNoisy(true)
    }

    private var tickerJob: Job? = null
    private var sleepTimerJob: Job? = null
    private var lastSavedAtMs: Long = 0L
    private var loadedSignature: String? = null

    private val playerListener = object : Player.Listener {
        override fun onIsPlayingChanged(isPlaying: Boolean) {
            _uiState.update { it.copy(isPlaying = isPlaying) }
            if (!isPlaying) {
                persistCurrentProgress()
            }
        }

        override fun onPlaybackStateChanged(playbackState: Int) {
            val duration = player.duration.takeIf { it > 0L } ?: _uiState.value.durationMs
            _uiState.update {
                it.copy(
                    isBuffering = playbackState == Player.STATE_BUFFERING,
                    isEnded = playbackState == Player.STATE_ENDED,
                    durationMs = duration,
                )
            }
            if (playbackState == Player.STATE_READY || playbackState == Player.STATE_ENDED) {
                persistCurrentProgress()
            }
        }

        override fun onMediaItemTransition(mediaItem: MediaItem?, reason: Int) {
            val newIndex = player.currentMediaItemIndex
            val queue = _uiState.value.queue
            val nextVideo = queue.getOrNull(newIndex)
            _uiState.update {
                it.copy(
                    currentIndex = newIndex,
                    currentVideo = nextVideo,
                    positionMs = player.currentPosition.coerceAtLeast(0L),
                    durationMs = player.duration.takeIf { d -> d > 0L } ?: nextVideo?.durationMs ?: 0L,
                    abLoopStartMs = null,
                    abLoopEndMs = null,
                    errorMessage = null,
                )
            }
        }

        override fun onVideoSizeChanged(videoSize: VideoSize) {
            if (videoSize.width > 0 && videoSize.height > 0) {
                _uiState.update {
                    it.copy(videoWidth = videoSize.width, videoHeight = videoSize.height)
                }
            }
        }

        override fun onTracksChanged(tracks: Tracks) {
            updateTrackOptions(tracks)
        }

        override fun onPlayerError(error: PlaybackException) {
            _uiState.update {
                it.copy(
                    isBuffering = false,
                    errorMessage = error.localizedMessage ?: "Unable to play this video source.",
                )
            }
        }
    }

    init {
        player.addListener(playerListener)
        startProgressTicker()
    }

    fun startPlayback(videoId: Long, playlistId: Long?) {
        val signature = "${videoId}_${playlistId ?: -1L}"
        if (loadedSignature == signature && _uiState.value.queue.isNotEmpty()) {
            return
        }
        loadedSignature = signature

        viewModelScope.launch {
            val prefs = preferencesRepository.preferences.first()
            val queue: List<VideoItem>
            val startIndex: Int

            if (playlistId != null) {
                val playlistWithVideos = videoRepository.getPlaylistWithVideos(playlistId)
                val items = playlistWithVideos?.videos.orEmpty()
                if (items.isNotEmpty()) {
                    queue = items
                    startIndex = items.indexOfFirst { it.id == videoId }.coerceAtLeast(0)
                } else {
                    val single = videoRepository.getVideoById(videoId) ?: return@launch
                    queue = listOf(single)
                    startIndex = 0
                }
            } else {
                val all = videoRepository.allVideos.first()
                val targetIndex = all.indexOfFirst { it.id == videoId }
                if (targetIndex >= 0 && prefs.autoPlayNext) {
                    queue = all
                    startIndex = targetIndex
                } else {
                    val single = videoRepository.getVideoById(videoId) ?: return@launch
                    queue = listOf(single)
                    startIndex = 0
                }
            }

            val startVideo = queue[startIndex]
            val startPosition = if (
                prefs.resumePlayback &&
                startVideo.lastPositionMs > 2_000L &&
                (startVideo.durationMs <= 0L || startVideo.lastPositionMs < startVideo.durationMs - 4_000L)
            ) {
                startVideo.lastPositionMs
            } else 0L

            val mediaItems = queue.map { item ->
                MediaItem.Builder()
                    .setMediaId(item.id.toString())
                    .setUri(Uri.parse(item.uri))
                    .setMediaMetadata(
                        MediaMetadata.Builder()
                            .setTitle(item.title)
                            .build()
                    )
                    .build()
            }

            _uiState.update {
                it.copy(
                    queue = queue,
                    currentIndex = startIndex,
                    currentVideo = startVideo,
                    playbackSpeed = prefs.defaultSpeed,
                    aspectMode = prefs.defaultAspectMode,
                    positionMs = startPosition,
                    durationMs = startVideo.durationMs,
                    errorMessage = null,
                    abLoopStartMs = null,
                    abLoopEndMs = null,
                )
            }

            player.setMediaItems(mediaItems, startIndex, startPosition)
            player.playbackParameters = PlaybackParameters(prefs.defaultSpeed)
            player.prepare()
            player.playWhenReady = true
        }
    }

    fun playExternalUri(uri: Uri) {
        viewModelScope.launch {
            val imported = videoRepository.importLocalUris(listOf(uri), VideoSourceType.LOCAL_FILE)
            val video = imported.firstOrNull() ?: return@launch
            loadedSignature = null
            startPlayback(video.id, null)
        }
    }

    fun togglePlayPause() {
        if (player.playbackState == Player.STATE_ENDED) {
            player.seekTo(0L)
            player.playWhenReady = true
        } else if (player.isPlaying) {
            player.pause()
        } else {
            player.play()
        }
    }

    fun seekTo(positionMs: Long) {
        val duration = player.duration.takeIf { it > 0L } ?: _uiState.value.durationMs
        val clamped = if (duration > 0L) positionMs.coerceIn(0L, duration) else positionMs.coerceAtLeast(0L)
        player.seekTo(clamped)
        _uiState.update { it.copy(positionMs = clamped) }
    }

    fun seekBy(deltaMs: Long) {
        seekTo(player.currentPosition + deltaMs)
    }

    fun playQueueItem(index: Int) {
        if (index in _uiState.value.queue.indices) {
            persistCurrentProgress()
            player.seekToDefaultPosition(index)
            player.playWhenReady = true
        }
    }

    fun skipNext() {
        if (player.hasNextMediaItem()) {
            persistCurrentProgress()
            player.seekToNextMediaItem()
        }
    }

    fun skipPrevious() {
        if (player.currentPosition > 3_000L || !player.hasPreviousMediaItem()) {
            seekTo(0L)
        } else {
            persistCurrentProgress()
            player.seekToPreviousMediaItem()
        }
    }

    fun setPlaybackSpeed(speed: Float) {
        val clamped = speed.coerceIn(0.25f, 2.5f)
        player.playbackParameters = PlaybackParameters(clamped)
        _uiState.update { it.copy(playbackSpeed = clamped) }
    }

    fun cycleRepeatMode() {
        val next = when (_uiState.value.repeatMode) {
            RepeatModeOption.OFF -> RepeatModeOption.ONE
            RepeatModeOption.ONE -> RepeatModeOption.ALL
            RepeatModeOption.ALL -> RepeatModeOption.OFF
        }
        player.repeatMode = when (next) {
            RepeatModeOption.OFF -> Player.REPEAT_MODE_OFF
            RepeatModeOption.ONE -> Player.REPEAT_MODE_ONE
            RepeatModeOption.ALL -> Player.REPEAT_MODE_ALL
        }
        _uiState.update { it.copy(repeatMode = next) }
    }

    fun toggleAspectMode() {
        val next = when (_uiState.value.aspectMode) {
            AspectResizeOption.FIT -> AspectResizeOption.FILL
            AspectResizeOption.FILL -> AspectResizeOption.FIT
        }
        _uiState.update { it.copy(aspectMode = next) }
    }

    fun setAbLoopPoint() {
        val current = player.currentPosition.coerceAtLeast(0L)
        val state = _uiState.value
        when {
            state.abLoopStartMs == null -> {
                _uiState.update { it.copy(abLoopStartMs = current, abLoopEndMs = null) }
            }
            state.abLoopEndMs == null -> {
                val start = state.abLoopStartMs
                if (current > start + 1_000L) {
                    _uiState.update { it.copy(abLoopEndMs = current) }
                } else {
                    _uiState.update { it.copy(abLoopStartMs = null, abLoopEndMs = null) }
                }
            }
            else -> {
                _uiState.update { it.copy(abLoopStartMs = null, abLoopEndMs = null) }
            }
        }
    }

    fun clearAbLoop() {
        _uiState.update { it.copy(abLoopStartMs = null, abLoopEndMs = null) }
    }

    fun selectAudioTrack(option: MediaTrackOption) {
        val groups = player.currentTracks.groups
        val group = groups.getOrNull(option.groupIndex) ?: return
        val params = player.trackSelectionParameters.buildUpon()
            .setOverrideForType(TrackSelectionOverride(group.mediaTrackGroup, option.trackIndex))
            .build()
        player.trackSelectionParameters = params
    }

    fun selectSubtitleTrack(option: MediaTrackOption?) {
        val builder = player.trackSelectionParameters.buildUpon()
        if (option == null) {
            builder.setTrackTypeDisabled(C.TRACK_TYPE_TEXT, true)
        } else {
            val group = player.currentTracks.groups.getOrNull(option.groupIndex) ?: return
            builder
                .setTrackTypeDisabled(C.TRACK_TYPE_TEXT, false)
                .setOverrideForType(TrackSelectionOverride(group.mediaTrackGroup, option.trackIndex))
        }
        player.trackSelectionParameters = builder.build()
    }

    fun setSleepTimerMinutes(minutes: Int?) {
        sleepTimerJob?.cancel()
        if (minutes == null || minutes <= 0) {
            _uiState.update { it.copy(sleepTimerRemainingSec = null) }
            return
        }
        sleepTimerJob = viewModelScope.launch {
            var remaining = minutes * 60
            while (remaining > 0 && isActive) {
                _uiState.update { it.copy(sleepTimerRemainingSec = remaining) }
                delay(1_000L)
                remaining--
            }
            if (isActive) {
                player.pause()
                _uiState.update { it.copy(sleepTimerRemainingSec = null) }
            }
        }
    }

    fun retryPlayback() {
        _uiState.update { it.copy(errorMessage = null) }
        player.prepare()
        player.playWhenReady = true
    }

    fun toggleCurrentFavorite() {
        val video = _uiState.value.currentVideo ?: return
        val nextFav = !video.isFavorite
        viewModelScope.launch {
            videoRepository.setFavorite(video.id, nextFav)
            _uiState.update { state ->
                val updatedVideo = video.copy(isFavorite = nextFav)
                state.copy(
                    currentVideo = updatedVideo,
                    queue = state.queue.map { if (it.id == video.id) updatedVideo else it },
                )
            }
        }
    }

    fun pauseAndSave() {
        player.pause()
        persistCurrentProgress()
    }

    private fun startProgressTicker() {
        tickerJob?.cancel()
        tickerJob = viewModelScope.launch {
            while (isActive) {
                val pos = player.currentPosition.coerceAtLeast(0L)
                val buffered = player.bufferedPosition.coerceAtLeast(0L)
                val duration = player.duration.takeIf { it > 0L } ?: _uiState.value.durationMs

                val state = _uiState.value
                val loopStart = state.abLoopStartMs
                val loopEnd = state.abLoopEndMs
                if (loopStart != null && loopEnd != null && pos >= loopEnd && player.isPlaying) {
                    player.seekTo(loopStart)
                }

                _uiState.update {
                    it.copy(
                        positionMs = pos,
                        bufferedPositionMs = buffered,
                        durationMs = duration,
                    )
                }

                val now = System.currentTimeMillis()
                if (player.isPlaying && now - lastSavedAtMs > 5_000L) {
                    lastSavedAtMs = now
                    persistCurrentProgress()
                }
                delay(250L)
            }
        }
    }

    private fun updateTrackOptions(tracks: Tracks) {
        val audioList = mutableListOf<MediaTrackOption>()
        val textList = mutableListOf<MediaTrackOption>()

        tracks.groups.forEachIndexed { groupIdx, group ->
            if (!group.isSupported) return@forEachIndexed
            when (group.type) {
                C.TRACK_TYPE_AUDIO -> {
                    for (i in 0 until group.length) {
                        val format = group.getTrackFormat(i)
                        val lang = format.language?.uppercase()
                        val label = format.label
                            ?: lang
                            ?: "Audio Track ${audioList.size + 1}"
                        audioList += MediaTrackOption(
                            groupIndex = groupIdx,
                            trackIndex = i,
                            label = label,
                            language = lang,
                            isSelected = group.isTrackSelected(i),
                        )
                    }
                }
                C.TRACK_TYPE_TEXT -> {
                    for (i in 0 until group.length) {
                        val format = group.getTrackFormat(i)
                        val lang = format.language?.uppercase()
                        val label = format.label
                            ?: lang
                            ?: "Subtitle ${textList.size + 1}"
                        textList += MediaTrackOption(
                            groupIndex = groupIdx,
                            trackIndex = i,
                            label = label,
                            language = lang,
                            isSelected = group.isTrackSelected(i),
                        )
                    }
                }
            }
        }

        val subtitlesDisabled = textList.none { it.isSelected }
        _uiState.update {
            it.copy(
                audioTracks = audioList,
                subtitleTracks = textList,
                subtitlesDisabled = subtitlesDisabled,
            )
        }
    }

    private fun persistCurrentProgress() {
        val video = _uiState.value.currentVideo ?: return
        val pos = player.currentPosition.coerceAtLeast(0L)
        val duration = player.duration.takeIf { it > 0L } ?: video.durationMs
        val width = _uiState.value.videoWidth
        val height = _uiState.value.videoHeight

        viewModelScope.launch {
            videoRepository.updatePlaybackStats(
                videoId = video.id,
                positionMs = pos,
                durationMs = duration,
                width = width,
                height = height,
            )
        }
    }

    override fun onCleared() {
        tickerJob?.cancel()
        sleepTimerJob?.cancel()
        persistCurrentProgress()
        player.removeListener(playerListener)
        player.release()
        super.onCleared()
    }

    class Factory(
        private val application: Application,
        private val videoRepository: VideoRepository,
        private val preferencesRepository: PlayerPreferencesRepository,
    ) : ViewModelProvider.Factory {
        @Suppress("UNCHECKED_CAST")
        override fun <T : androidx.lifecycle.ViewModel> create(modelClass: Class<T>): T {
            return PlayerViewModel(application, videoRepository, preferencesRepository) as T
        }
    }
}
