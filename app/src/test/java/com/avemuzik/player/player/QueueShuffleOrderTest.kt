package com.avemuzik.player.player

import androidx.media3.common.C
import androidx.media3.exoplayer.source.ShuffleOrder
import org.junit.Assert.assertEquals
import org.junit.Test

class QueueShuffleOrderTest {

    private fun ShuffleOrder.playbackOrder(): List<Int> {
        val order = ArrayList<Int>()
        var index = firstIndex
        while (index != C.INDEX_UNSET) {
            order += index
            index = getNextIndex(index)
        }
        return order
    }

    @Test
    fun startsWithChosenSongAndPlaysEverySongOnce() {
        repeat(50) {
            val order = QueueShuffleOrder.startingWith(length = 20, firstIndex = 7).playbackOrder()
            assertEquals(7, order.first())
            assertEquals((0 until 20).toList(), order.sorted())
        }
    }

    @Test
    fun previousIsInverseOfNext() {
        val shuffle = QueueShuffleOrder.startingWith(10, 0)
        val order = shuffle.playbackOrder()
        for (i in 1 until order.size) assertEquals(order[i - 1], shuffle.getPreviousIndex(order[i]))
        assertEquals(C.INDEX_UNSET, shuffle.getPreviousIndex(order.first()))
        assertEquals(order.last(), shuffle.lastIndex)
    }

    @Test
    fun playNextInsertsRightAfterCurrentSong() {
        val current = 2
        val inserted = QueueShuffleOrder.startingWith(5, current).cloneAndInsert(current + 1, 2)
        val order = inserted.playbackOrder()
        assertEquals(listOf(current, current + 1, current + 2), order.take(3))
        assertEquals((0 until 7).toList(), order.sorted())
    }

    @Test
    fun addToQueueAppendsToEndOfShuffleOrder() {
        val order = QueueShuffleOrder.startingWith(5, 0).cloneAndInsert(5, 2).playbackOrder()
        assertEquals(listOf(5, 6), order.takeLast(2))
        assertEquals(7, order.size)
    }

    @Test
    fun removeKeepsRelativeOrderAndShiftsIndices() {
        val original = QueueShuffleOrder.startingWith(6, 0)
        val before = original.playbackOrder()
        val after = original.cloneAndRemove(1, 3).playbackOrder()
        val expected = before.filter { it !in 1..2 }.map { if (it >= 3) it - 2 else it }
        assertEquals(expected, after)
    }

    @Test
    fun insertIntoEmptyCreatesFullOrder() {
        val order = QueueShuffleOrder(0).cloneAndInsert(0, 8).playbackOrder()
        assertEquals((0 until 8).toList(), order.sorted())
    }

    @Test
    fun clearProducesEmptyOrder() {
        val cleared = QueueShuffleOrder.startingWith(4, 1).cloneAndClear()
        assertEquals(0, cleared.length)
        assertEquals(C.INDEX_UNSET, cleared.firstIndex)
    }
}
