package com.avemuzik.player.player

import android.content.Context
import android.graphics.Bitmap
import android.net.Uri
import androidx.annotation.OptIn
import androidx.media3.common.util.BitmapLoader
import androidx.media3.common.util.UnstableApi
import com.avemuzik.player.data.artwork.ArtworkDecoder
import com.google.common.util.concurrent.ListenableFuture
import com.google.common.util.concurrent.ListeningExecutorService
import com.google.common.util.concurrent.MoreExecutors
import java.io.IOException
import java.util.concurrent.Executors

/**
 * Bildirim, kilit ekranı ve Bluetooth (AVRCP) için kapak yükleyici.
 * Kapaklar yalnızca yerel dosyadan/MediaStore'dan okunur; ağ kullanılmaz.
 */
@OptIn(UnstableApi::class)
class ArtworkBitmapLoader(context: Context) : BitmapLoader {

    private val appContext = context.applicationContext
    private val executor: ListeningExecutorService =
        MoreExecutors.listeningDecorator(Executors.newSingleThreadExecutor())

    override fun supportsMimeType(mimeType: String): Boolean = mimeType.startsWith("image/")

    /**
     * ExoPlayer dosyadaki gömülü kapağı tam çözünürlükte verir (ör. 3000×3000 ≈ 36 MB bitmap);
     * bildirim için küçültülerek çözülür.
     */
    override fun decodeBitmap(data: ByteArray): ListenableFuture<Bitmap> = executor.submit<Bitmap> {
        ArtworkDecoder.decodeSampled(data, NOTIFICATION_ART_SIZE) ?: throw IOException("Kapak çözülemedi")
    }

    override fun loadBitmap(uri: Uri): ListenableFuture<Bitmap> = executor.submit<Bitmap> {
        ArtworkDecoder.decode(appContext, uri, NOTIFICATION_ART_SIZE, allowEmbeddedFallback = true)
            ?: throw IOException("Kapak yok")
    }

    fun release() {
        executor.shutdown()
    }

    private companion object {
        const val NOTIFICATION_ART_SIZE = 512
    }
}
