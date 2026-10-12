package com.avemuzik.player.player

import androidx.annotation.StringRes
import androidx.compose.runtime.Immutable
import androidx.media3.common.PlaybackException
import androidx.media3.common.Player
import com.avemuzik.player.R

/** Arayüzün ihtiyaç duyduğu oynatıcı durumu (konum hariç; konum ayrı ve seyrek akar). */
@Immutable
data class PlaybackState(
    val isConnected: Boolean = false,
    val mediaId: String? = null,
    val title: String = "",
    val artist: String = "",
    val album: String = "",
    val playWhenReady: Boolean = false,
    val playbackState: Int = Player.STATE_IDLE,
    val isPlaying: Boolean = false,
    val durationMs: Long = 0L,
    val shuffleEnabled: Boolean = false,
    val repeatMode: Int = Player.REPEAT_MODE_OFF,
    val queueSize: Int = 0,
    /** Atlama (seek / şarkı değişimi) olduğunda artar; konum bilgisinin hemen tazelenmesi için. */
    val positionVersion: Int = 0,
    /** Kuyruk veya sırası değiştiğinde artar. */
    val queueVersion: Int = 0,
) {
    val hasMedia: Boolean get() = mediaId != null
    val songId: Long? get() = mediaId?.toLongOrNull()

    /** Duraklat ikonu gösterilmeli mi (tamponlama sırasında ikon titremez). */
    val showPause: Boolean
        get() = playWhenReady && playbackState != Player.STATE_ENDED && playbackState != Player.STATE_IDLE
}

@Immutable
data class QueueEntry(
    /** Kuyruktaki gerçek konum (seek için). */
    val index: Int,
    val songId: Long?,
    val title: String,
    val artist: String,
)

enum class PlaybackError(@param:StringRes val messageRes: Int) {
    FILE_NOT_FOUND(R.string.error_file_not_found),
    UNSUPPORTED_FORMAT(R.string.error_unsupported),
    GENERIC(R.string.error_generic);

    companion object {
        fun from(error: PlaybackException): PlaybackError = when (error.errorCode) {
            PlaybackException.ERROR_CODE_IO_FILE_NOT_FOUND,
            PlaybackException.ERROR_CODE_IO_NO_PERMISSION,
            PlaybackException.ERROR_CODE_IO_READ_POSITION_OUT_OF_RANGE,
            -> FILE_NOT_FOUND

            PlaybackException.ERROR_CODE_PARSING_CONTAINER_MALFORMED,
            PlaybackException.ERROR_CODE_PARSING_CONTAINER_UNSUPPORTED,
            PlaybackException.ERROR_CODE_PARSING_MANIFEST_MALFORMED,
            PlaybackException.ERROR_CODE_DECODER_INIT_FAILED,
            PlaybackException.ERROR_CODE_DECODER_QUERY_FAILED,
            PlaybackException.ERROR_CODE_DECODING_FAILED,
            PlaybackException.ERROR_CODE_DECODING_FORMAT_EXCEEDS_CAPABILITIES,
            PlaybackException.ERROR_CODE_DECODING_FORMAT_UNSUPPORTED,
            PlaybackException.ERROR_CODE_AUDIO_TRACK_INIT_FAILED,
            -> UNSUPPORTED_FORMAT

            else -> GENERIC
        }
    }
}
