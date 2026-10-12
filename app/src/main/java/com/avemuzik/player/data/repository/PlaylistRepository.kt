package com.avemuzik.player.data.repository

import com.avemuzik.player.data.database.PlaylistDao
import com.avemuzik.player.model.Playlist
import kotlinx.coroutines.flow.Flow

class PlaylistRepository(private val dao: PlaylistDao) {

    val playlists: Flow<List<Playlist>> = dao.observePlaylists()

    fun songIds(playlistId: Long): Flow<List<Long>> = dao.observeSongIds(playlistId)

    fun name(playlistId: Long): Flow<String?> = dao.observeName(playlistId)

    suspend fun create(name: String, songIds: List<Long> = emptyList()): Long =
        dao.createPlaylist(name.trim(), songIds)

    suspend fun rename(playlistId: Long, name: String) = dao.rename(playlistId, name.trim())

    suspend fun delete(playlistId: Long) = dao.deletePlaylist(playlistId)

    suspend fun addSongs(playlistId: Long, songIds: List<Long>) = dao.addSongs(playlistId, songIds)

    suspend fun removeSong(playlistId: Long, songId: Long) = dao.removeSong(playlistId, songId)
}
