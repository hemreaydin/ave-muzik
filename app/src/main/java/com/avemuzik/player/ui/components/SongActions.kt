package com.avemuzik.player.ui.components

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.rounded.PlaylistAdd
import androidx.compose.material.icons.automirrored.rounded.PlaylistPlay
import androidx.compose.material.icons.automirrored.rounded.QueueMusic
import androidx.compose.material.icons.rounded.Album
import androidx.compose.material.icons.rounded.Favorite
import androidx.compose.material.icons.rounded.FavoriteBorder
import androidx.compose.material.icons.rounded.Person
import androidx.compose.material.icons.rounded.RemoveCircleOutline
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.Text
import androidx.compose.material3.rememberModalBottomSheetState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.Stable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.LocalResources
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.avemuzik.player.R
import com.avemuzik.player.model.Song
import com.avemuzik.player.navigation.AppNavigator
import com.avemuzik.player.ui.viewmodel.CollectionsViewModel
import com.avemuzik.player.ui.viewmodel.PlayerViewModel
import kotlinx.coroutines.launch

/** Tüm ekranların paylaştığı şarkı menüsü durumu (tek bir alt sayfa, tek bir liste seçici). */
@Stable
class SongActionsState {
    var song by mutableStateOf<Song?>(null)
        private set
    var playlistId by mutableStateOf<Long?>(null)
        private set
    var pickerSongs by mutableStateOf<List<Song>?>(null)
        private set

    /** @param playlistId menü bir listenin içinden açıldıysa "Listeden çıkar" seçeneği görünür. */
    fun open(song: Song, playlistId: Long? = null) {
        this.song = song
        this.playlistId = playlistId
    }

    fun dismiss() {
        song = null
        playlistId = null
    }

    fun pickPlaylistFor(songs: List<Song>) {
        pickerSongs = songs
    }

    fun dismissPicker() {
        pickerSongs = null
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SongActionsHost(
    state: SongActionsState,
    collectionsViewModel: CollectionsViewModel,
    playerViewModel: PlayerViewModel,
    navigator: AppNavigator,
) {
    val messenger = LocalMessenger.current
    val resources = LocalResources.current
    val favorites by collectionsViewModel.favoriteIds.collectAsStateWithLifecycle()

    val song = state.song
    if (song != null) {
        val sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true)
        val scope = rememberCoroutineScope()
        val isFavorite = song.id in favorites
        val playlistId = state.playlistId

        fun closeThen(action: () -> Unit) {
            scope.launch { sheetState.hide() }.invokeOnCompletion {
                state.dismiss()
                action()
            }
        }

        ModalBottomSheet(onDismissRequest = state::dismiss, sheetState = sheetState) {
            SheetHeader(song)
            HorizontalDivider(Modifier.padding(vertical = 8.dp), color = MaterialTheme.colorScheme.outlineVariant)
            ActionRow(Icons.AutoMirrored.Rounded.PlaylistPlay, stringResource(R.string.action_play_next)) {
                closeThen {
                    playerViewModel.playNext(song)
                    messenger.show(resources.getString(R.string.msg_play_next))
                }
            }
            ActionRow(Icons.AutoMirrored.Rounded.QueueMusic, stringResource(R.string.action_add_to_queue)) {
                closeThen {
                    playerViewModel.addToQueue(song)
                    messenger.show(resources.getString(R.string.msg_added_to_queue))
                }
            }
            ActionRow(
                icon = if (isFavorite) Icons.Rounded.Favorite else Icons.Rounded.FavoriteBorder,
                text = stringResource(if (isFavorite) R.string.action_remove_favorite else R.string.action_add_favorite),
                tint = if (isFavorite) MaterialTheme.colorScheme.primary else null,
            ) {
                closeThen {
                    collectionsViewModel.toggleFavorite(song.id) { nowFavorite ->
                        messenger.show(
                            resources.getString(if (nowFavorite) R.string.msg_added_favorite else R.string.msg_removed_favorite),
                        )
                    }
                }
            }
            ActionRow(Icons.AutoMirrored.Rounded.PlaylistAdd, stringResource(R.string.add_to_playlist)) {
                closeThen { state.pickPlaylistFor(listOf(song)) }
            }
            if (playlistId != null) {
                ActionRow(Icons.Rounded.RemoveCircleOutline, stringResource(R.string.action_remove_from_playlist)) {
                    closeThen {
                        collectionsViewModel.removeFromPlaylist(playlistId, song.id)
                        messenger.show(resources.getString(R.string.msg_removed_from_playlist))
                    }
                }
            }
            ActionRow(Icons.Rounded.Album, stringResource(R.string.action_go_to_album)) {
                closeThen {
                    navigator.leaveNowPlaying()
                    navigator.openAlbum(song.albumId)
                }
            }
            ActionRow(Icons.Rounded.Person, stringResource(R.string.action_go_to_artist)) {
                closeThen {
                    navigator.leaveNowPlaying()
                    navigator.openArtist(song.artistId)
                }
            }
            Spacer(Modifier.height(12.dp))
        }
    }

    val pickerSongs = state.pickerSongs
    if (pickerSongs != null) {
        val playlists by collectionsViewModel.playlistList.collectAsStateWithLifecycle()
        val songIds = pickerSongs.map { it.id }
        AddToPlaylistDialog(
            playlists = playlists,
            onPick = { playlist ->
                collectionsViewModel.addToPlaylist(playlist.id, songIds)
                messenger.show(resources.getString(R.string.msg_added_to_playlist, playlist.name))
                state.dismissPicker()
            },
            onCreate = { name ->
                collectionsViewModel.createPlaylist(name, songIds) {
                    messenger.show(resources.getString(R.string.msg_added_to_playlist, name))
                }
                state.dismissPicker()
            },
            onDismiss = state::dismissPicker,
        )
    }
}

@Composable
private fun SheetHeader(song: Song) {
    Row(
        modifier = Modifier.fillMaxWidth().padding(horizontal = 20.dp, vertical = 4.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        ArtworkImage(
            songId = song.id,
            loadSize = 56.dp,
            modifier = Modifier.size(56.dp),
            shape = RoundedCornerShape(12.dp),
        )
        Spacer(Modifier.width(14.dp))
        Column(Modifier.weight(1f)) {
            Text(song.title, style = MaterialTheme.typography.titleMedium, maxLines = 1, overflow = TextOverflow.Ellipsis)
            Text(
                text = stringResource(R.string.meta_separator, song.artist, song.album),
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
            )
            Text(
                text = "${song.folderPath}/${song.fileName}",
                style = MaterialTheme.typography.labelSmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.8f),
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
            )
        }
    }
}

@Composable
private fun ActionRow(icon: ImageVector, text: String, tint: Color? = null, onClick: () -> Unit) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clickable(onClick = onClick)
            .padding(horizontal = 24.dp, vertical = 14.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Icon(icon, contentDescription = null, tint = tint ?: MaterialTheme.colorScheme.onSurfaceVariant)
        Spacer(Modifier.width(20.dp))
        Text(text, style = MaterialTheme.typography.bodyLarge)
    }
}
