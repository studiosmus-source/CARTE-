package it.studiosmus.carte.audio

import android.content.Context
import android.media.AudioAttributes
import android.media.MediaPlayer
import android.media.SoundPool
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
    private val spatialMix = mutableMapOf<MediaPlayer, Pair<Float, Float>>()
    private val pending = mutableListOf<Runnable>()
    private var music: MediaPlayer? = null
    private var previewPlayer: MediaPlayer? = null
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
    private val whisperNames = listOf("salon_whisper_01", "salon_whisper_02", "salon_whisper_03", "salon_whisper_04")
    private var lastWhisper = -1
    private var whisperTask: Runnable? = null
    // Card impact cues are preloaded, unlike the long-running room ambience.
    private var cardPool: SoundPool? = null
    private val cardSamples = mutableMapOf<CardSoundEvent, Int>()
    private val cardReady = mutableSetOf<Int>()
    private var activeCardStream = 0
    private var cardStopRunnable: Runnable? = null

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
        val previous = scene
        scene = next
        // Salon conversations have their own irregular, scene-scoped schedule.
        // Never play a long fixed greeting each time the table opens.
        if (next == Scene.SALON && previous != Scene.SALON && running) {
            scheduleWhisper(initial = true)
        } else if (next != Scene.SALON) {
            cancelWhisper()
        }
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
        prepareCardCues()
        if (fireplaceEnabled) playLoopIfMissing("salon_fireplace")
        listOf(AmbienceScheduler.Event.GLASSWARE, AmbienceScheduler.Event.DOOR).forEach(::schedule)
        if (scene == Scene.SALON) scheduleWhisper(initial = true)
        refreshMix()
    }

    fun stop() {
        if (!running) return
        stopPreview()
        running = false
        generation++
        cancelWhisper()
        stopCardCue()
        cardPool?.release()
        cardPool = null
        cardSamples.clear()
        cardReady.clear()
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

    /**
     * Settings-only isolated playback. A one-shot sample is loud enough to
     * identify, while the jazz is ducked. It never changes persisted sliders.
     * The fireplace and conversations previews stop after a few seconds.
     */
    fun previewSample(name: String): Boolean {
        stopPreview()
        if (!running || !hasRecording(name)) return false
        val p = createPlayer(name) ?: return false
        previewPlayer = p
        p.setVolume(0.64f, 0.64f)
        p.setOnCompletionListener { if (previewPlayer === it) stopPreview() }
        p.setOnErrorListener { broken, _, _ ->
            if (previewPlayer === broken) stopPreview()
            true
        }
        val end = object : Runnable {
            override fun run() {
                pending.remove(this)
                if (previewPlayer === p) stopPreview()
            }
        }
        pending.add(end)
        handler.postDelayed(end, when (name) {
            "salon_fireplace" -> 6500L
            "salon_conversation" -> 9500L
            else -> 7000L
        })
        refreshMix()
        try { p.start() } catch (_: IllegalStateException) {
            stopPreview()
            return false
        }
        return true
    }

    private fun stopPreview() {
        val p = previewPlayer ?: return
        previewPlayer = null
        try { p.release() } catch (_: RuntimeException) { }
        refreshMix()
    }

    private fun createPlayer(name: String): MediaPlayer? {
        val id = context.resources.getIdentifier(name, "raw", context.packageName)
        if (id == 0) return null
        return try { MediaPlayer.create(context, id) } catch (_: RuntimeException) { null }
    }

    private fun volumeFor(name: String): Float = when (name) {
        "salon_jazz" -> settings.jazz * 0.82f * musicPerspective
        "salon_fireplace" -> if (fireplaceEnabled) settings.fireplace * 0.40f * roomPerspective else 0f
        "salon_conversation" -> settings.conversationGain(roomPerspective)
        "salon_whisper_01", "salon_whisper_02", "salon_whisper_03",
        "salon_whisper_04" -> settings.conversationGain(roomPerspective)
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
            val (level, pan) = spatialMix[player] ?: (1f to 0f)
            val gain = volumeFor(names[player] ?: "") * level *
                if (previewPlayer != null && player === music) 0.08f else 1f
            try {
                player.setVolume(
                    (gain * (1f - pan.coerceAtLeast(0f))).coerceIn(0f, 1f),
                    (gain * (1f + pan.coerceAtMost(0f))).coerceIn(0f, 1f)
                )
            } catch (_: IllegalStateException) { }
        }
        if (activeCardStream != 0) {
            val gain = (settings.cards * roomPerspective).coerceIn(0f, 1f)
            cardPool?.setVolume(activeCardStream, gain, gain)
        }
    }

    private fun dispose(player: MediaPlayer) {
        players.remove(player)
        names.remove(player)
        spatialMix.remove(player)
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

    /**
     * Preload short real-world card foley on resume so that PLAY/DEAL/COLLECT
     * are triggered without the MediaPlayer creation latency on every move.
     * Loaded IDs are tied to this pool instance; stale callbacks are ignored.
     */
    private fun prepareCardCues() {
        if (cardPool != null) return
        val attributes = AudioAttributes.Builder()
            .setUsage(AudioAttributes.USAGE_GAME)
            .setContentType(AudioAttributes.CONTENT_TYPE_SONIFICATION)
            .build()
        val pool = SoundPool.Builder()
            .setMaxStreams(1)
            .setAudioAttributes(attributes)
            .build()
        cardPool = pool
        pool.setOnLoadCompleteListener { completedPool, soundId, status ->
            if (cardPool === completedPool && status == 0) cardReady.add(soundId)
        }
        for (event in CardSoundEvent.entries) {
            val name = when (event) {
                CardSoundEvent.DEAL -> "salon_card_deal"
                CardSoundEvent.PLAY -> "salon_card_play"
                CardSoundEvent.COLLECT -> "salon_card_collect"
            }
            val resource = context.resources.getIdentifier(name, "raw", context.packageName)
            if (resource != 0) {
                val soundId = pool.load(context, resource, 1)
                if (soundId != 0) cardSamples[event] = soundId
            }
        }
    }

    private fun stopCardCue() {
        cardStopRunnable?.let(handler::removeCallbacks)
        cardStopRunnable = null
        if (activeCardStream != 0) {
            cardPool?.stop(activeCardStream)
            activeCardStream = 0
        }
    }

    /** One real recorded impact per visual event; never let it overlap the next action. */
    fun playCard(event: CardSoundEvent): Boolean {
        val pool = cardPool ?: return false
        if (!running || scene != Scene.SALON || settings.cards <= 0f) return false
        val soundId = cardSamples[event] ?: return false
        if (soundId !in cardReady) return false
        stopCardCue()
        val volume = (settings.cards * roomPerspective).coerceIn(0f, 1f)
        val stream = pool.play(soundId, volume, volume, 1, 0, 1f)
        if (stream == 0) return false
        activeCardStream = stream
        // Defensive caps also cover an accidentally bundled long audio file.
        val maxDuration = when (event) {
            CardSoundEvent.DEAL -> 500L
            CardSoundEvent.PLAY -> 310L
            CardSoundEvent.COLLECT -> 420L
        }
        val stop = Runnable {
            if (cardPool === pool && activeCardStream == stream) {
                pool.stop(stream)
                activeCardStream = 0
                cardStopRunnable = null
            }
        }
        cardStopRunnable = stop
        handler.postDelayed(stop, maxDuration)
        return true
    }

    /** Only an approved, restrained applause; defeats and ties stay silent. */
    fun playResult(won: Boolean) {
        if (won && scene == Scene.SALON) playOnce("salon_applause")
    }

    private fun playOnce(name: String, level: Float = 1f, pan: Float = 0f): Boolean {
        if (!running || !hasRecording(name)) return false
        val player = createPlayer(name) ?: return false
        val session = generation
        players.add(player)
        names[player] = name
        spatialMix[player] = level.coerceIn(0f, 1f) to pan.coerceIn(-0.55f, 0.55f)
        transientCount++
        refreshMix()
        fun finish(p: MediaPlayer) {
            if (session == generation) {
                transientCount = (transientCount - 1).coerceAtLeast(0)
            }
            dispose(p)
        }
        player.setOnCompletionListener(::finish)
        player.setOnErrorListener { broken, _, _ -> finish(broken); true }
        try { player.start() } catch (_: IllegalStateException) { finish(player); return false }
        return true
    }

    private fun cancelWhisper() {
        whisperTask?.let(handler::removeCallbacks)
        whisperTask = null
    }

    private fun scheduleWhisper(initial: Boolean = false) {
        cancelWhisper()
        if (!running || scene != Scene.SALON) return
        val delay = if (initial) scheduler.firstWhisperDelayMs()
                    else scheduler.nextWhisperDelayMs()
        val task = object : Runnable {
            override fun run() {
                if (whisperTask !== this) return
                whisperTask = null
                if (!running || scene != Scene.SALON) return
                playWhisperConversation()
                scheduleWhisper()
            }
        }
        whisperTask = task
        handler.postDelayed(task, delay)
    }

    /**
     * Genuine voices from different excerpts of the approved CC0 conversation.
     * Most visits have one distant group; occasionally a second excerpt starts
     * off-axis, suggesting two small groups without a constant crowd bed.
     */
    private fun playWhisperConversation() {
        if (settings.conversations <= 0f || transientCount > 2) return
        val available = whisperNames.filter(::hasRecording)
        if (available.isEmpty()) return
        val firstIndex = scheduler.nextWhisperIndex(lastWhisper, available.size)
        lastWhisper = firstIndex
        val pan = if (random.nextBoolean()) -.34f else .34f
        val level = random.nextFloat() * .18f + .72f
        if (!playOnce(available[firstIndex], level, pan)) return
        if (!scheduler.shouldOverlapWhispers() || available.size < 2) return
        val secondIndex = scheduler.nextWhisperIndex(firstIndex, available.size)
        val token = generation
        val follow = object : Runnable {
            override fun run() {
                pending.remove(this)
                if (running && scene == Scene.SALON && token == generation &&
                    settings.conversations > 0f && transientCount < 3) {
                    playOnce(available[secondIndex], level * .64f, -pan)
                }
            }
        }
        pending.add(follow)
        handler.postDelayed(follow, scheduler.overlapWhisperDelayMs())
    }

    /** A brief believable incident, not independent randomly overlapping noises. */
    private fun playRoomMovement() {
        if (random.nextInt(10) == 0 && playOnce("salon_door")) return
        if (!playOnce("salon_chair")) {
            playOnce("salon_footsteps")
            return
        }
        if (!hasRecording("salon_footsteps") || random.nextInt(4) == 0) return
        val session = generation
        val follow = object : Runnable {
            override fun run() {
                pending.remove(this)
                if (running && session == generation && scene == Scene.SALON)
                    playOnce("salon_footsteps")
            }
        }
        pending.add(follow)
        handler.postDelayed(follow, random.nextLong(1200L, 2300L))
    }

    private fun schedule(event: AmbienceScheduler.Event) {
        if (!running) return
        val task = object : Runnable {
            override fun run() {
                pending.remove(this)
                if (!running) return
                if (scene == Scene.SALON && transientCount < 3) {
                    when (event) {
                        AmbienceScheduler.Event.GLASSWARE -> playOnce("salon_glassware")
                        AmbienceScheduler.Event.CONVERSATION -> Unit // scheduled separately
                        AmbienceScheduler.Event.DOOR -> playRoomMovement()
                    }
                }
                schedule(event)
            }
        }
        pending.add(task)
        handler.postDelayed(task, scheduler.nextDelayMs(event))
    }
}
