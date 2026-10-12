package com.avemuzik.player.ui.home

import androidx.annotation.StringRes
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.pager.HorizontalPager
import androidx.compose.foundation.pager.rememberPagerState
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.ErrorOutline
import androidx.compose.material.icons.rounded.MusicOff
import androidx.compose.material.icons.rounded.Search
import androidx.compose.material.icons.rounded.Settings
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.PrimaryScrollableTabRow
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Tab
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.avemuzik.player.R
import com.avemuzik.player.data.repository.LibraryState
import com.avemuzik.player.model.Library
import com.avemuzik.player.navigation.AppNavigator
import com.avemuzik.player.ui.components.AppTitle
import com.avemuzik.player.ui.components.LoadingState
import com.avemuzik.player.ui.components.MessageState
import com.avemuzik.player.ui.viewmodel.CollectionsViewModel
import com.avemuzik.player.ui.viewmodel.LibraryViewModel
import com.avemuzik.player.ui.viewmodel.PlayerViewModel
import kotlinx.coroutines.flow.distinctUntilChanged
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.launch

private enum class HomeTab(@param:StringRes val title: Int) {
    SONGS(R.string.tab_songs),
    ALBUMS(R.string.tab_albums),
    ARTISTS(R.string.tab_artists),
    PLAYLISTS(R.string.tab_playlists),
    FOLDERS(R.string.tab_folders),
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun HomeScreen(
    libraryViewModel: LibraryViewModel,
    playerViewModel: PlayerViewModel,
    collectionsViewModel: CollectionsViewModel,
    navigator: AppNavigator,
) {
    val libraryState by libraryViewModel.state.collectAsStateWithLifecycle()
    // Yalnızca çalan şarkı değişince güncellenir (konum/oynat-duraklat listeyi etkilemez).
    val currentSongId by remember(playerViewModel) {
        playerViewModel.state.map { it.songId }.distinctUntilChanged()
    }.collectAsStateWithLifecycle(initialValue = null)

    val pagerState = rememberPagerState { HomeTab.entries.size }
    val scope = rememberCoroutineScope()

    Scaffold(
        contentWindowInsets = WindowInsets(0),
        topBar = {
            TopAppBar(
                title = { AppTitle() },
                actions = {
                    IconButton(onClick = navigator::openSearch) {
                        Icon(Icons.Rounded.Search, contentDescription = stringResource(R.string.search))
                    }
                    IconButton(onClick = navigator::openSettings) {
                        Icon(Icons.Rounded.Settings, contentDescription = stringResource(R.string.settings))
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(containerColor = MaterialTheme.colorScheme.background),
            )
        },
    ) { padding ->
        Column(Modifier.padding(padding).fillMaxSize()) {
            PrimaryScrollableTabRow(
                selectedTabIndex = pagerState.currentPage,
                edgePadding = 8.dp,
                containerColor = Color.Transparent,
                divider = {},
            ) {
                HomeTab.entries.forEachIndexed { index, tab ->
                    Tab(
                        selected = pagerState.currentPage == index,
                        onClick = { scope.launch { pagerState.animateScrollToPage(index) } },
                        text = { Text(stringResource(tab.title)) },
                        unselectedContentColor = MaterialTheme.colorScheme.onSurfaceVariant,
                    )
                }
            }

            HorizontalPager(
                state = pagerState,
                modifier = Modifier.fillMaxSize(),
                key = { it },
            ) { page ->
                when (val tab = HomeTab.entries[page]) {
                    HomeTab.PLAYLISTS -> PlaylistsTab(
                        collectionsViewModel = collectionsViewModel,
                        library = (libraryState as? LibraryState.Ready)?.library,
                        navigator = navigator,
                    )

                    else -> LibraryContent(libraryState, onRetry = libraryViewModel::refresh) { library ->
                        when (tab) {
                            HomeTab.SONGS -> SongsTab(
                                songs = library.songs,
                                currentSongId = currentSongId,
                                onPlay = { index -> playerViewModel.play(library.songs, index) },
                                onShuffle = { playerViewModel.shuffleAll(library.songs) },
                            )

                            HomeTab.ALBUMS -> AlbumsTab(library.albums, onOpen = navigator::openAlbum)
                            HomeTab.ARTISTS -> ArtistsTab(library.artists, onOpen = navigator::openArtist)
                            HomeTab.FOLDERS -> FoldersTab(library.folders, onOpen = navigator::openFolder)
                            HomeTab.PLAYLISTS -> Unit
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun LibraryContent(
    state: LibraryState,
    onRetry: () -> Unit,
    content: @Composable (Library) -> Unit,
) {
    when (state) {
        LibraryState.Idle, LibraryState.Loading -> LoadingState(stringResource(R.string.library_loading))

        is LibraryState.Error -> MessageState(
            icon = Icons.Rounded.ErrorOutline,
            title = stringResource(R.string.library_error_title),
            body = stringResource(
                when (state.reason) {
                    LibraryState.Error.Reason.PERMISSION -> R.string.library_error_permission
                    LibraryState.Error.Reason.UNKNOWN -> R.string.library_error_generic
                },
            ),
            actionLabel = stringResource(R.string.retry),
            onAction = onRetry,
        )

        is LibraryState.Ready -> if (state.library.songs.isEmpty()) {
            MessageState(
                icon = Icons.Rounded.MusicOff,
                title = stringResource(R.string.library_empty_title),
                body = stringResource(R.string.library_empty_body),
                actionLabel = stringResource(R.string.refresh),
                onAction = onRetry,
            )
        } else {
            content(state.library)
        }
    }
}
