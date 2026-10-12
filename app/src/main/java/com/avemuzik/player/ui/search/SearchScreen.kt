package com.avemuzik.player.ui.search

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.rounded.ArrowBack
import androidx.compose.material.icons.rounded.Close
import androidx.compose.material.icons.rounded.Search
import androidx.compose.material.icons.rounded.SearchOff
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TextField
import androidx.compose.material3.TextFieldDefaults
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.focus.focusRequester
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalSoftwareKeyboardController
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.avemuzik.player.R
import com.avemuzik.player.navigation.AppNavigator
import com.avemuzik.player.ui.components.AlbumCard
import com.avemuzik.player.ui.components.ArtistRow
import com.avemuzik.player.ui.components.LocalSongActions
import com.avemuzik.player.ui.components.MessageState
import com.avemuzik.player.ui.components.SongRow
import com.avemuzik.player.ui.viewmodel.LibraryViewModel
import com.avemuzik.player.ui.viewmodel.PlayerViewModel
import kotlinx.coroutines.flow.distinctUntilChanged
import kotlinx.coroutines.flow.map

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SearchScreen(
    libraryViewModel: LibraryViewModel,
    playerViewModel: PlayerViewModel,
    navigator: AppNavigator,
) {
    var query by rememberSaveable { mutableStateOf("") }
    LaunchedEffect(query) { libraryViewModel.onQueryChange(query) }

    val results by libraryViewModel.searchResults.collectAsStateWithLifecycle()
    val currentSongId by remember(playerViewModel) {
        playerViewModel.state.map { it.songId }.distinctUntilChanged()
    }.collectAsStateWithLifecycle(initialValue = null)
    val songActions = LocalSongActions.current
    val focusRequester = remember { FocusRequester() }
    val keyboard = LocalSoftwareKeyboardController.current

    LaunchedEffect(Unit) { focusRequester.requestFocus() }

    Scaffold(
        contentWindowInsets = WindowInsets(0),
        topBar = {
            TopAppBar(
                navigationIcon = {
                    IconButton(onClick = navigator::back) {
                        Icon(Icons.AutoMirrored.Rounded.ArrowBack, contentDescription = stringResource(R.string.back))
                    }
                },
                title = {
                    TextField(
                        value = query,
                        onValueChange = { query = it },
                        placeholder = { Text(stringResource(R.string.search_hint)) },
                        singleLine = true,
                        trailingIcon = {
                            if (query.isNotEmpty()) {
                                IconButton(onClick = { query = "" }) {
                                    Icon(Icons.Rounded.Close, contentDescription = stringResource(R.string.clear))
                                }
                            }
                        },
                        keyboardOptions = KeyboardOptions(imeAction = ImeAction.Search),
                        keyboardActions = KeyboardActions(onSearch = { keyboard?.hide() }),
                        colors = TextFieldDefaults.colors(
                            focusedContainerColor = Color.Transparent,
                            unfocusedContainerColor = Color.Transparent,
                            focusedIndicatorColor = Color.Transparent,
                            unfocusedIndicatorColor = Color.Transparent,
                        ),
                        modifier = Modifier.fillMaxWidth().focusRequester(focusRequester),
                    )
                },
                colors = TopAppBarDefaults.topAppBarColors(containerColor = MaterialTheme.colorScheme.background),
            )
        },
    ) { padding ->
        Box(Modifier.padding(padding).fillMaxSize()) {
            // Sonuçlar, yazılan metinle eşleşene kadar gösterilmez (eski sonuç yanıp sönmesin).
            val upToDate = results.query == query
            when {
                query.isBlank() -> MessageState(
                    icon = Icons.Rounded.Search,
                    title = null,
                    body = stringResource(R.string.search_start),
                )

                upToDate && results.isEmpty -> MessageState(
                    icon = Icons.Rounded.SearchOff,
                    title = null,
                    body = stringResource(R.string.search_no_results, query.trim()),
                )

                upToDate -> LazyColumn(
                    modifier = Modifier.fillMaxSize(),
                    contentPadding = PaddingValues(bottom = 16.dp),
                ) {
                    if (results.artists.isNotEmpty()) {
                        item(key = "artists-header") { SectionHeader(stringResource(R.string.tab_artists)) }
                        items(results.artists.take(MAX_ARTISTS), key = { "artist-${it.id}" }) { artist ->
                            ArtistRow(artist = artist, onClick = { navigator.openArtist(artist.id) })
                        }
                    }
                    if (results.albums.isNotEmpty()) {
                        item(key = "albums-header") { SectionHeader(stringResource(R.string.tab_albums)) }
                        item(key = "albums-row") {
                            LazyRow(contentPadding = PaddingValues(horizontal = 12.dp)) {
                                items(results.albums, key = { it.id }) { album ->
                                    AlbumCard(
                                        album = album,
                                        onClick = { navigator.openAlbum(album.id) },
                                        modifier = Modifier.width(140.dp),
                                    )
                                }
                            }
                        }
                    }
                    if (results.songs.isNotEmpty()) {
                        item(key = "songs-header") { SectionHeader(stringResource(R.string.tab_songs)) }
                        itemsIndexed(results.songs, key = { _, song -> "song-${song.id}" }) { index, song ->
                            SongRow(
                                song = song,
                                isCurrent = song.id == currentSongId,
                                onClick = {
                                    keyboard?.hide()
                                    playerViewModel.play(results.songs, index)
                                },
                                onMore = { songActions.open(song) },
                            )
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun SectionHeader(text: String) {
    Text(
        text = text,
        style = MaterialTheme.typography.titleMedium,
        color = MaterialTheme.colorScheme.primary,
        modifier = Modifier.padding(start = 16.dp, end = 16.dp, top = 16.dp, bottom = 8.dp),
    )
}

private const val MAX_ARTISTS = 5
