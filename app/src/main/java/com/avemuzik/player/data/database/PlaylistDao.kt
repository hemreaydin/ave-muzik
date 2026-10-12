package com.avemuzik.player.data.database

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import androidx.room.Transaction
import com.avemuzik.player.model.Playlist
import kotlinx.coroutines.flow.Flow

@Dao
abstract class PlaylistDao {

    @Query(
        """
        SELECT p.id AS id, p.name AS name, COUNT(ps.songId) AS songCount
        FROM playlists p
        LEFT JOIN playlist_songs ps ON ps.playlistId = p.id
        GROUP BY p.id
        ORDER BY p.name COLLATE NOCASE
        """,
    )
    abstract fun observePlaylists(): Flow<List<Playlist>>

    @Query("SELECT songId FROM playlist_songs WHERE playlistId = :playlistId ORDER BY position")
    abstract fun observeSongIds(playlistId: Long): Flow<List<Long>>

    @Query("SELECT name FROM playlists WHERE id = :playlistId")
    abstract fun observeName(playlistId: Long): Flow<String?>

    @Insert
    abstract suspend fun insertPlaylist(entity: PlaylistEntity): Long

    @Query("UPDATE playlists SET name = :name WHERE id = :playlistId")
    abstract suspend fun rename(playlistId: Long, name: String)

    @Query("DELETE FROM playlists WHERE id = :playlistId")
    protected abstract suspend fun deletePlaylistRow(playlistId: Long)

    @Query("DELETE FROM playlist_songs WHERE playlistId = :playlistId")
    protected abstract suspend fun deletePlaylistSongs(playlistId: Long)

    @Query("SELECT COALESCE(MAX(position), -1) FROM playlist_songs WHERE playlistId = :playlistId")
    protected abstract suspend fun maxPosition(playlistId: Long): Int

    @Insert(onConflict = OnConflictStrategy.IGNORE)
    protected abstract suspend fun insertSongs(entities: List<PlaylistSongEntity>)

    @Query("DELETE FROM playlist_songs WHERE playlistId = :playlistId AND songId = :songId")
    abstract suspend fun removeSong(playlistId: Long, songId: Long)

    @Transaction
    open suspend fun addSongs(playlistId: Long, songIds: List<Long>) {
        val now = System.currentTimeMillis()
        var position = maxPosition(playlistId) + 1
        insertSongs(
            songIds.distinct().map { songId ->
                PlaylistSongEntity(playlistId, songId, position++, now)
            },
        )
    }

    @Transaction
    open suspend fun createPlaylist(name: String, songIds: List<Long>): Long {
        val id = insertPlaylist(PlaylistEntity(name = name, createdAt = System.currentTimeMillis()))
        if (songIds.isNotEmpty()) addSongs(id, songIds)
        return id
    }

    @Transaction
    open suspend fun deletePlaylist(playlistId: Long) {
        deletePlaylistSongs(playlistId)
        deletePlaylistRow(playlistId)
    }
}
