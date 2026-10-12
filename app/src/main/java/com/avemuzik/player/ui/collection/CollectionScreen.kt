package com.avemuzik.player.ui.collection

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.rounded.ArrowBack
import androidx.compose.material.icons.automirrored.rounded.QueueMusic
import androidx.compose.material.icons.rounded.Album
import androidx.compose.material.icons.rounded.Favorite
import androidx.compose.material.icons.rounded.Folder
import androidx.compose.material.icons.rounded.Person
import androidx.compose.material.icons.rounded.PlayArrow
import androidx.compose.material.icons.rounded.SearchOff
import androidx.compose.material.icons.rounded.Shuffle
import androidx.compose.material3.Button
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FilledTonalButton
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.derivedStateOf
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalConfiguration
import androidx.compose.ui.res.pluralStringResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import com.avemuzik.player.R
import com.avemuzik.player.navigation.AppNavigator
import com.avemuzik.player.ui.components.ArtworkImage
import com.avemuzik.player.ui.components.ConfirmDialog
import com.avemuzik.player.ui.components.LoadingState
import com.avemuzik.player.ui.components.LocalAppContainer
import com.avemuzik.player.ui.components.LocalSongActions
import com.avemuzik.player.ui.components.MessageState
import com.avemuzik.player.ui.components.SongRow
import com.avemuzik.player.ui.components.TextInputDialog
import com.avemuzik.player.ui.components.totalDurationText
import com.avemuzik.player.ui.home.PlaylistMenu
import com.avemuzik.player.ui.viewmodel.CollectionsViewModel
import com.avemuzik.player.ui.viewmodel.PlayerViewModel
import kotlinx.coroutines.flow.distinctUntilChanged
import kotlinx.coroutines.flow.map

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun CollectionScreen(
    source: CollectionSource,
    playerViewModel: PlayerViewModel,
    collectionsViewModel: CollectionsViewModel,
    navigator: AppNavigator,
) {
    val container = LocalAppContainer.current
    val favoritesTitle = stringResource(R.string.favorites)
    val viewModel: CollectionViewModel = viewModel(
        factory = CollectionViewModel.factory(container, source, favoritesTitle),
    )
    val state by viewModel.state.collectAsStateWithLifecycle()
    val currentSongId by remember(playerViewModel) {
        playerViewModel.state.map { it.songId }.distinctUntilChanged()
    }.collectAsStateWithLifecycle(initialValue = null)

    val songActions = LocalSongActions.current
    val listState = rememberLazyListState()
    val showTitleInBar by remember { derivedStateOf { listState.firstVisibleItemIndex > 0 } }
    val playlistId = (source as? CollectionSource.Playlist)?.playlistId

    var renaming by remember { mutableStateOf(false) }
    var deleting by remember { mutableStateOf(false) }

    // Liste başka bir yerden silinirse bu ekranı kapat.
    if (playlistId != null) {
        LaunchedEffect(state?.notFound) { if (state?.notFound == true) navigator.back() }
    }

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
                    if (showTitleInBar) {
                        Text(state?.title.orEmpty(), maxLines = 1, overflow = TextOverflow.Ellipsis)
                    }
                },
                actions = {
                    if (playlistId != null && state?.notFound == false) {
                        PlaylistMenu(onRename = { renaming = true }, onDelete = { deleting = true })
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(containerColor = MaterialTheme.colorScheme.background),
            )
        },
    ) { padding ->
        Box(Modifier.padding(padding).fillMaxSize()) {
            val current = state
            when {
                current == null -> LoadingState(stringResource(R.string.library_loading))

                current.notFound && playlistId == null -> MessageState(
                    icon = Icons.Rounded.SearchOff,
                    title = null,
                    body = stringResource(R.string.collection_not_found),
                )

                else -> LazyColumn(
                    state = listState,
                    modifier = Modifier.fillMaxSize(),
                    contentPadding = PaddingValues(bottom = 16.dp),
                ) {
                    item(key = "header", contentType = "header") {
                        CollectionHeader(
                            source = source,
                            state = current,
                            onPlay = { playerViewModel.playAll(current.songs) },
                            onShuffle = { playerViewModel.shuffleAll(current.songs) },
                        )
                    }
                    if (current.songs.isEmpty()) {
                        item(key = "empty") {
                            Text(
                                text = stringResource(
                                    if (source is CollectionSource.Favorites) R.string.favorites_empty else R.string.playlist_empty,
                                ),
                                style = MaterialTheme.typography.bodyMedium,
                                color = MaterialTheme.colorScheme.onSurfaceVariant,
                                textAlign = TextAlign.Center,
                                modifier = Modifier.fillMaxWidth().padding(32.dp),
                            )
                        }
                    }
                    itemsIndexed(current.songs, key = { _, song -> song.id }, contentType = { _, _ -> "song" }) { index, song ->
                        SongRow(
                            song = song,
                            isCurrent = song.id == currentSongId,
                            onClick = { playerViewModel.play(current.songs, index) },
                            onMore = { songActions.open(song, playlistId) },
                        )
                    }
                }
            }
        }
    }

    val current = state
    if (renaming && playlistId != null && current != null) {
        TextInputDialog(
            title = stringResource(R.string.rename_playlist),
            initialValue = current.title,
            confirmLabel = stringResource(R.string.save),
            onConfirm = { name ->
                collectionsViewModel.renamePlaylist(playlistId, name)
                renaming = false
            },
            onDismiss = { renaming = false },
        )
    }
    if (deleting && playlistId != null && current != null) {
        ConfirmDialog(
            title = stringResource(R.string.delete_playlist),
            message = stringResource(R.string.delete_playlist_confirm, current.title),
            confirmLabel = stringResource(R.string.delete),
            onConfirm = {
                deleting = false
                collectionsViewModel.deletePlaylist(playlistId)
                navigator.back()
            },
            onDismiss = { deleting = false },
        )
    }
}

@Composable
private fun CollectionHeader(
    source: CollectionSource,
    state: CollectionUiState,
    onPlay: () -> Unit,
    onShuffle: () -> Unit,
) {
    val (kindLabel, placeholder) = when (source) {
        is CollectionSource.Album -> R.string.kind_album to Icons.Rounded.Album
        is CollectionSource.Artist -> R.string.kind_artist to Icons.Rounded.Person
        is CollectionSource.Folder -> R.string.kind_folder to Icons.Rounded.Folder
        is CollectionSource.Playlist -> R.string.kind_playlist to Icons.AutoMirrored.Rounded.QueueMusic
        CollectionSource.Favorites -> R.string.kind_playlist to Icons.Rounded.Favorite
    }
    // Klasör ve favorilerde ikon, diğerlerinde kapak gösterilir.
    val showArt = source !is CollectionSource.Folder && source !is CollectionSource.Favorites

    Column(
        modifier = Modifier.fillMaxWidth().padding(horizontal = 24.dp, vertical = 8.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
    ) {
        ArtworkImage(
            songId = if (showArt) state.artSongId else null,
            loadSize = 200.dp,
            modifier = Modifier.size(200.dp),
            shape = if (source is CollectionSource.Artist) CircleShape else RoundedCornerShape(20.dp),
            placeholderIcon = placeholder,
            placeholderIconSize = 72.dp,
        )
        Spacer(Modifier.height(20.dp))
        Text(
            text = stringResource(kindLabel).uppercase(LocalConfiguration.current.locales[0]),
            style = MaterialTheme.typography.labelMedium,
            color = MaterialTheme.colorScheme.primary,
        )
        Spacer(Modifier.height(4.dp))
        Text(
            text = state.title,
            style = MaterialTheme.typography.headlineSmall,
            textAlign = TextAlign.Center,
            maxLines = 2,
            overflow = TextOverflow.Ellipsis,
        )
        state.secondary?.let {
            Spacer(Modifier.height(4.dp))
            Text(
                text = it,
                style = MaterialTheme.typography.bodyLarge,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                textAlign = TextAlign.Center,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
            )
        }
        Spacer(Modifier.height(4.dp))
        Text(
            text = stringResource(
                R.string.meta_separator,
                pluralStringResource(R.plurals.song_count, state.songs.size, state.songs.size),
                totalDurationText(state.songs.sumOf { it.durationMs }),
            ),
            style = MaterialTheme.typography.bodySmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
        )
        Spacer(Modifier.height(20.dp))
        Row(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
            Button(onClick = onPlay, enabled = state.songs.isNotEmpty()) {
                Icon(Icons.Rounded.PlayArrow, contentDescription = null, modifier = Modifier.size(20.dp))
                Spacer(Modifier.width(8.dp))
                Text(stringResource(R.string.play))
            }
            FilledTonalButton(onClick = onShuffle, enabled = state.songs.isNotEmpty()) {
                Icon(Icons.Rounded.Shuffle, contentDescription = null, modifier = Modifier.size(20.dp))
                Spacer(Modifier.width(8.dp))
                Text(stringResource(R.string.shuffle))
            }
        }
        Spacer(Modifier.height(12.dp))
    }
}
