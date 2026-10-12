package com.avemuzik.player.navigation

import androidx.navigation.NavDestination.Companion.hasRoute
import androidx.navigation.NavHostController
import kotlinx.serialization.Serializable

@Serializable
data object HomeRoute

@Serializable
data object SearchRoute

@Serializable
data object SettingsRoute

@Serializable
data object NowPlayingRoute

@Serializable
data object FavoritesRoute

@Serializable
data class AlbumRoute(val albumId: Long)

@Serializable
data class ArtistRoute(val artistId: Long)

@Serializable
data class FolderRoute(val path: String)

@Serializable
data class PlaylistRoute(val playlistId: Long)

/** Ekranların NavController'a doğrudan bağımlı olmaması için ince bir sarmalayıcı. */
class AppNavigator(private val navController: NavHostController) {
    fun openSearch() = navController.navigate(SearchRoute) { launchSingleTop = true }
    fun openSettings() = navController.navigate(SettingsRoute) { launchSingleTop = true }
    fun openNowPlaying() = navController.navigate(NowPlayingRoute) { launchSingleTop = true }
    fun openFavorites() = navController.navigate(FavoritesRoute) { launchSingleTop = true }
    fun openAlbum(albumId: Long) = navController.navigate(AlbumRoute(albumId)) { launchSingleTop = true }
    fun openArtist(artistId: Long) = navController.navigate(ArtistRoute(artistId)) { launchSingleTop = true }
    fun openFolder(path: String) = navController.navigate(FolderRoute(path)) { launchSingleTop = true }
    fun openPlaylist(playlistId: Long) = navController.navigate(PlaylistRoute(playlistId)) { launchSingleTop = true }

    /** Çalma ekranındayken başka bir sayfaya gidilecekse önce çalma ekranı kapatılır. */
    fun leaveNowPlaying() {
        if (navController.currentDestination?.hasRoute<NowPlayingRoute>() == true) back()
    }

    /** Başlangıç ekranı hiçbir zaman yığından çıkarılmaz (hızlı çift dokunuşa karşı). */
    fun back() {
        if (navController.previousBackStackEntry != null) navController.popBackStack()
    }
}
