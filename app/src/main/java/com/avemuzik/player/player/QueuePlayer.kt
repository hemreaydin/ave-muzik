package com.avemuzik.player.player

import androidx.annotation.OptIn
import androidx.media3.common.ForwardingPlayer
import androidx.media3.common.MediaItem
import androidx.media3.common.util.UnstableApi
import androidx.media3.exoplayer.ExoPlayer

/**
 * MediaSession'a verilen oynatıcı. Karışık mod açıldığında veya yeni kuyruk kurulduğunda,
 * karışık sıranın o an çalan / seçilen şarkıyla başlamasını sağlar. Bildirim, kilit ekranı ve
 * Bluetooth'tan gelen komutlar da bu sınıftan geçtiği için davranış her yerde aynıdır.
 */
@OptIn(UnstableApi::class)
class QueuePlayer(private val exoPlayer: ExoPlayer) : ForwardingPlayer(exoPlayer) {

    override fun setShuffleModeEnabled(shuffleModeEnabled: Boolean) {
        if (shuffleModeEnabled && !exoPlayer.shuffleModeEnabled) {
            reshuffleFrom(exoPlayer.currentMediaItemIndex)
        }
        super.setShuffleModeEnabled(shuffleModeEnabled)
    }

    override fun setMediaItems(mediaItems: MutableList<MediaItem>, startIndex: Int, startPositionMs: Long) {
        super.setMediaItems(mediaItems, startIndex, startPositionMs)
        if (exoPlayer.shuffleModeEnabled) reshuffleFrom(startIndex)
    }

    override fun setMediaItems(mediaItems: MutableList<MediaItem>, resetPosition: Boolean) {
        super.setMediaItems(mediaItems, resetPosition)
        if (exoPlayer.shuffleModeEnabled) reshuffleFrom(exoPlayer.currentMediaItemIndex)
    }

    private fun reshuffleFrom(firstIndex: Int) {
        val count = exoPlayer.mediaItemCount
        if (count == 0) return
        exoPlayer.setShuffleOrder(QueueShuffleOrder.startingWith(count, firstIndex.coerceIn(0, count - 1)))
    }
}
