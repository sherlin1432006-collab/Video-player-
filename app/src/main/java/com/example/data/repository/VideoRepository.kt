package com.example.data.repository

import android.content.Context
import android.content.Intent
import android.graphics.Bitmap
import android.media.MediaMetadataRetriever
import android.net.Uri
import android.provider.OpenableColumns
import com.example.data.local.PlaylistDao
import com.example.data.local.VideoDao
import com.example.data.model.PlaylistEntity
import com.example.data.model.PlaylistVideoCrossRef
import com.example.data.model.PlaylistWithVideos
import com.example.data.model.VideoItem
import com.example.data.model.VideoSourceType
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.withContext
import java.io.File
import java.io.FileOutputStream

class VideoRepository(
    private val context: Context,
    private val videoDao: VideoDao,
    private val playlistDao: PlaylistDao,
) {
    val allVideos: Flow<List<VideoItem>> = videoDao.observeAllVideos()
    val recentlyPlayed: Flow<List<VideoItem>> = videoDao.observeRecentlyPlayed()
    val playlists: Flow<List<PlaylistWithVideos>> = playlistDao.observePlaylistsWithVideos()

    suspend fun getVideoById(id: Long): VideoItem? = videoDao.getVideoById(id)

    suspend fun getPlaylistWithVideos(playlistId: Long): PlaylistWithVideos? =
        playlistDao.getPlaylistWithVideos(playlistId)

    suspend fun importLocalUris(uris: List<Uri>, sourceType: VideoSourceType): List<VideoItem> =
        withContext(Dispatchers.IO) {
            uris.mapNotNull { uri -> importSingleLocalUri(uri, sourceType) }
        }

    private suspend fun importSingleLocalUri(uri: Uri, sourceType: VideoSourceType): VideoItem? {
        runCatching {
            context.contentResolver.takePersistableUriPermission(
                uri,
                Intent.FLAG_GRANT_READ_URI_PERMISSION,
            )
        }

        val uriString = uri.toString()
        val existing = videoDao.getVideoByUri(uriString)
        if (existing != null) return existing

        val displayName = queryDisplayName(uri)
        val mimeType = context.contentResolver.getType(uri) ?: "video/*"

        var extractedTitle: String? = null
        var durationMs = 0L
        var width = 0
        var height = 0
        var thumbPath: String? = null

        val retriever = MediaMetadataRetriever()
        try {
            retriever.setDataSource(context, uri)
            extractedTitle = retriever.extractMetadata(MediaMetadataRetriever.METADATA_KEY_TITLE)
                ?.takeIf { it.isNotBlank() }
            durationMs = retriever.extractMetadata(MediaMetadataRetriever.METADATA_KEY_DURATION)
                ?.toLongOrNull() ?: 0L
            width = retriever.extractMetadata(MediaMetadataRetriever.METADATA_KEY_VIDEO_WIDTH)
                ?.toIntOrNull() ?: 0
            height = retriever.extractMetadata(MediaMetadataRetriever.METADATA_KEY_VIDEO_HEIGHT)
                ?.toIntOrNull() ?: 0

            val frameTimeUs = if (durationMs > 3000L) (durationMs * 150L) else 500_000L
            val bitmap = retriever.getFrameAtTime(frameTimeUs, MediaMetadataRetriever.OPTION_CLOSEST_SYNC)
                ?: retriever.getFrameAtTime(0L)
            if (bitmap != null) {
                thumbPath = saveThumbnailBitmap(uriString, bitmap)
                bitmap.recycle()
            }
        } catch (_: Exception) {
            // Some content providers restrict retriever metadata; ExoPlayer still plays the stream.
        } finally {
            runCatching { retriever.release() }
        }

        val finalTitle = extractedTitle
            ?: displayName?.substringBeforeLast('.')?.takeIf { it.isNotBlank() }
            ?: "Video Clip"

        val item = VideoItem(
            uri = uriString,
            title = finalTitle,
            sourceType = sourceType,
            durationMs = durationMs,
            width = width,
            height = height,
            mimeType = mimeType,
            thumbnailPath = thumbPath,
        )
        val insertedId = videoDao.insertVideo(item)
        return if (insertedId > 0L) {
            item.copy(id = insertedId)
        } else {
            videoDao.getVideoByUri(uriString)
        }
    }

    suspend fun addStreamUrl(url: String, customTitle: String?): Result<VideoItem> =
        withContext(Dispatchers.IO) {
            val trimmed = url.trim()
            val parsed = runCatching { Uri.parse(trimmed) }.getOrNull()
            val scheme = parsed?.scheme?.lowercase()
            if (trimmed.isEmpty() || scheme !in setOf("http", "https", "rtsp", "rtmp")) {
                return@withContext Result.failure(
                    IllegalArgumentException("Enter a valid http://, https://, or rtsp:// video URL.")
                )
            }

            val existing = videoDao.getVideoByUri(trimmed)
            if (existing != null) {
                return@withContext Result.success(existing)
            }

            val inferredTitle = customTitle?.trim()?.takeIf { it.isNotEmpty() }
                ?: parsed?.lastPathSegment
                    ?.substringBeforeLast('.')
                    ?.replace('-', ' ')
                    ?.replace('_', ' ')
                    ?.replaceFirstChar { it.uppercase() }
                    ?.takeIf { it.isNotBlank() }
                ?: "Network Stream"

            val mime = when {
                trimmed.contains(".m3u8", ignoreCase = true) -> "application/x-mpegURL"
                trimmed.contains(".mpd", ignoreCase = true) -> "application/dash+xml"
                else -> "video/mp4"
            }

            val item = VideoItem(
                uri = trimmed,
                title = inferredTitle,
                sourceType = VideoSourceType.STREAM_URL,
                mimeType = mime,
            )
            val id = videoDao.insertVideo(item)
            val saved = if (id > 0L) item.copy(id = id) else videoDao.getVideoByUri(trimmed)!!
            Result.success(saved)
        }

    suspend fun loadSampleCinemaReels(): Int = withContext(Dispatchers.IO) {
        val samples = listOf(
            VideoItem(
                uri = "https://commondatastorage.googleapis.com/gtv-videos-bucket/sample/BigBuckBunny.mp4",
                title = "Big Buck Bunny",
                sourceType = VideoSourceType.DEMO_STREAM,
                durationMs = 596_000L,
                width = 1280,
                height = 720,
                mimeType = "video/mp4",
            ),
            VideoItem(
                uri = "https://commondatastorage.googleapis.com/gtv-videos-bucket/sample/Sintel.mp4",
                title = "Sintel (Open Movie)",
                sourceType = VideoSourceType.DEMO_STREAM,
                durationMs = 888_000L,
                width = 1280,
                height = 720,
                mimeType = "video/mp4",
            ),
            VideoItem(
                uri = "https://commondatastorage.googleapis.com/gtv-videos-bucket/sample/TearsOfSteel.mp4",
                title = "Tears of Steel",
                sourceType = VideoSourceType.DEMO_STREAM,
                durationMs = 734_000L,
                width = 1280,
                height = 720,
                mimeType = "video/mp4",
            ),
            VideoItem(
                uri = "https://commondatastorage.googleapis.com/gtv-videos-bucket/sample/ForBiggerBlazes.mp4",
                title = "For Bigger Blazes (Quick Clip)",
                sourceType = VideoSourceType.DEMO_STREAM,
                durationMs = 15_000L,
                width = 1280,
                height = 720,
                mimeType = "video/mp4",
            ),
        )
        var addedCount = 0
        for (sample in samples) {
            if (videoDao.getVideoByUri(sample.uri) == null) {
                val id = videoDao.insertVideo(sample)
                if (id > 0L) addedCount++
            }
        }
        addedCount
    }

    suspend fun updatePlaybackStats(
        videoId: Long,
        positionMs: Long,
        durationMs: Long,
        width: Int,
        height: Int,
    ) = withContext(Dispatchers.IO) {
        videoDao.updatePlaybackStats(
            videoId = videoId,
            positionMs = positionMs.coerceAtLeast(0L),
            playedAt = System.currentTimeMillis(),
            durationMs = durationMs,
            width = width,
            height = height,
        )
    }

    suspend fun setFavorite(videoId: Long, isFavorite: Boolean) =
        videoDao.setFavorite(videoId, isFavorite)

    suspend fun renameVideo(videoId: Long, newTitle: String) {
        val clean = newTitle.trim()
        if (clean.isNotEmpty()) {
            videoDao.renameVideo(videoId, clean)
        }
    }

    suspend fun deleteVideo(video: VideoItem) = withContext(Dispatchers.IO) {
        video.thumbnailPath?.let { path ->
            runCatching { File(path).delete() }
        }
        videoDao.deleteVideoById(video.id)
    }

    suspend fun clearWatchHistory() = videoDao.clearWatchHistory()

    suspend fun createPlaylist(name: String): Long {
        val clean = name.trim().ifEmpty { "Untitled Playlist" }
        return playlistDao.insertPlaylist(PlaylistEntity(name = clean))
    }

    suspend fun renamePlaylist(playlistId: Long, name: String) {
        val clean = name.trim()
        if (clean.isNotEmpty()) playlistDao.renamePlaylist(playlistId, clean)
    }

    suspend fun deletePlaylist(playlistId: Long) = playlistDao.deletePlaylist(playlistId)

    suspend fun addVideoToPlaylist(playlistId: Long, videoId: Long) {
        playlistDao.addVideoToPlaylist(PlaylistVideoCrossRef(playlistId = playlistId, videoId = videoId))
    }

    suspend fun removeVideoFromPlaylist(playlistId: Long, videoId: Long) {
        playlistDao.removeVideoFromPlaylist(playlistId, videoId)
    }

    private fun queryDisplayName(uri: Uri): String? {
        if (uri.scheme == "file") return uri.lastPathSegment
        return runCatching {
            context.contentResolver.query(
                uri,
                arrayOf(OpenableColumns.DISPLAY_NAME),
                null,
                null,
                null,
            )?.use { cursor ->
                if (cursor.moveToFirst()) {
                    val idx = cursor.getColumnIndex(OpenableColumns.DISPLAY_NAME)
                    if (idx >= 0) cursor.getString(idx) else null
                } else null
            }
        }.getOrNull()
    }

    private fun saveThumbnailBitmap(key: String, bitmap: Bitmap): String? {
        return runCatching {
            val dir = File(context.filesDir, "thumbnails").apply { mkdirs() }
            val file = File(dir, "thumb_${key.hashCode().toUInt()}.jpg")
            val maxDim = 480
            val scale = minOf(maxDim.toFloat() / bitmap.width, maxDim.toFloat() / bitmap.height, 1f)
            val scaled = if (scale < 1f) {
                Bitmap.createScaledBitmap(
                    bitmap,
                    (bitmap.width * scale).toInt().coerceAtLeast(1),
                    (bitmap.height * scale).toInt().coerceAtLeast(1),
                    true,
                )
            } else bitmap

            FileOutputStream(file).use { out ->
                scaled.compress(Bitmap.CompressFormat.JPEG, 82, out)
            }
            if (scaled !== bitmap) scaled.recycle()
            file.absolutePath
        }.getOrNull()
    }
}
