package com.avemuzik.player.ui.home

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.Album
import androidx.compose.material.icons.rounded.Folder
import androidx.compose.material.icons.rounded.Person
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.pluralStringResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import com.avemuzik.player.R
import com.avemuzik.player.model.Album
import com.avemuzik.player.model.Artist
import com.avemuzik.player.model.Folder
import com.avemuzik.player.model.Song
import com.avemuzik.player.ui.components.AlbumCard
import com.avemuzik.player.ui.components.ArtistRow
import com.avemuzik.player.ui.components.EntityRow
import com.avemuzik.player.ui.components.FastScroller
import com.avemuzik.player.ui.components.IconTile
import com.avemuzik.player.ui.components.LocalSongActions
import com.avemuzik.player.ui.components.MessageState
import com.avemuzik.player.ui.components.PlayHeader
import com.avemuzik.player.ui.components.SongRow

@Composable
fun SongsTab(
    songs: List<Song>,
    currentSongId: Long?,
    onPlay: (index: Int) -> Unit,
    onShuffle: () -> Unit,
) {
    val songActions = LocalSongActions.current
    val listState = rememberLazyListState()
    Box(Modifier.fillMaxSize()) {
        LazyColumn(
            state = listState,
            modifier = Modifier.fillMaxSize(),
            contentPadding = PaddingValues(bottom = 8.dp),
        ) {
            item(key = "header", contentType = "header") {
                PlayHeader(songCount = songs.size, onPlay = { onPlay(0) }, onShuffle = onShuffle)
            }
            itemsIndexed(songs, key = { _, song -> song.id }, contentType = { _, _ -> "song" }) { index, song ->
                SongRow(
                    song = song,
                    isCurrent = song.id == currentSongId,
                    onClick = { onPlay(index) },
                    onMore = { songActions.open(song) },
                )
            }
        }
        FastScroller(state = listState, itemCount = songs.size + 1)
    }
}

@Composable
fun AlbumsTab(albums: List<Album>, onOpen: (Long) -> Unit) {
    if (albums.isEmpty()) {
        MessageState(Icons.Rounded.Album, title = null, body = stringResource(R.string.albums_empty))
        return
    }
    LazyVerticalGrid(
        columns = GridCells.Adaptive(minSize = 148.dp),
        modifier = Modifier.fillMaxSize(),
        contentPadding = PaddingValues(horizontal = 12.dp, vertical = 8.dp),
        horizontalArrangement = Arrangement.spacedBy(8.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp),
    ) {
        items(albums, key = { it.id }, contentType = { "album" }) { album ->
            AlbumCard(album = album, onClick = { onOpen(album.id) })
        }
    }
}

@Composable
fun ArtistsTab(artists: List<Artist>, onOpen: (Long) -> Unit) {
    if (artists.isEmpty()) {
        MessageState(Icons.Rounded.Person, title = null, body = stringResource(R.string.artists_empty))
        return
    }
    val listState = rememberLazyListState()
    Box(Modifier.fillMaxSize()) {
        LazyColumn(state = listState, modifier = Modifier.fillMaxSize(), contentPadding = PaddingValues(vertical = 4.dp)) {
            items(artists, key = { it.id }, contentType = { "artist" }) { artist ->
                ArtistRow(artist = artist, onClick = { onOpen(artist.id) })
            }
        }
        FastScroller(state = listState, itemCount = artists.size)
    }
}

@Composable
fun FoldersTab(folders: List<Folder>, onOpen: (String) -> Unit) {
    if (folders.isEmpty()) {
        MessageState(Icons.Rounded.Folder, title = null, body = stringResource(R.string.folders_empty))
        return
    }
    LazyColumn(modifier = Modifier.fillMaxSize(), contentPadding = PaddingValues(vertical = 4.dp)) {
        items(folders, key = { it.path }, contentType = { "folder" }) { folder ->
            EntityRow(
                title = folder.name,
                subtitle = stringResource(
                    R.string.meta_separator,
                    pluralStringResource(R.plurals.song_count, folder.songCount, folder.songCount),
                    folder.path,
                ),
                onClick = { onOpen(folder.path) },
                leading = { IconTile(Icons.Rounded.Folder) },
            )
        }
    }
}
