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
    private var settings = AmbienceSettings()

    fun configure(value: AmbienceSettings) {
        settings = value.normalized()
    }

    fun start() {
        if (running) return
        running = true
        playLoop("salon_jazz", settings.jazz)
        AmbienceScheduler.Event.entries.forEach { schedule(it) }
    }

    fun stop() {
        running = false
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
                playOnce(name, gain)
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
        val player = MediaPlayer.create(context, id) ?: return
        player.isLooping = loop
        player.setVolume(gain, gain)
        players.add(player)
        playerNames[player] = name
        player.setOnCompletionListener {
            players.remove(it)
            playerNames.remove(it)
            it.release()
        }
        player.start()
    }
}
