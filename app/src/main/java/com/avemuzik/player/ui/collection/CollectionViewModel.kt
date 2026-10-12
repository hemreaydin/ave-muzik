package com.avemuzik.player.ui.collection

import androidx.compose.runtime.Immutable
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import androidx.lifecycle.viewmodel.initializer
import androidx.lifecycle.viewmodel.viewModelFactory
import com.avemuzik.player.AppContainer
import com.avemuzik.player.data.repository.FavoritesRepository
import com.avemuzik.player.data.repository.MusicRepository
import com.avemuzik.player.data.repository.PlaylistRepository
import com.avemuzik.player.model.Song
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.flowOn
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.stateIn

/** Detay ekranında gösterilebilecek şarkı koleksiyonları. */
sealed interface CollectionSource {
    data class Album(val albumId: Long) : CollectionSource
    data class Artist(val artistId: Long) : CollectionSource
    data class Folder(val path: String) : CollectionSource
    data class Playlist(val playlistId: Long) : CollectionSource
    data object Favorites : CollectionSource
}

@Immutable
data class CollectionUiState(
    val title: String,
    /** Albüm için sanatçı, klasör için tam yol vb. */
    val secondary: String?,
    val songs: List<Song>,
    val artSongId: Long?,
    val notFound: Boolean = false,
)

class CollectionViewModel(
    source: CollectionSource,
    music: MusicRepository,
    favorites: FavoritesRepository,
    playlists: PlaylistRepository,
    favoritesTitle: String,
) : ViewModel() {

    private val upstream: Flow<CollectionUiState> = when (source) {
        is CollectionSource.Album -> music.library.map { library ->
            val album = library.albumsById[source.albumId]
            val songs = library.songsByAlbum[source.albumId].orEmpty()
            CollectionUiState(
                title = album?.title.orEmpty(),
                secondary = album?.artist,
                songs = songs,
                artSongId = album?.artSongId,
                notFound = album == null,
            )
        }

        is CollectionSource.Artist -> music.library.map { library ->
            val artist = library.artistsById[source.artistId]
            CollectionUiState(
                title = artist?.name.orEmpty(),
                secondary = null,
                songs = library.songsByArtist[source.artistId].orEmpty(),
                artSongId = artist?.artSongId,
                notFound = artist == null,
            )
        }

        is CollectionSource.Folder -> music.library.map { library ->
            val songs = library.songsByFolder[source.path].orEmpty()
            CollectionUiState(
                title = source.path.substringAfterLast('/'),
                secondary = source.path,
                songs = songs,
                artSongId = songs.firstOrNull()?.id,
                notFound = songs.isEmpty(),
            )
        }

        is CollectionSource.Playlist -> combine(
            music.library,
            playlists.songIds(source.playlistId),
            playlists.name(source.playlistId),
        ) { library, ids, name ->
            // Cihazdan silinmiş şarkılar listede gösterilmez.
            val songs = ids.mapNotNull { library.songsById[it] }
            CollectionUiState(
                title = name.orEmpty(),
                secondary = null,
                songs = songs,
                artSongId = songs.firstOrNull()?.id,
                notFound = name == null,
            )
        }

        CollectionSource.Favorites -> combine(music.library, favorites.favoriteIds) { library, ids ->
            val songs = ids.mapNotNull { library.songsById[it] }
            CollectionUiState(
                title = favoritesTitle,
                secondary = null,
                songs = songs,
                artSongId = songs.firstOrNull()?.id,
            )
        }
    }

    /** null: kütüphane henüz yüklenmedi. */
    val state: StateFlow<CollectionUiState?> = upstream
        .flowOn(Dispatchers.Default)
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), null)

    companion object {
        fun factory(container: AppContainer, source: CollectionSource, favoritesTitle: String) = viewModelFactory {
            initializer {
                CollectionViewModel(
                    source = source,
                    music = container.musicRepository,
                    favorites = container.favoritesRepository,
                    playlists = container.playlistRepository,
                    favoritesTitle = favoritesTitle,
                )
            }
        }
    }
}
