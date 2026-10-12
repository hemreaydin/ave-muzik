package com.avemuzik.player.data.artwork

import android.content.Context
import android.util.LruCache
import androidx.compose.ui.graphics.ImageBitmap
import androidx.compose.ui.graphics.asAndroidBitmap
import androidx.compose.ui.graphics.asImageBitmap
import androidx.core.graphics.get
import androidx.core.graphics.scale
import com.avemuzik.player.model.Song
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.util.concurrent.ConcurrentHashMap

/**
 * Arayüz için kapak yükleyici.
 * - Bellek sınırlı LRU önbellek (uygulama belleğinin 1/6'sı)
 * - Boyutlar birkaç kovaya yuvarlanır; aynı kapak her boyut için tekrar decode edilmez
 * - Kapağı olmayan şarkılar hatırlanır, tekrar tekrar denenmez
 * - Decode işlemleri ana thread dışında, sınırlı paralellikte yapılır
 */
class ArtworkLoader(private val context: Context) {

    private val cache = object : LruCache<String, ImageBitmap>(cacheSizeBytes()) {
        override fun sizeOf(key: String, value: ImageBitmap): Int = value.width * value.height * 4
    }
    private val missing: MutableSet<String> = ConcurrentHashMap.newKeySet()
    private val colors = LruCache<Long, Int>(COLOR_CACHE_SIZE)
    private val decodeDispatcher = Dispatchers.IO.limitedParallelism(DECODE_PARALLELISM)

    /** Önbellekte varsa senkron döner; ilk karede yer tutucu yanıp sönmesini önler. */
    fun peek(songId: Long, sizePx: Int): ImageBitmap? = cache.get(key(songId, bucket(sizePx)))

    suspend fun load(songId: Long, sizePx: Int): ImageBitmap? {
        val bucket = bucket(sizePx)
        val key = key(songId, bucket)
        cache.get(key)?.let { return it }
        if (key in missing) return null
        val image = withContext(decodeDispatcher) {
            cache.get(key) ?: ArtworkDecoder
                .decode(context, Song.uriFor(songId), bucket, allowEmbeddedFallback = bucket >= LARGE)
                ?.asImageBitmap()
                ?.also { cache.put(key, it) }
        }
        if (image == null) missing += key
        return image
    }

    /** Kapağın ortalama rengi; çalma ekranı arka planını albüm kapağına göre renklendirmek için. */
    suspend fun dominantColor(songId: Long): Int? {
        colors.get(songId)?.let { return it }
        val image = load(songId, SMALL) ?: return null
        return withContext(Dispatchers.Default) {
            image.asAndroidBitmap().scale(1, 1)[0, 0].also { colors.put(songId, it) }
        }
    }

    private fun key(songId: Long, bucket: Int) = "$songId:$bucket"

    private fun bucket(sizePx: Int): Int = when {
        sizePx <= SMALL -> SMALL
        sizePx <= MEDIUM -> MEDIUM
        else -> LARGE
    }

    private companion object {
        const val SMALL = 200
        const val MEDIUM = 480
        const val LARGE = 1080
        const val DECODE_PARALLELISM = 3
        const val COLOR_CACHE_SIZE = 128

        fun cacheSizeBytes(): Int = (Runtime.getRuntime().maxMemory() / 6).toInt()
    }
}
