package com.avemuzik.player.navigation

import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.slideInVertically
import androidx.compose.animation.slideOutVertically
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.navigation.NavHostController
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.toRoute
import com.avemuzik.player.ui.collection.CollectionScreen
import com.avemuzik.player.ui.collection.CollectionSource
import com.avemuzik.player.ui.home.HomeScreen
import com.avemuzik.player.ui.nowplaying.NowPlayingScreen
import com.avemuzik.player.ui.search.SearchScreen
import com.avemuzik.player.ui.settings.SettingsScreen
import com.avemuzik.player.ui.viewmodel.CollectionsViewModel
import com.avemuzik.player.ui.viewmodel.LibraryViewModel
import com.avemuzik.player.ui.viewmodel.PlayerViewModel

@Composable
fun AveNavHost(
    navController: NavHostController,
    navigator: AppNavigator,
    libraryViewModel: LibraryViewModel,
    playerViewModel: PlayerViewModel,
    collectionsViewModel: CollectionsViewModel,
    modifier: Modifier = Modifier,
) {
    NavHost(
        navController = navController,
        startDestination = HomeRoute,
        modifier = modifier,
        enterTransition = { fadeIn(tween(FADE_MS)) },
        exitTransition = { fadeOut(tween(FADE_MS)) },
        popEnterTransition = { fadeIn(tween(FADE_MS)) },
        popExitTransition = { fadeOut(tween(FADE_MS)) },
    ) {
        composable<HomeRoute> {
            HomeScreen(libraryViewModel, playerViewModel, collectionsViewModel, navigator)
        }
        composable<SearchRoute> {
            SearchScreen(libraryViewModel, playerViewModel, navigator)
        }
        composable<SettingsRoute> {
            SettingsScreen(libraryViewModel, navigator)
        }
        composable<FavoritesRoute> {
            CollectionScreen(CollectionSource.Favorites, playerViewModel, collectionsViewModel, navigator)
        }
        composable<AlbumRoute> { entry ->
            CollectionScreen(CollectionSource.Album(entry.toRoute<AlbumRoute>().albumId), playerViewModel, collectionsViewModel, navigator)
        }
        composable<ArtistRoute> { entry ->
            CollectionScreen(CollectionSource.Artist(entry.toRoute<ArtistRoute>().artistId), playerViewModel, collectionsViewModel, navigator)
        }
        composable<FolderRoute> { entry ->
            CollectionScreen(CollectionSource.Folder(entry.toRoute<FolderRoute>().path), playerViewModel, collectionsViewModel, navigator)
        }
        composable<PlaylistRoute> { entry ->
            CollectionScreen(CollectionSource.Playlist(entry.toRoute<PlaylistRoute>().playlistId), playerViewModel, collectionsViewModel, navigator)
        }
        composable<NowPlayingRoute>(
            enterTransition = { slideInVertically(tween(SLIDE_MS)) { it } },
            exitTransition = { fadeOut(tween(FADE_MS)) },
            popEnterTransition = { fadeIn(tween(FADE_MS)) },
            popExitTransition = { slideOutVertically(tween(SLIDE_MS)) { it } },
        ) {
            NowPlayingScreen(playerViewModel, collectionsViewModel, navigator)
        }
    }
}

private const val FADE_MS = 160
private const val SLIDE_MS = 260
