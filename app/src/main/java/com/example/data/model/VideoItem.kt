package com.example.data.model

import androidx.room.Entity
import androidx.room.Index
import androidx.room.PrimaryKey

enum class VideoSourceType {
    LOCAL_PICKER,
    LOCAL_FILE,
    STREAM_URL,
    DEMO_STREAM
}

@Entity(
    tableName = "videos",
    indices = [Index(value = ["uri"], unique = true)]
)
data class VideoItem(
    @PrimaryKey(autoGenerate = true) val id: Long = 0L,
    val uri: String,
    val title: String,
    val sourceType: VideoSourceType,
    val durationMs: Long = 0L,
    val lastPositionMs: Long = 0L,
    val lastPlayedAt: Long = 0L,
    val addedAt: Long = System.currentTimeMillis(),
    val width: Int = 0,
    val height: Int = 0,
    val mimeType: String = "video/*",
    val thumbnailPath: String? = null,
    val isFavorite: Boolean = false,
) {
    val resolutionLabel: String
        get() = when {
            height >= 2160 -> "4K"
            height >= 1440 -> "1440p"
            height >= 1080 -> "1080p"
            height >= 720 -> "720p"
            height > 0 -> "${height}p"
            sourceType == VideoSourceType.STREAM_URL || sourceType == VideoSourceType.DEMO_STREAM -> "STREAM"
            else -> "VIDEO"
        }

    val progressFraction: Float
        get() = if (durationMs > 0L && lastPositionMs > 0L) {
            (lastPositionMs.toFloat() / durationMs.toFloat()).coerceIn(0f, 1f)
        } else 0f
}
