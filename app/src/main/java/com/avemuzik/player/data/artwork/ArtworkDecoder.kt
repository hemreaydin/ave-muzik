package com.avemuzik.player.data.artwork

import android.content.ContentResolver
import android.content.Context
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.media.MediaMetadataRetriever
import android.net.Uri
import android.util.Size

/**
 * Şarkının kapağını yalnızca cihaz içinden okur:
 * 1. MediaStore küçük resmi (gömülü kapak veya klasördeki cover.jpg; sistem tarafından önbelleklenir)
 * 2. Büyük boyutlarda, gerekirse dosyadaki gömülü kapak (MediaMetadataRetriever)
 * İnternetten hiçbir zaman kapak indirilmez.
 */
object ArtworkDecoder {

    fun decode(context: Context, songUri: Uri, sizePx: Int, allowEmbeddedFallback: Boolean): Bitmap? {
        val resolver = context.contentResolver
        val thumbnail = loadThumbnail(resolver, songUri, sizePx)
        if (thumbnail != null) return thumbnail
        return if (allowEmbeddedFallback) decodeEmbedded(context, songUri, sizePx) else null
    }

    private fun loadThumbnail(resolver: ContentResolver, uri: Uri, sizePx: Int): Bitmap? =
        try {
            resolver.loadThumbnail(uri, Size(sizePx, sizePx), null).toSoftware()
        } catch (e: Exception) {
            // Kapak yok, dosya silinmiş veya metadata bozuk: sessizce yer tutucuya düş.
            null
        }

    private fun decodeEmbedded(context: Context, uri: Uri, sizePx: Int): Bitmap? =
        try {
            MediaMetadataRetriever().use { retriever ->
                retriever.setDataSource(context, uri)
                retriever.embeddedPicture?.let { decodeSampled(it, sizePx) }
            }
        } catch (e: Exception) {
            null
        }

    /** Gömülü kapak baytlarını hedef boyuta yakın küçültülmüş olarak çözer (bellek dostu). */
    fun decodeSampled(bytes: ByteArray, sizePx: Int): Bitmap? {
        val bounds = BitmapFactory.Options().apply { inJustDecodeBounds = true }
        BitmapFactory.decodeByteArray(bytes, 0, bytes.size, bounds)
        val options = BitmapFactory.Options().apply {
            inSampleSize = sampleSize(bounds.outWidth, bounds.outHeight, sizePx)
        }
        return BitmapFactory.decodeByteArray(bytes, 0, bytes.size, options)
    }

    private fun sampleSize(width: Int, height: Int, target: Int): Int {
        var sample = 1
        if (width <= 0 || height <= 0) return sample
        while (width / (sample * 2) >= target && height / (sample * 2) >= target) sample *= 2
        return sample
    }

    /** Bildirim ve renk çıkarımı için donanım (HARDWARE) bitmap'leri yazılım bitmap'ine çevirir. */
    private fun Bitmap.toSoftware(): Bitmap =
        if (config == Bitmap.Config.HARDWARE) copy(Bitmap.Config.ARGB_8888, false) ?: this else this
}
