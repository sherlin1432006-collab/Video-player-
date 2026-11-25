package com.example.data.local

import android.content.Context
import androidx.room.Database
import androidx.room.Room
import androidx.room.RoomDatabase
import androidx.room.TypeConverter
import androidx.room.TypeConverters
import com.example.data.model.PlaylistEntity
import com.example.data.model.PlaylistVideoCrossRef
import com.example.data.model.VideoItem
import com.example.data.model.VideoSourceType

class ReelFlowTypeConverters {
    @TypeConverter
    fun fromSourceType(type: VideoSourceType): String = type.name

    @TypeConverter
    fun toSourceType(value: String): VideoSourceType =
        runCatching { VideoSourceType.valueOf(value) }.getOrDefault(VideoSourceType.LOCAL_FILE)
}

@Database(
    entities = [VideoItem::class, PlaylistEntity::class, PlaylistVideoCrossRef::class],
    version = 1,
    exportSchema = false,
)
@TypeConverters(ReelFlowTypeConverters::class)
abstract class ReelFlowDatabase : RoomDatabase() {
    abstract fun videoDao(): VideoDao
    abstract fun playlistDao(): PlaylistDao

    companion object {
        @Volatile
        private var INSTANCE: ReelFlowDatabase? = null

        fun getInstance(context: Context): ReelFlowDatabase {
            return INSTANCE ?: synchronized(this) {
                INSTANCE ?: Room.databaseBuilder(
                    context.applicationContext,
                    ReelFlowDatabase::class.java,
                    "reelflow_database",
                ).build().also { INSTANCE = it }
            }
        }
    }
}
