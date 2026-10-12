package com.avemuzik.player.ui.viewmodel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import androidx.lifecycle.viewmodel.initializer
import androidx.lifecycle.viewmodel.viewModelFactory
import com.avemuzik.player.AppContainer
import com.avemuzik.player.data.repository.FavoritesRepository
import com.avemuzik.player.data.repository.PlaylistRepository
import com.avemuzik.player.model.Playlist
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch

/** Favoriler ve çalma listeleri (tamamen yerel, Room veritabanında). */
class CollectionsViewModel(
    private val favorites: FavoritesRepository,
    private val playlists: PlaylistRepository,
) : ViewModel() {

    val favoriteIds: StateFlow<Set<Long>> = favorites.favoriteIds
        .map { it.toSet() }
        .stateIn(viewModelScope, SharingStarted.Eagerly, emptySet())

    val playlistList: StateFlow<List<Playlist>> = playlists.playlists
        .stateIn(viewModelScope, SharingStarted.Eagerly, emptyList())

    fun toggleFavorite(songId: Long, onResult: (isFavorite: Boolean) -> Unit = {}) {
        viewModelScope.launch { onResult(favorites.toggle(songId)) }
    }

    fun createPlaylist(name: String, songIds: List<Long> = emptyList(), onCreated: (Long) -> Unit = {}) {
        if (name.isBlank()) return
        viewModelScope.launch { onCreated(playlists.create(name, songIds)) }
    }

    fun renamePlaylist(playlistId: Long, name: String) {
        if (name.isBlank()) return
        viewModelScope.launch { playlists.rename(playlistId, name) }
    }

    fun deletePlaylist(playlistId: Long) {
        viewModelScope.launch { playlists.delete(playlistId) }
    }

    fun addToPlaylist(playlistId: Long, songIds: List<Long>) {
        viewModelScope.launch { playlists.addSongs(playlistId, songIds) }
    }

    fun removeFromPlaylist(playlistId: Long, songId: Long) {
        viewModelScope.launch { playlists.removeSong(playlistId, songId) }
    }

    companion object {
        fun factory(container: AppContainer) = viewModelFactory {
            initializer { CollectionsViewModel(container.favoritesRepository, container.playlistRepository) }
        }
    }
}
