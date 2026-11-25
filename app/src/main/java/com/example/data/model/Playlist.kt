package com.example.data.model

import androidx.room.Embedded
import androidx.room.Entity
import androidx.room.ForeignKey
import androidx.room.Index
import androidx.room.Junction
import androidx.room.PrimaryKey
import androidx.room.Relation

@Entity(tableName = "playlists")
data class PlaylistEntity(
    @PrimaryKey(autoGenerate = true) val id: Long = 0L,
    val name: String,
    val createdAt: Long = System.currentTimeMillis(),
)

@Entity(
    tableName = "playlist_video_cross_ref",
    primaryKeys = ["playlistId", "videoId"],
    indices = [Index(value = ["videoId"])],
    foreignKeys = [
        ForeignKey(
            entity = PlaylistEntity::class,
            parentColumns = ["id"],
            childColumns = ["playlistId"],
            onDelete = ForeignKey.CASCADE,
        ),
        ForeignKey(
            entity = VideoItem::class,
            parentColumns = ["id"],
            childColumns = ["videoId"],
            onDelete = ForeignKey.CASCADE,
        ),
    ],
)
data class PlaylistVideoCrossRef(
    val playlistId: Long,
    val videoId: Long,
    val addedAt: Long = System.currentTimeMillis(),
)

data class PlaylistWithVideos(
    @Embedded val playlist: PlaylistEntity,
    @Relation(
        parentColumn = "id",
        entityColumn = "id",
        associateBy = Junction(
            value = PlaylistVideoCrossRef::class,
            parentColumn = "playlistId",
            entityColumn = "videoId",
        ),
    )
    val videos: List<VideoItem>,
)
