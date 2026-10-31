package it.studiosmus.carte.audio

import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test
import kotlin.random.Random

class AmbienceSettingsTest {
    @Test fun volumesAreClamped() {
        val result = AmbienceSettings(
            fireplace = -1f, jazz = 2f, conversations = 0.3f,
            glassware = 5f, doors = -3f, cards = 0.9f
        ).normalized()
        assertEquals(0f, result.fireplace, 0f)
        assertEquals(1f, result.jazz, 0f)
        assertEquals(0.3f, result.conversations, 0f)
        assertEquals(1f, result.glassware, 0f)
        assertEquals(0f, result.doors, 0f)
        assertEquals(0.9f, result.cards, 0f)
    }

    @Test fun allFourWhispersHaveAudibleGainAtAnEnabledVolume() {
        val mix = AmbienceSettings(conversations = 0.20f)
        assertEquals(0.104f, mix.conversationGain(1f), 0.00001f)
        assertEquals(0f, mix.conversationGain(0f), 0f)
        assertEquals(0f, AmbienceSettings(conversations = 0f).conversationGain(1f), 0f)
    }

    @Test fun allEventDelaysRespectLimits() {
        val scheduler = AmbienceScheduler(Random(42))
        repeat(1000) {
            val glass = scheduler.nextDelayMs(AmbienceScheduler.Event.GLASSWARE)
            val door = scheduler.nextDelayMs(AmbienceScheduler.Event.DOOR)
            val conversation = scheduler.nextDelayMs(AmbienceScheduler.Event.CONVERSATION)
            assertTrue(glass in 25_000L..85_000L)
            assertTrue(door in 90_000L..240_000L)
            assertTrue(conversation in 45_000L..150_000L)
        }
    }

    @Test fun schedulingIsReproducibleWithSeed() {
        val first = AmbienceScheduler(Random(91))
        val second = AmbienceScheduler(Random(91))
        repeat(100) {
            assertEquals(
                first.nextDelayMs(AmbienceScheduler.Event.DOOR),
                second.nextDelayMs(AmbienceScheduler.Event.DOOR)
            )
        }
    }
    @Test fun whisperedScenesAreSparseAndDoNotStartImmediately() {
        val scheduler = AmbienceScheduler(Random(713))
        repeat(1_000) {
            assertTrue(scheduler.firstWhisperDelayMs() in 15_000L..31_000L)
            assertTrue(scheduler.nextWhisperDelayMs() in 24_000L..66_000L)
            assertTrue(scheduler.overlapWhisperDelayMs() in 450L..1_400L)
        }
    }

    @Test fun whisperExcerptsNeverRepeatConsecutivelyAndOverlapIsOccasional() {
        val scheduler = AmbienceScheduler(Random(112))
        var previous = -1
        val variants = mutableSetOf<Int>()
        repeat(2_000) {
            val next = scheduler.nextWhisperIndex(previous, 4)
            assertTrue(next in 0..3)
            if (previous >= 0) assertTrue(previous != next)
            variants.add(next)
            previous = next
        }
        assertEquals(setOf(0, 1, 2, 3), variants)
        val overlaps = (0 until 1_000).count { scheduler.shouldOverlapWhispers() }
        assertTrue("Overlaps should be occasional, not constant", overlaps in 100..300)
    }

    @Test fun whisperTimingsAreSeedReproducible() {
        val a = AmbienceScheduler(Random(56))
        val b = AmbienceScheduler(Random(56))
        repeat(100) {
            assertEquals(a.firstWhisperDelayMs(), b.firstWhisperDelayMs())
            assertEquals(a.nextWhisperDelayMs(), b.nextWhisperDelayMs())
            assertEquals(a.shouldOverlapWhispers(), b.shouldOverlapWhispers())
        }
    }

}
