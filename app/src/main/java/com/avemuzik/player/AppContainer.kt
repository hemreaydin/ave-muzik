package com.avemuzik.player

import android.content.Context
import com.avemuzik.player.data.artwork.ArtworkLoader
import com.avemuzik.player.data.database.AveDatabase
import com.avemuzik.player.data.repository.FavoritesRepository
import com.avemuzik.player.data.repository.MusicRepository
import com.avemuzik.player.data.repository.PlaylistRepository
import com.avemuzik.player.data.repository.SettingsRepository
import com.avemuzik.player.player.PlayerController
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob

/**
 * Basit, elle yazılmış bağımlılık kabı. Her şey ilk kullanımda (lazy) oluşturulur;
 * böylece uygulama açılışında gereksiz iş yapılmaz.
 */
class AppContainer(context: Context) {
    private val appContext = context.applicationContext
    private val appScope = CoroutineScope(SupervisorJob() + Dispatchers.Default)
    private val database by lazy { AveDatabase.build(appContext) }

    val musicRepository by lazy { MusicRepository(appContext, appScope) }
    val favoritesRepository by lazy { FavoritesRepository(database.favoriteDao()) }
    val playlistRepository by lazy { PlaylistRepository(database.playlistDao()) }
    val settingsRepository by lazy { SettingsRepository(appContext, appScope) }
    val artworkLoader by lazy { ArtworkLoader(appContext) }
    val playerController by lazy { PlayerController(appContext) }
}
