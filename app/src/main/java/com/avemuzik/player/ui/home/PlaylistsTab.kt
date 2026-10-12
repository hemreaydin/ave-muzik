package com.avemuzik.player.ui.home

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.rounded.QueueMusic
import androidx.compose.material.icons.rounded.Add
import androidx.compose.material.icons.rounded.Delete
import androidx.compose.material.icons.rounded.Edit
import androidx.compose.material.icons.rounded.Favorite
import androidx.compose.material.icons.rounded.MoreVert
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalResources
import androidx.compose.ui.res.pluralStringResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.avemuzik.player.R
import com.avemuzik.player.model.Library
import com.avemuzik.player.model.Playlist
import com.avemuzik.player.navigation.AppNavigator
import com.avemuzik.player.ui.components.ConfirmDialog
import com.avemuzik.player.ui.components.EntityRow
import com.avemuzik.player.ui.components.IconTile
import com.avemuzik.player.ui.components.LocalMessenger
import com.avemuzik.player.ui.components.TextInputDialog
import com.avemuzik.player.ui.viewmodel.CollectionsViewModel

@Composable
fun PlaylistsTab(
    collectionsViewModel: CollectionsViewModel,
    library: Library?,
    navigator: AppNavigator,
) {
    val playlists by collectionsViewModel.playlistList.collectAsStateWithLifecycle()
    val favoriteIds by collectionsViewModel.favoriteIds.collectAsStateWithLifecycle()
    // Cihazdan silinmiş şarkılar sayılmaz.
    val favoriteCount = remember(favoriteIds, library) {
        library?.let { lib -> favoriteIds.count { it in lib.songsById } } ?: favoriteIds.size
    }

    val messenger = LocalMessenger.current
    val resources = LocalResources.current
    var creating by rememberSaveable { mutableStateOf(false) }
    var renaming by remember { mutableStateOf<Playlist?>(null) }
    var deleting by remember { mutableStateOf<Playlist?>(null) }

    LazyColumn(modifier = Modifier.fillMaxSize(), contentPadding = PaddingValues(vertical = 4.dp)) {
        item(key = "new") {
            EntityRow(
                title = stringResource(R.string.new_playlist),
                subtitle = stringResource(R.string.create_playlist),
                onClick = { creating = true },
                leading = { IconTile(Icons.Rounded.Add, highlighted = true) },
            )
        }
        item(key = "favorites") {
            EntityRow(
                title = stringResource(R.string.favorites),
                subtitle = pluralStringResource(R.plurals.song_count, favoriteCount, favoriteCount),
                onClick = navigator::openFavorites,
                leading = { IconTile(Icons.Rounded.Favorite, highlighted = true) },
            )
        }
        items(playlists, key = { it.id }) { playlist ->
            EntityRow(
                title = playlist.name,
                subtitle = pluralStringResource(R.plurals.song_count, playlist.songCount, playlist.songCount),
                onClick = { navigator.openPlaylist(playlist.id) },
                leading = { IconTile(Icons.AutoMirrored.Rounded.QueueMusic) },
                trailing = {
                    PlaylistMenu(
                        onRename = { renaming = playlist },
                        onDelete = { deleting = playlist },
                    )
                },
            )
        }
        if (playlists.isEmpty()) {
            item(key = "empty") {
                Text(
                    text = stringResource(R.string.playlists_empty),
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    textAlign = TextAlign.Center,
                    modifier = Modifier.padding(32.dp),
                )
            }
        }
    }

    if (creating) {
        TextInputDialog(
            title = stringResource(R.string.create_playlist),
            initialValue = "",
            confirmLabel = stringResource(R.string.create),
            onConfirm = { name ->
                collectionsViewModel.createPlaylist(name) {
                    messenger.show(resources.getString(R.string.msg_playlist_created, name))
                }
                creating = false
            },
            onDismiss = { creating = false },
        )
    }
    renaming?.let { playlist ->
        TextInputDialog(
            title = stringResource(R.string.rename_playlist),
            initialValue = playlist.name,
            confirmLabel = stringResource(R.string.save),
            onConfirm = { name ->
                collectionsViewModel.renamePlaylist(playlist.id, name)
                renaming = null
            },
            onDismiss = { renaming = null },
        )
    }
    deleting?.let { playlist ->
        ConfirmDialog(
            title = stringResource(R.string.delete_playlist),
            message = stringResource(R.string.delete_playlist_confirm, playlist.name),
            confirmLabel = stringResource(R.string.delete),
            onConfirm = {
                collectionsViewModel.deletePlaylist(playlist.id)
                deleting = null
            },
            onDismiss = { deleting = null },
        )
    }
}

@Composable
fun PlaylistMenu(onRename: () -> Unit, onDelete: () -> Unit) {
    var expanded by remember { mutableStateOf(false) }
    Box {
        IconButton(onClick = { expanded = true }) {
            Icon(
                Icons.Rounded.MoreVert,
                contentDescription = stringResource(R.string.more_options),
                tint = MaterialTheme.colorScheme.onSurfaceVariant,
            )
        }
        DropdownMenu(expanded = expanded, onDismissRequest = { expanded = false }) {
            DropdownMenuItem(
                text = { Text(stringResource(R.string.rename)) },
                leadingIcon = { Icon(Icons.Rounded.Edit, contentDescription = null) },
                onClick = {
                    expanded = false
                    onRename()
                },
            )
            DropdownMenuItem(
                text = { Text(stringResource(R.string.delete)) },
                leadingIcon = { Icon(Icons.Rounded.Delete, contentDescription = null) },
                onClick = {
                    expanded = false
                    onDelete()
                },
            )
        }
    }
}
