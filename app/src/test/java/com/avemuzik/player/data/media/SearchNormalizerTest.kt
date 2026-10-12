package com.avemuzik.player.data.media

import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class SearchNormalizerTest {

    @Test
    fun ignoresCaseAndTurkishDiacritics() {
        assertEquals("muzik", SearchNormalizer.normalize("Müzik"))
        assertEquals("istanbul", SearchNormalizer.normalize("İSTANBUL"))
        assertEquals("isik", SearchNormalizer.normalize("Işık"))
        assertEquals("cagri soguk", SearchNormalizer.normalize("Çağrı Soğuk"))
    }

    @Test
    fun tokensMatchRegardlessOfSpelling() {
        val key = SearchNormalizer.normalize("Şımarık Tarkan Ölürüm Sana")
        val tokens = SearchNormalizer.tokens("  tarkan   SIMARIK ")
        assertEquals(listOf("tarkan", "simarik"), tokens)
        assertTrue(tokens.all { key.contains(it) })
    }
}
