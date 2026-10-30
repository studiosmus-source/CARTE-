package it.studiosmus.carte.audio

import android.content.Context
import android.media.MediaPlayer
import android.os.Handler
import android.os.Looper
import kotlin.random.Random

/**
 * Optional, locally bundled salon recordings. No network calls.
 * Missing assets are skipped silently; this never fabricates conversations or music.
 * Place licensed files in res/raw as:
 * salon_jazz, salon_glassware, salon_door, salon_conversation.
 */
class SalonAudio(
    private val context: Context,
    private val random: Random = Random.Default
) {
    private val scheduler = AmbienceScheduler(random)
    private val handler = Handler(Looper.getMainLooper())
    private val players = mutableSetOf<MediaPlayer>()
    private val pending = mutableListOf<Runnable>()
    private val playerNames = mutableMapOf<MediaPlayer, String>()
    private var running = false
    private var transientActive = false
    private var generation = 0
    private var settings = AmbienceSettings()

    private fun mixVolume(name: String): Float = when (name) {
        "salon_jazz" -> settings.jazz * if (transientActive) 0.38f else 0.70f
        "salon_glassware" -> settings.glassware * 0.65f
        "salon_door" -> settings.doors * 0.55f
        "salon_conversation" -> settings.conversations * 0.50f
        else -> 0f
    }

    private fun refreshMix() {
        players.forEach { player ->
            val gain = mixVolume(playerNames[player] ?: "")
            player.setVolume(mixVolume(name), mixVolume(name))
        }
    }

    fun configure(value: AmbienceSettings) {
        settings = value.normalized()
        refreshMix()
    }

    fun start() {
        if (running) return
        running = true
        generation++
        playLoop("salon_jazz", settings.jazz)
        AmbienceScheduler.Event.entries.forEach { schedule(it) }
    }

    fun stop() {
        running = false
        generation++
        transientActive = false
        pending.forEach(handler::removeCallbacks)
        pending.clear()
        players.toList().forEach { it.release() }
        players.clear()
        playerNames.clear()
    }

    private fun schedule(event: AmbienceScheduler.Event) {
        if (!running) return
        val task = object : Runnable {
            override fun run() {
                pending.remove(this)
                if (!running) return
                val (name, gain) = when (event) {
                    AmbienceScheduler.Event.GLASSWARE -> "salon_glassware" to settings.glassware
                    AmbienceScheduler.Event.DOOR -> "salon_door" to settings.doors
                    AmbienceScheduler.Event.CONVERSATION -> "salon_conversation" to settings.conversations
                }
                // Only one occasional foreground event at a time: no stacked doors/glasses/voices.
                if (!transientActive) playOnce(name, gain)
                schedule(event)
            }
        }
        pending.add(task)
        handler.postDelayed(task, scheduler.nextDelayMs(event))
    }

    private fun rawId(name: String): Int =
        context.resources.getIdentifier(name, "raw", context.packageName)

    private fun playLoop(name: String, gain: Float) {
        play(name, gain, loop = true)
    }

    private fun playOnce(name: String, gain: Float) {
        play(name, gain, loop = false)
    }

    private fun play(name: String, gain: Float, loop: Boolean) {
        if (!running || gain <= 0f) return
        val id = rawId(name)
        if (id == 0) return
        val player = try { MediaPlayer.create(context, id) } catch (_: RuntimeException) { null } ?: return
        val session = generation
        player.isLooping = loop
        if (!loop) transientActive = true
        player.setVolume(gain, gain)
        players.add(player)
        playerNames[player] = name
        refreshMix()
        player.setOnCompletionListener {
            players.remove(it)
            playerNames.remove(it)
            if (!loop && session == generation) transientActive = false
            it.release()
            refreshMix()
        }
        player.setOnErrorListener { broken, _, _ ->
            players.remove(broken)
            playerNames.remove(broken)
            if (!loop && session == generation) transientActive = false
            broken.release()
            refreshMix()
            true
        }
        try { player.start() } catch (_: IllegalStateException) {
            players.remove(player)
            playerNames.remove(player)
            if (!loop && session == generation) transientActive = false
            player.release()
            refreshMix()
        }
    }
}
