package com.avemuzik.player.ui

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.consumeWindowInsets
import androidx.compose.foundation.layout.navigationBars
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.windowInsetsBottomHeight
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalResources
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.compose.LifecycleEventEffect
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.navigation.NavDestination.Companion.hasRoute
import androidx.navigation.compose.currentBackStackEntryAsState
import androidx.navigation.compose.rememberNavController
import com.avemuzik.player.AppContainer
import com.avemuzik.player.data.repository.LibraryState
import com.avemuzik.player.navigation.AppNavigator
import com.avemuzik.player.navigation.AveNavHost
import com.avemuzik.player.navigation.NowPlayingRoute
import com.avemuzik.player.ui.components.LocalAppContainer
import com.avemuzik.player.ui.components.LocalArtworkLoader
import com.avemuzik.player.ui.components.LocalMessenger
import com.avemuzik.player.ui.components.LocalSongActions
import com.avemuzik.player.ui.components.Messenger
import com.avemuzik.player.ui.components.MiniPlayer
import com.avemuzik.player.ui.components.SongActionsHost
import com.avemuzik.player.ui.components.SongActionsState
import com.avemuzik.player.ui.permission.AudioPermission
import com.avemuzik.player.ui.permission.PermissionScreen
import com.avemuzik.player.ui.viewmodel.CollectionsViewModel
import com.avemuzik.player.ui.viewmodel.LibraryViewModel
import com.avemuzik.player.ui.viewmodel.PlayerViewModel
import kotlinx.coroutines.launch

@Composable
fun AveMuzikRoot(container: AppContainer) {
    val context = LocalContext.current
    var permissionGranted by remember { mutableStateOf(AudioPermission.isGranted(context)) }
    // Kullanıcı izni sistem ayarlarından verip geri dönerse yakala.
    LifecycleEventEffect(Lifecycle.Event.ON_RESUME) {
        permissionGranted = AudioPermission.isGranted(context)
    }

    CompositionLocalProvider(
        LocalAppContainer provides container,
        LocalArtworkLoader provides container.artworkLoader,
    ) {
        if (permissionGranted) {
            MainContent(container)
        } else {
            PermissionScreen(onGranted = { permissionGranted = true })
        }
    }
}

@Composable
private fun MainContent(container: AppContainer) {
    val libraryViewModel: LibraryViewModel = viewModel(factory = LibraryViewModel.factory(container))
    val playerViewModel: PlayerViewModel = viewModel(factory = PlayerViewModel.factory(container))
    val collectionsViewModel: CollectionsViewModel = viewModel(factory = CollectionsViewModel.factory(container))

    LaunchedEffect(Unit) { libraryViewModel.ensureLoaded() }

    val navController = rememberNavController()
    val navigator = remember(navController) { AppNavigator(navController) }
    val backStackEntry by navController.currentBackStackEntryAsState()
    val onNowPlaying = backStackEntry?.destination?.hasRoute<NowPlayingRoute>() == true

    val snackbarHostState = remember { SnackbarHostState() }
    val scope = rememberCoroutineScope()
    val messenger = remember(snackbarHostState) {
        Messenger { message ->
            scope.launch {
                snackbarHostState.currentSnackbarData?.dismiss()
                snackbarHostState.showSnackbar(message)
            }
        }
    }
    val songActions = remember { SongActionsState() }

    val resources = LocalResources.current
    LaunchedEffect(playerViewModel) {
        playerViewModel.errors.collect { error -> messenger.show(resources.getString(error.messageRes)) }
    }

    val playback by playerViewModel.state.collectAsStateWithLifecycle()
    val library by libraryViewModel.state.collectAsStateWithLifecycle()

    CompositionLocalProvider(
        LocalMessenger provides messenger,
        LocalSongActions provides songActions,
    ) {
        Scaffold(
            contentWindowInsets = WindowInsets(0),
            snackbarHost = {
                SnackbarHost(
                    hostState = snackbarHostState,
                    modifier = if (onNowPlaying) Modifier.navigationBarsPadding() else Modifier,
                )
            },
            bottomBar = {
                if (!onNowPlaying) {
                    Column(Modifier.background(MaterialTheme.colorScheme.background)) {
                        if (playback.hasMedia) {
                            val position = playerViewModel.position.collectAsStateWithLifecycle()
                            val fallbackDuration = playback.songId?.let { id ->
                                (library as? LibraryState.Ready)?.library?.songsById?.get(id)?.durationMs
                            } ?: 0L
                            MiniPlayer(
                                state = playback,
                                position = position,
                                durationMs = playback.durationMs.takeIf { it > 0 } ?: fallbackDuration,
                                onOpen = navigator::openNowPlaying,
                                onPlayPause = playerViewModel::togglePlayPause,
                                onNext = playerViewModel::next,
                            )
                        }
                        Spacer(Modifier.windowInsetsBottomHeight(WindowInsets.navigationBars))
                    }
                }
            },
        ) { padding ->
            AveNavHost(
                navController = navController,
                navigator = navigator,
                libraryViewModel = libraryViewModel,
                playerViewModel = playerViewModel,
                collectionsViewModel = collectionsViewModel,
                modifier = Modifier.padding(padding).consumeWindowInsets(padding),
            )
        }

        SongActionsHost(
            state = songActions,
            collectionsViewModel = collectionsViewModel,
            playerViewModel = playerViewModel,
            navigator = navigator,
        )
    }
}
