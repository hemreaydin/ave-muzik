package com.avemuzik.player.player

import android.content.ComponentName
import android.content.Context
import androidx.core.content.ContextCompat
import androidx.lifecycle.DefaultLifecycleObserver
import androidx.lifecycle.LifecycleOwner
import androidx.media3.common.C
import androidx.media3.common.PlaybackException
import androidx.media3.common.Player
import androidx.media3.session.MediaController
import androidx.media3.session.SessionToken
import com.avemuzik.player.model.Song
import com.google.common.util.concurrent.ListenableFuture
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharedFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asSharedFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update

/**
 * Arayüz ile [PlaybackService] arasındaki köprü. Oynatıcının kendisi serviste yaşar;
 * burada yalnızca bir MediaController tutulur. Activity görünürken bağlanır (onStart),
 * görünmez olunca bağlantı bırakılır (onStop) — çalma serviste kesintisiz devam eder.
 */
class PlayerController(private val context: Context) : DefaultLifecycleObserver {

    private val mainExecutor = ContextCompat.getMainExecutor(context)
    private var controllerFuture: ListenableFuture<MediaController>? = null
    private var controller: MediaController? = null

    private val _state = MutableStateFlow(PlaybackState())
    val state: StateFlow<PlaybackState> = _state.asStateFlow()

    private val _errors = MutableSharedFlow<PlaybackError>(extraBufferCapacity = 4)
    val errors: SharedFlow<PlaybackError> = _errors.asSharedFlow()

    private val listener = object : Player.Listener {
        override fun onEvents(player: Player, events: Player.Events) {
            publish(
                player = player,
                positionJump = events.containsAny(
                    Player.EVENT_POSITION_DISCONTINUITY,
                    Player.EVENT_MEDIA_ITEM_TRANSITION,
                ),
                queueChanged = events.containsAny(
                    Player.EVENT_TIMELINE_CHANGED,
                    Player.EVENT_MEDIA_ITEM_TRANSITION,
                    Player.EVENT_SHUFFLE_MODE_ENABLED_CHANGED,
                ),
            )
        }

        override fun onPlayerError(error: PlaybackException) {
            _errors.tryEmit(PlaybackError.from(error))
        }
    }

    override fun onStart(owner: LifecycleOwner) = connect()

    override fun onStop(owner: LifecycleOwner) = disconnect()

    fun connect() {
        if (controllerFuture != null) return
        val token = SessionToken(context, ComponentName(context, PlaybackService::class.java))
        val future = MediaController.Builder(context, token).buildAsync()
        controllerFuture = future
        future.addListener({
            if (controllerFuture !== future) return@addListener
            val connected = runCatching { future.get() }.getOrNull()
            if (connected == null) {
                controllerFuture = null
                return@addListener
            }
            controller = connected
            connected.addListener(listener)
            publish(connected, positionJump = true, queueChanged = true)
        }, mainExecutor)
    }

    fun disconnect() {
        controller?.removeListener(listener)
        controller = null
        controllerFuture?.let { MediaController.releaseFuture(it) }
        controllerFuture = null
        _state.update { it.copy(isConnected = false) }
    }

    // region Komutlar

    fun playSongs(songs: List<Song>, startIndex: Int = 0, shuffle: Boolean? = null) {
        if (songs.isEmpty()) return
        val items = songs.map { it.toMediaItem() }
        withController { player ->
            // Karışık mod, kuyruk kurulmadan önce ayarlanır; böylece seçilen şarkıdan başlar.
            if (shuffle != null) player.shuffleModeEnabled = shuffle
            player.setMediaItems(items, startIndex.coerceIn(0, items.lastIndex), 0L)
            player.prepare()
            player.play()
        }
    }

    fun togglePlayPause() = withController { player ->
        if (player.playWhenReady && player.playbackState != Player.STATE_ENDED) {
            player.pause()
        } else {
            when (player.playbackState) {
                Player.STATE_IDLE -> player.prepare()
                Player.STATE_ENDED -> player.seekToDefaultPosition(player.currentMediaItemIndex)
                Player.STATE_BUFFERING, Player.STATE_READY -> Unit
            }
            player.play()
        }
    }

    fun next() = withController { it.seekToNext() }

    fun previous() = withController { it.seekToPrevious() }

    fun seekTo(positionMs: Long) = withController { it.seekTo(positionMs) }

    fun toggleShuffle() = withController { it.shuffleModeEnabled = !it.shuffleModeEnabled }

    /** Kapalı → Tümünü tekrarla → Bu şarkıyı tekrarla → Kapalı */
    fun cycleRepeatMode() = withController { player ->
        player.repeatMode = when (player.repeatMode) {
            Player.REPEAT_MODE_OFF -> Player.REPEAT_MODE_ALL
            Player.REPEAT_MODE_ALL -> Player.REPEAT_MODE_ONE
            else -> Player.REPEAT_MODE_OFF
        }
    }

    fun playQueueItem(index: Int) = withController { player ->
        if (index !in 0 until player.mediaItemCount) return@withController
        player.seekTo(index, 0L)
        if (player.playbackState == Player.STATE_IDLE) player.prepare()
        player.play()
    }

    fun removeQueueItem(index: Int) = withController { player ->
        if (index in 0 until player.mediaItemCount) player.removeMediaItem(index)
    }

    fun playNext(songs: List<Song>) = withController { player ->
        if (player.mediaItemCount == 0) {
            playSongs(songs)
        } else {
            player.addMediaItems(player.currentMediaItemIndex + 1, songs.map { it.toMediaItem() })
        }
    }

    fun addToQueue(songs: List<Song>) = withController { player ->
        if (player.mediaItemCount == 0) playSongs(songs) else player.addMediaItems(songs.map { it.toMediaItem() })
    }

    // endregion

    fun currentPosition(): Long = controller?.currentPosition ?: 0L

    /** Şu an çalan şarkıdan başlayarak çalınacak sırada kuyruk (karışık mod dikkate alınır). */
    fun queue(): List<QueueEntry> {
        val player = controller ?: return emptyList()
        val timeline = player.currentTimeline
        if (timeline.isEmpty) return emptyList()
        val shuffle = player.shuffleModeEnabled
        val result = ArrayList<QueueEntry>(timeline.windowCount)
        var index = player.currentMediaItemIndex
        while (index != C.INDEX_UNSET && result.size < timeline.windowCount) {
            val item = player.getMediaItemAt(index)
            result += QueueEntry(
                index = index,
                songId = item.mediaId.toLongOrNull(),
                title = item.mediaMetadata.title?.toString().orEmpty(),
                artist = item.mediaMetadata.artist?.toString().orEmpty(),
            )
            index = timeline.getNextWindowIndex(index, Player.REPEAT_MODE_OFF, shuffle)
        }
        return result
    }

    private fun withController(block: (MediaController) -> Unit) {
        controller?.let { return block(it) }
        connect()
        val future = controllerFuture ?: return
        future.addListener({ controller?.let(block) }, mainExecutor)
    }

    private fun publish(player: Player, positionJump: Boolean, queueChanged: Boolean) {
        val item = player.currentMediaItem
        val metadata = player.mediaMetadata
        _state.update { old ->
            PlaybackState(
                isConnected = true,
                mediaId = item?.mediaId,
                title = metadata.title?.toString().orEmpty(),
                artist = metadata.artist?.toString().orEmpty(),
                album = metadata.albumTitle?.toString().orEmpty(),
                playWhenReady = player.playWhenReady,
                playbackState = player.playbackState,
                isPlaying = player.isPlaying,
                durationMs = player.duration.takeIf { it != C.TIME_UNSET && it > 0 } ?: 0L,
                shuffleEnabled = player.shuffleModeEnabled,
                repeatMode = player.repeatMode,
                queueSize = player.mediaItemCount,
                positionVersion = if (positionJump) old.positionVersion + 1 else old.positionVersion,
                queueVersion = if (queueChanged) old.queueVersion + 1 else old.queueVersion,
            )
        }
    }
}
