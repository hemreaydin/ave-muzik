package com.avemuzik.player.data.media

import java.text.Normalizer
import java.util.Locale

/**
 * Aramayı aksan ve büyük/küçük harf duyarsız yapar:
 * "Müzik", "MUZIK" ve "muzik" aynı anahtara dönüşür. Türkçe ı/İ de i olarak eşlenir.
 */
object SearchNormalizer {
    private val combiningMarks = "\\p{Mn}+".toRegex()

    fun normalize(text: String): String =
        Normalizer.normalize(text.lowercase(Locale.ROOT), Normalizer.Form.NFD)
            .replace(combiningMarks, "")
            .replace('ı', 'i')

    fun tokens(query: String): List<String> =
        normalize(query).split(' ').filter { it.isNotBlank() }
}
