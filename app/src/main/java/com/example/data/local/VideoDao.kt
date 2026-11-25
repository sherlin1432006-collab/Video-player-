package com.example.data.local

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import androidx.room.Update
import com.example.data.model.VideoItem
import kotlinx.coroutines.flow.Flow

@Dao
interface VideoDao {
    @Query("SELECT * FROM videos ORDER BY addedAt DESC")
    fun observeAllVideos(): Flow<List<VideoItem>>

    @Query("SELECT * FROM videos WHERE lastPlayedAt > 0 ORDER BY lastPlayedAt DESC LIMIT 10")
    fun observeRecentlyPlayed(): Flow<List<VideoItem>>

    @Query("SELECT * FROM videos WHERE id = :id LIMIT 1")
    suspend fun getVideoById(id: Long): VideoItem?

    @Query("SELECT * FROM videos WHERE uri = :uri LIMIT 1")
    suspend fun getVideoByUri(uri: String): VideoItem?

    @Insert(onConflict = OnConflictStrategy.IGNORE)
    suspend fun insertVideo(video: VideoItem): Long

    @Update
    suspend fun updateVideo(video: VideoItem)

    @Query(
        """
        UPDATE videos SET 
            lastPositionMs = :positionMs,
            lastPlayedAt = :playedAt,
            durationMs = CASE WHEN :durationMs > 0 THEN :durationMs ELSE durationMs END,
            width = CASE WHEN :width > 0 THEN :width ELSE width END,
            height = CASE WHEN :height > 0 THEN :height ELSE height END
        WHERE id = :videoId
        """
    )
    suspend fun updatePlaybackStats(
        videoId: Long,
        positionMs: Long,
        playedAt: Long,
        durationMs: Long,
        width: Int,
        height: Int,
    )

    @Query("UPDATE videos SET isFavorite = :isFavorite WHERE id = :videoId")
    suspend fun setFavorite(videoId: Long, isFavorite: Boolean)

    @Query("UPDATE videos SET title = :newTitle WHERE id = :videoId")
    suspend fun renameVideo(videoId: Long, newTitle: String)

    @Query("DELETE FROM videos WHERE id = :videoId")
    suspend fun deleteVideoById(videoId: Long)

    @Query("UPDATE videos SET lastPositionMs = 0, lastPlayedAt = 0")
    suspend fun clearWatchHistory()
}
