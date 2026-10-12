package com.avemuzik.player.ui.viewmodel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import androidx.lifecycle.viewmodel.initializer
import androidx.lifecycle.viewmodel.viewModelFactory
import com.avemuzik.player.AppContainer
import com.avemuzik.player.model.Song
import com.avemuzik.player.player.PlaybackError
import com.avemuzik.player.player.PlaybackState
import com.avemuzik.player.player.PlayerController
import com.avemuzik.player.player.QueueEntry
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.SharedFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.distinctUntilChanged
import kotlinx.coroutines.flow.flatMapLatest
import kotlinx.coroutines.flow.flow
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.stateIn

class PlayerViewModel(private val controller: PlayerController) : ViewModel() {

    val state: StateFlow<PlaybackState> = controller.state
    val errors: SharedFlow<PlaybackError> = controller.errors

    /**
     * Çalma konumu. Yalnızca dinleyen bir ekran varken ve şarkı çalarken yarım saniyede bir
     * güncellenir; duraklatıldığında hiç iş yapmaz.
     */
    @OptIn(ExperimentalCoroutinesApi::class)
    val position: StateFlow<Long> = controller.state
        .map { Triple(it.isPlaying, it.mediaId, it.positionVersion) }
        .distinctUntilChanged()
        .flatMapLatest { (isPlaying, _, _) ->
            flow {
                emit(controller.currentPosition())
                while (isPlaying) {
                    delay(POSITION_INTERVAL_MS)
                    emit(controller.currentPosition())
                }
            }
        }
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(1_000), 0L)

    fun play(songs: List<Song>, startIndex: Int) = controller.playSongs(songs, startIndex)

    fun playAll(songs: List<Song>) = controller.playSongs(songs, 0, shuffle = false)

    fun shuffleAll(songs: List<Song>) {
        if (songs.isEmpty()) return
        controller.playSongs(songs, songs.indices.random(), shuffle = true)
    }

    fun togglePlayPause() = controller.togglePlayPause()
    fun next() = controller.next()
    fun previous() = controller.previous()
    fun seekTo(positionMs: Long) = controller.seekTo(positionMs)
    fun toggleShuffle() = controller.toggleShuffle()
    fun cycleRepeatMode() = controller.cycleRepeatMode()
    fun playQueueItem(index: Int) = controller.playQueueItem(index)
    fun removeQueueItem(index: Int) = controller.removeQueueItem(index)
    fun playNext(song: Song) = controller.playNext(listOf(song))
    fun addToQueue(song: Song) = controller.addToQueue(listOf(song))
    fun queue(): List<QueueEntry> = controller.queue()

    companion object {
        private const val POSITION_INTERVAL_MS = 500L

        fun factory(container: AppContainer) = viewModelFactory {
            initializer { PlayerViewModel(container.playerController) }
        }
    }
}
