package com.avemuzik.player.data.database

import android.content.Context
import androidx.room.Database
import androidx.room.Room
import androidx.room.RoomDatabase

@Database(
    entities = [FavoriteEntity::class, PlaylistEntity::class, PlaylistSongEntity::class],
    version = 1,
    exportSchema = true,
)
abstract class AveDatabase : RoomDatabase() {
    abstract fun favoriteDao(): FavoriteDao
    abstract fun playlistDao(): PlaylistDao

    companion object {
        fun build(context: Context): AveDatabase =
            Room.databaseBuilder(context, AveDatabase::class.java, "ave_muzik.db").build()
    }
}
