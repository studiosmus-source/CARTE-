package it.studiosmus.carte.audio

import android.content.Context
import android.media.MediaPlayer
import android.media.audiofx.Equalizer
import android.os.Handler
import android.os.Looper
import android.os.SystemClock
import kotlin.math.roundToLong
import kotlin.random.Random

/**
 * Continuous, offline salon sound. A single jazz player is shared across the
 * home screen, cinematic blackout and game table. Only its acoustic perspective
 * changes; navigation never restarts the composition.
 *
 * All recordings are optional res/raw files. Missing recordings stay silent.
 */
class SalonAudio(
    private val context: Context,
    private val random: Random = Random.Default
) {
    enum class Scene { HOME, BLACK, SALON }
    private val scheduler = AmbienceScheduler(random)
    private val handler = Handler(Looper.getMainLooper())
    private val players = mutableSetOf<MediaPlayer>()
    private val names = mutableMapOf<MediaPlayer, String>()
    private val pending = mutableListOf<Runnable>()
    private var music: MediaPlayer? = null
    private var musicEqualizer: Equalizer? = null
    private var musicPositionMs = 0
    private var musicHasStarted = false
    private var generation = 0
    private var running = false
    private var scene = Scene.HOME
    private var musicPerspective = 1f
    private var roomPerspective = 0f
    private var transition: Runnable? = null
    private var settings = AmbienceSettings()
    private var fireplaceEnabled = false
    private var transientCount = 0

    fun hasRecording(name: String): Boolean =
        context.resources.getIdentifier(name, "raw", context.packageName) != 0

    fun setFireplaceEnabled(value: Boolean) {
        fireplaceEnabled = value
        if (running && value) playLoopIfMissing("salon_fireplace")
        if (!value) stopNamed("salon_fireplace")
        refreshMix()
    }

    private fun targetMusic(next: Scene): Float = when (next) {
        Scene.HOME -> 1f
        Scene.BLACK -> 0f
        Scene.SALON -> 0.29f
    }

    private fun targetRoom(next: Scene): Float = when (next) {
        Scene.HOME, Scene.BLACK -> 0f
        Scene.SALON -> 1f
    }

    /** Transitions affect all audible layers, not the player's seek position. */
    fun enterScene(next: Scene, durationMs: Long = 1800L) {
        scene = next
        transition?.let(handler::removeCallbacks)
        transition = null
        val fromMusic = musicPerspective
        val fromRoom = roomPerspective
        val toMusic = targetMusic(next)
        val toRoom = targetRoom(next)
        if (!running || durationMs <= 0L) {
            musicPerspective = toMusic
            roomPerspective = toRoom
            refreshMix()
            return
        }
        val begun = SystemClock.uptimeMillis()
        val task = object : Runnable {
            override fun run() {
                val progress = ((SystemClock.uptimeMillis() - begun).toFloat() /
                    durationMs).coerceIn(0f, 1f)
                val eased = progress * progress * (3f - 2f * progress)
                musicPerspective = fromMusic + (toMusic - fromMusic) * eased
                roomPerspective = fromRoom + (toRoom - fromRoom) * eased
                refreshMix()
                if (progress < 1f && running) handler.postDelayed(this, 40L)
                else if (transition === this) transition = null
            }
        }
        transition = task
        handler.post(task)
    }

    fun configure(value: AmbienceSettings) {
        settings = value.normalized()
        if (running) {
            ensureMusic()
            if (fireplaceEnabled) playLoopIfMissing("salon_fireplace")
        }
        refreshMix()
    }

    fun start() {
        if (running) return
        running = true
        generation++
        musicPerspective = targetMusic(scene)
        roomPerspective = targetRoom(scene)
        ensureMusic()
        if (fireplaceEnabled) playLoopIfMissing("salon_fireplace")
        AmbienceScheduler.Event.entries.forEach(::schedule)
        refreshMix()
    }

    fun stop() {
        if (!running) return
        running = false
        generation++
        transition?.let(handler::removeCallbacks)
        transition = null
        pending.forEach(handler::removeCallbacks)
        pending.clear()
        music?.let {
            musicPositionMs = try { it.currentPosition } catch (_: IllegalStateException) { musicPositionMs }
        }
        releaseEqualizer()
        players.toList().forEach(::dispose)
        music = null
        transientCount = 0
    }

    private fun ensureMusic() {
        if (!running || music != null || !hasRecording("salon_jazz")) return
        val player = createPlayer("salon_jazz") ?: return
        player.isLooping = true
        val length = player.duration
        if (!musicHasStarted && length > 10000) {
            // The salon is already playing: don't begin with the first note.
            musicPositionMs = random.nextInt(length / 5, length * 4 / 5)
        }
        musicHasStarted = true
        if (length > 0) player.seekTo(musicPositionMs.coerceIn(0, length - 1))
        music = player
        // An optional on-device EQ makes the same soundtrack clear in home and
        // gently muffled across the room, without a second overlapping player.
        try {
            musicEqualizer = Equalizer(0, player.audioSessionId).also { it.enabled = true }
        } catch (_: RuntimeException) {
            releaseEqualizer()
        }
        players.add(player)
        names[player] = "salon_jazz"
        try { player.start() } catch (_: IllegalStateException) { dispose(player) }
    }

    private fun createPlayer(name: String): MediaPlayer? {
        val id = context.resources.getIdentifier(name, "raw", context.packageName)
        if (id == 0) return null
        return try { MediaPlayer.create(context, id) } catch (_: RuntimeException) { null }
    }

    private fun volumeFor(name: String): Float = when (name) {
        "salon_jazz" -> settings.jazz * 0.82f * musicPerspective
        "salon_fireplace" -> if (fireplaceEnabled) settings.fireplace * 0.40f * roomPerspective else 0f
        "salon_conversation" -> settings.conversations * 0.52f * roomPerspective
        "salon_glassware" -> settings.glassware * 0.52f * roomPerspective
        "salon_door", "salon_chair", "salon_footsteps" ->
            settings.doors * 0.40f * roomPerspective
        "salon_applause" -> settings.conversations * 0.54f * roomPerspective
        "salon_card_deal", "salon_card_play", "salon_card_collect" ->
            settings.cards * roomPerspective
        else -> 0f
    }

    private fun releaseEqualizer() {
        try { musicEqualizer?.release() } catch (_: RuntimeException) { }
        musicEqualizer = null
    }

    private fun updateMusicDistance() {
        val eq = musicEqualizer ?: return
        try {
            val distance = (1f - musicPerspective).coerceIn(0f, 1f)
            val supported = eq.bandLevelRange
            val lower = supported[0].toInt()
            val upper = supported[1].toInt()
            for (index in 0 until eq.numberOfBands.toInt()) {
                val band = index.toShort()
                val hz = eq.getCenterFreq(band) / 1000f
                val maxCut = when {
                    hz >= 2600f -> 1050f  // room absorbs upper harmonics
                    hz >= 900f -> 480f
                    else -> 0f  // bass travels farther
                }
                val level = (-distance * maxCut).toInt()
                    .coerceIn(lower, upper).toShort()
                if (eq.getBandLevel(band) != level) eq.setBandLevel(band, level)
            }
        } catch (_: RuntimeException) {
            // EQ is optional. Device-specific effect errors must not stop music.
            releaseEqualizer()
        }
    }

    private fun refreshMix() {
        updateMusicDistance()
        players.toList().forEach { player ->
            val gain = volumeFor(names[player] ?: "")
            try { player.setVolume(gain, gain) } catch (_: IllegalStateException) { }
        }
    }

    private fun dispose(player: MediaPlayer) {
        players.remove(player)
        names.remove(player)
        if (music === player) {
            releaseEqualizer()
            music = null
        }
        try { player.release() } catch (_: RuntimeException) { }
    }

    private fun stopNamed(name: String) {
        players.filter { names[it] == name }.toList().forEach(::dispose)
    }

    private fun playLoopIfMissing(name: String) {
        if (!running || players.any { names[it] == name }) return
        val player = createPlayer(name) ?: return
        player.isLooping = true
        players.add(player)
        names[player] = name
        refreshMix()
        try { player.start() } catch (_: IllegalStateException) { dispose(player) }
    }

    /** Returns false only when no sampled foley asset exists: legacy fallback. */
    fun playCard(event: Soundscape.CardEvent): Boolean {
        val name = when (event) {
            Soundscape.CardEvent.DEAL -> "salon_card_deal"
            Soundscape.CardEvent.PLAY -> "salon_card_play"
            Soundscape.CardEvent.COLLECT -> "salon_card_collect"
        }
        if (!hasRecording(name)) return false
        if (settings.cards > 0f) playOnce(name)
        return true
    }

    /** Only an approved, restrained applause; defeats and ties stay silent. */
    fun playResult(won: Boolean) {
        if (won && scene == Scene.SALON) playOnce("salon_applause")
    }

    private fun playOnce(name: String): Boolean {
        if (!running || !hasRecording(name)) return false
        val player = createPlayer(name) ?: return false
        val session = generation
        players.add(player)
        names[player] = name
        if (name != "salon_card_deal" && name != "salon_card_play" &&
            name != "salon_card_collect") transientCount++
        refreshMix()
        fun finish(p: MediaPlayer) {
            if (session == generation && name != "salon_card_deal" &&
                name != "salon_card_play" && name != "salon_card_collect") {
                transientCount = (transientCount - 1).coerceAtLeast(0)
            }
            dispose(p)
        }
        player.setOnCompletionListener(::finish)
        player.setOnErrorListener { broken, _, _ -> finish(broken); true }
        try { player.start() } catch (_: IllegalStateException) { finish(player); return false }
        return true
    }

    private fun schedule(event: AmbienceScheduler.Event) {
        if (!running) return
        val task = object : Runnable {
            override fun run() {
                pending.remove(this)
                if (!running) return
                if (scene == Scene.SALON && transientCount == 0) {
                    val name = when (event) {
                        AmbienceScheduler.Event.GLASSWARE -> "salon_glassware"
                        AmbienceScheduler.Event.CONVERSATION -> "salon_conversation"
                        AmbienceScheduler.Event.DOOR -> listOf(
                            "salon_chair", "salon_footsteps", "salon_door"
                        ).random(random)
                    }
                    playOnce(name)
                }
                schedule(event)
            }
        }
        pending.add(task)
        handler.postDelayed(task, scheduler.nextDelayMs(event))
    }
}
