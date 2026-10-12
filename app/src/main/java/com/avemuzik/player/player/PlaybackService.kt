package com.avemuzik.player.player

import android.app.PendingIntent
import android.content.Intent
import androidx.annotation.OptIn
import androidx.media3.common.AudioAttributes
import androidx.media3.common.C
import androidx.media3.common.MediaItem
import androidx.media3.common.PlaybackException
import androidx.media3.common.Player
import androidx.media3.common.util.UnstableApi
import androidx.media3.exoplayer.ExoPlayer
import androidx.media3.session.CacheBitmapLoader
import androidx.media3.session.DefaultMediaNotificationProvider
import androidx.media3.session.MediaSession
import androidx.media3.session.MediaSessionService
import com.avemuzik.player.MainActivity
import com.avemuzik.player.R
import com.google.common.util.concurrent.Futures
import com.google.common.util.concurrent.ListenableFuture

/**
 * Arka planda çalmanın kalbi. Oynatıcı Activity'ye değil bu servise bağlıdır;
 * uygulama arka plana alındığında, ekran kapandığında veya telefon kilitlendiğinde çalma sürer.
 *
 * MediaSession sayesinde şunlar otomatik çalışır:
 * - Medya bildirimi (önceki / oynat-duraklat / sonraki, kapak, başlık, sanatçı)
 * - Kilit ekranı ve sistem medya kontrolleri
 * - Bluetooth / kablolu kulaklık tuşları
 * - Ses odağı (arama gelince duraklatma) ve kulaklık çıkınca duraklatma
 */
@OptIn(UnstableApi::class)
class PlaybackService : MediaSessionService() {

    private var mediaSession: MediaSession? = null
    private var bitmapLoader: ArtworkBitmapLoader? = null

    override fun onCreate() {
        super.onCreate()

        val exoPlayer = ExoPlayer.Builder(this)
            .setAudioAttributes(
                AudioAttributes.Builder()
                    .setUsage(C.USAGE_MEDIA)
                    .setContentType(C.AUDIO_CONTENT_TYPE_MUSIC)
                    .build(),
                /* handleAudioFocus = */ true,
            )
            .setHandleAudioBecomingNoisy(true)
            .setWakeMode(C.WAKE_MODE_LOCAL)
            .build()
        exoPlayer.setShuffleOrder(QueueShuffleOrder(0))
        exoPlayer.addListener(ErrorRecoveryListener(exoPlayer))

        val sessionActivity = PendingIntent.getActivity(
            this,
            0,
            Intent(this, MainActivity::class.java).addFlags(Intent.FLAG_ACTIVITY_SINGLE_TOP),
            PendingIntent.FLAG_IMMUTABLE or PendingIntent.FLAG_UPDATE_CURRENT,
        )

        val loader = ArtworkBitmapLoader(this).also { bitmapLoader = it }
        mediaSession = MediaSession.Builder(this, QueuePlayer(exoPlayer))
            .setSessionActivity(sessionActivity)
            .setBitmapLoader(CacheBitmapLoader(loader))
            .setCallback(SessionCallback())
            .build()

        setMediaNotificationProvider(
            DefaultMediaNotificationProvider.Builder(this)
                .setChannelName(R.string.notification_channel_name)
                .build()
                .apply { setSmallIcon(R.drawable.ic_notification) },
        )
    }

    override fun onGetSession(controllerInfo: MediaSession.ControllerInfo): MediaSession? = mediaSession

    /** Uygulama son kullanılanlardan kaydırılınca: çalmıyorsa servisi kapat, çalıyorsa devam et. */
    override fun onTaskRemoved(rootIntent: Intent?) {
        val player = mediaSession?.player
        if (player == null ||
            !player.playWhenReady ||
            player.mediaItemCount == 0 ||
            player.playbackState == Player.STATE_ENDED
        ) {
            stopSelf()
        }
    }

    override fun onDestroy() {
        mediaSession?.run {
            player.release()
            release()
        }
        mediaSession = null
        bitmapLoader?.release()
        bitmapLoader = null
        super.onDestroy()
    }

    private class SessionCallback : MediaSession.Callback {
        override fun onAddMediaItems(
            mediaSession: MediaSession,
            controller: MediaSession.ControllerInfo,
            mediaItems: MutableList<MediaItem>,
        ): ListenableFuture<MutableList<MediaItem>> =
            Futures.immediateFuture(mediaItems.map { it.withPlaybackUri() }.toMutableList())
    }

    /**
     * Silinmiş dosya veya desteklenmeyen format çalmayı durdurmasın: sıradaki şarkıya geç.
     * Kuyruktaki her şarkı hatalıysa sonsuz döngüye girmemek için hata sayısı sınırlanır.
     */
    private class ErrorRecoveryListener(private val player: ExoPlayer) : Player.Listener {
        private var consecutiveErrors = 0

        override fun onPlayerError(error: PlaybackException) {
            consecutiveErrors++
            if (consecutiveErrors < player.mediaItemCount && player.hasNextMediaItem()) {
                player.seekToNextMediaItem()
                player.prepare()
                player.play()
            } else {
                consecutiveErrors = 0
            }
        }

        override fun onPlaybackStateChanged(playbackState: Int) {
            if (playbackState == Player.STATE_READY) consecutiveErrors = 0
        }
    }
}
