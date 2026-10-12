package com.avemuzik.player.ui.viewmodel

import androidx.compose.runtime.Immutable
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import androidx.lifecycle.viewmodel.initializer
import androidx.lifecycle.viewmodel.viewModelFactory
import com.avemuzik.player.AppContainer
import com.avemuzik.player.data.media.SearchNormalizer
import com.avemuzik.player.data.repository.LibraryState
import com.avemuzik.player.data.repository.MusicRepository
import com.avemuzik.player.model.Album
import com.avemuzik.player.model.Artist
import com.avemuzik.player.model.Library
import com.avemuzik.player.model.Song
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.FlowPreview
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.debounce
import kotlinx.coroutines.flow.flowOn
import kotlinx.coroutines.flow.stateIn

@Immutable
data class SearchResults(
    val query: String = "",
    val songs: List<Song> = emptyList(),
    val albums: List<Album> = emptyList(),
    val artists: List<Artist> = emptyList(),
) {
    val isEmpty: Boolean get() = songs.isEmpty() && albums.isEmpty() && artists.isEmpty()
}

class LibraryViewModel(private val repository: MusicRepository) : ViewModel() {

    val state: StateFlow<LibraryState> = repository.state

    private val query = MutableStateFlow("")

    /** Arama tamamen yerel ve bellekte yapılır; yazarken kısa bir gecikmeyle tetiklenir. */
    @OptIn(FlowPreview::class)
    val searchResults: StateFlow<SearchResults> =
        combine(query.debounce(SEARCH_DEBOUNCE_MS), repository.library) { q, library -> search(library, q) }
            .flowOn(Dispatchers.Default)
            .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), SearchResults())

    fun ensureLoaded() = repository.ensureLoaded()

    fun refresh() = repository.refresh()

    fun onQueryChange(value: String) {
        query.value = value
    }

    private fun search(library: Library, raw: String): SearchResults {
        val tokens = SearchNormalizer.tokens(raw.trim())
        if (tokens.isEmpty()) return SearchResults(query = raw)
        fun matches(key: String) = tokens.all { key.contains(it) }
        return SearchResults(
            query = raw,
            songs = library.songs.filter { matches(it.searchKey) },
            albums = library.albums.filter { matches(it.searchKey) },
            artists = library.artists.filter { matches(it.searchKey) },
        )
    }

    companion object {
        private const val SEARCH_DEBOUNCE_MS = 120L

        fun factory(container: AppContainer) = viewModelFactory {
            initializer { LibraryViewModel(container.musicRepository) }
        }
    }
}
