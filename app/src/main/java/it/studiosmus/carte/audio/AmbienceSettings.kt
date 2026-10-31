package it.studiosmus.carte.audio

/**
 * Independent sound layers for the offline salon atmosphere.
 * Playback assets are intentionally not assumed to exist.
 */
data class AmbienceSettings(
    val fireplace: Float = 0.22f,
    val jazz: Float = 0.20f,
    val conversations: Float = 0.12f,
    val glassware: Float = 0.24f,
    val doors: Float = 0.16f,
    val cards: Float = 0.55f
) {
    /** Gain shared by the old preview and every short in-game whisper excerpt. */
    fun conversationGain(roomPerspective: Float): Float =
        conversations * 0.52f * roomPerspective.coerceIn(0f, 1f)

    fun normalized(): AmbienceSettings = copy(
        fireplace = fireplace.coerceIn(0f, 1f),
        jazz = jazz.coerceIn(0f, 1f),
        conversations = conversations.coerceIn(0f, 1f),
        glassware = glassware.coerceIn(0f, 1f),
        doors = doors.coerceIn(0f, 1f),
        cards = cards.coerceIn(0f, 1f)
    )
}

/**
 * Independent scheduling avoids audible, repetitive loops.
 * A single Random instance makes sequences reproducible in tests.
 */
class AmbienceScheduler(private val random: kotlin.random.Random = kotlin.random.Random.Default) {
    enum class Event { GLASSWARE, DOOR, CONVERSATION }

    fun nextDelayMs(event: Event): Long {
        val range = when (event) {
            Event.GLASSWARE -> 25_000L..85_000L
            Event.DOOR -> 90_000L..240_000L
            Event.CONVERSATION -> 45_000L..150_000L
        }
        return random.nextLong(range.first, range.last + 1)
    }

    /** Voices begin after a natural settling-in period, then return irregularly. */
    fun firstWhisperDelayMs(): Long = random.nextLong(15_000L, 31_001L)

    fun nextWhisperDelayMs(): Long = random.nextLong(24_000L, 66_001L)

    fun overlapWhisperDelayMs(): Long = random.nextLong(450L, 1_401L)

    fun shouldOverlapWhispers(): Boolean = random.nextInt(5) == 0

    /** Avoid replaying the exact same excerpt twice in succession. */
    fun nextWhisperIndex(previous: Int, size: Int): Int {
        require(size > 0)
        if (size == 1) return 0
        val choice = random.nextInt(size - 1)
        return if (choice >= previous && previous in 0 until size) choice + 1 else choice
    }

    fun nextEventDurationMs(event: Event): Long = when (event) {
        Event.GLASSWARE -> random.nextLong(400L, 1600L)
        Event.DOOR -> random.nextLong(800L, 3000L)
        Event.CONVERSATION -> random.nextLong(2500L, 9500L)
    }
}
