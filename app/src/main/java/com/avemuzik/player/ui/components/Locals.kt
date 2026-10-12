package com.avemuzik.player.ui.components

import androidx.compose.runtime.staticCompositionLocalOf
import com.avemuzik.player.AppContainer
import com.avemuzik.player.data.artwork.ArtworkLoader

/** Kullanıcıya kısa bilgi mesajı (snackbar) göstermek için. */
fun interface Messenger {
    fun show(message: String)
}

val LocalAppContainer = staticCompositionLocalOf<AppContainer> { error("AppContainer sağlanmadı") }
val LocalArtworkLoader = staticCompositionLocalOf<ArtworkLoader> { error("ArtworkLoader sağlanmadı") }
val LocalMessenger = staticCompositionLocalOf { Messenger { } }
val LocalSongActions = staticCompositionLocalOf<SongActionsState> { error("SongActionsState sağlanmadı") }
