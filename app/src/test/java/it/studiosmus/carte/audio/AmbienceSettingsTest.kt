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
}
