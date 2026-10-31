package it.studiosmus.carte.audio

import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test
import kotlin.random.Random

class CardCuePickerTest {
    @Test fun neverRepeatsAnAdjacentTake() {
        val rng = Random(701)
        var previous = -1
        val encountered = mutableSetOf<Int>()
        repeat(1500) {
            val current = CardCuePicker.next(previous, 3, rng)
            assertTrue(current in 0..2)
            if (previous >= 0) assertTrue(current != previous)
            encountered.add(current)
            previous = current
        }
        assertEquals(setOf(0, 1, 2), encountered)
    }

    @Test fun oneAvailableRecordingNeverCrashes() {
        repeat(20) { assertEquals(0, CardCuePicker.next(it, 1, Random(1))) }
    }

    @Test(expected = IllegalArgumentException::class)
    fun noRecordingsIsRejected() {
        CardCuePicker.next(-1, 0, Random(1))
    }
}
