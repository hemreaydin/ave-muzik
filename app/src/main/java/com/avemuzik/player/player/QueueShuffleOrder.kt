package com.avemuzik.player.player

import androidx.annotation.OptIn
import androidx.media3.common.C
import androidx.media3.common.util.UnstableApi
import androidx.media3.exoplayer.source.ShuffleOrder
import java.util.Random

/**
 * Ave-Müzik'in karışık çalma sırası.
 *
 * ExoPlayer'ın varsayılan sırasından farkları:
 * - Karışık çalma, seçilen şarkıdan başlar ve kuyruktaki her şarkı bir kez çalınır.
 * - "Sıradaki olarak çal" ile araya eklenen şarkılar, karışık modda da gerçekten sıradaki olur.
 * - Kuyruğun sonuna eklenen şarkılar karışık sıranın da sonuna eklenir.
 */
@OptIn(UnstableApi::class)
class QueueShuffleOrder private constructor(
    private val shuffled: IntArray,
    private val random: Random,
) : ShuffleOrder {

    /** positions[index] = index'in karışık sıradaki konumu */
    private val positions = IntArray(shuffled.size).also { array ->
        shuffled.forEachIndexed { position, index -> array[index] = position }
    }

    constructor(length: Int) : this(randomOrder(length, C.INDEX_UNSET, Random()), Random())

    override fun getLength(): Int = shuffled.size

    override fun getNextIndex(index: Int): Int {
        val next = positions[index] + 1
        return if (next < shuffled.size) shuffled[next] else C.INDEX_UNSET
    }

    override fun getPreviousIndex(index: Int): Int {
        val previous = positions[index] - 1
        return if (previous >= 0) shuffled[previous] else C.INDEX_UNSET
    }

    override fun getLastIndex(): Int = if (shuffled.isEmpty()) C.INDEX_UNSET else shuffled[shuffled.size - 1]

    override fun getFirstIndex(): Int = if (shuffled.isEmpty()) C.INDEX_UNSET else shuffled[0]

    override fun cloneAndInsert(insertionIndex: Int, insertionCount: Int): ShuffleOrder {
        if (shuffled.isEmpty()) {
            return QueueShuffleOrder(randomOrder(insertionCount, C.INDEX_UNSET, random), random)
        }
        val shifted = IntArray(shuffled.size) { position ->
            val index = shuffled[position]
            if (index >= insertionIndex) index + insertionCount else index
        }
        // Eklenen öğeler, eklendikleri yerden hemen önceki öğenin ardına yerleşir.
        val insertAt = when {
            insertionIndex >= shuffled.size -> shuffled.size
            insertionIndex == 0 -> 0
            else -> positions[insertionIndex - 1] + 1
        }
        val result = IntArray(shuffled.size + insertionCount)
        shifted.copyInto(result, destinationOffset = 0, startIndex = 0, endIndex = insertAt)
        for (i in 0 until insertionCount) result[insertAt + i] = insertionIndex + i
        shifted.copyInto(result, destinationOffset = insertAt + insertionCount, startIndex = insertAt)
        return QueueShuffleOrder(result, random)
    }

    override fun cloneAndRemove(indexFrom: Int, indexToExclusive: Int): ShuffleOrder {
        val removedCount = indexToExclusive - indexFrom
        val result = shuffled
            .filter { it < indexFrom || it >= indexToExclusive }
            .map { if (it >= indexToExclusive) it - removedCount else it }
            .toIntArray()
        return QueueShuffleOrder(result, random)
    }

    override fun cloneAndClear(): ShuffleOrder = QueueShuffleOrder(IntArray(0), random)

    companion object {
        /** [firstIndex] karışık sıranın başına alınır; diğerleri rastgele dizilir. */
        fun startingWith(length: Int, firstIndex: Int): QueueShuffleOrder {
            val random = Random()
            return QueueShuffleOrder(randomOrder(length, firstIndex, random), random)
        }

        private fun randomOrder(length: Int, firstIndex: Int, random: Random): IntArray {
            val order = IntArray(length) { it }
            for (i in length - 1 downTo 1) {
                val j = random.nextInt(i + 1)
                val tmp = order[i]; order[i] = order[j]; order[j] = tmp
            }
            if (firstIndex in 0 until length) {
                val position = order.indexOf(firstIndex)
                order[position] = order[0]
                order[0] = firstIndex
            }
            return order
        }
    }
}
