package it.studiosmus.carte.audio

import android.media.AudioAttributes
import android.media.AudioFormat
import android.media.AudioTrack
import kotlin.math.PI
import kotlin.math.exp
import kotlin.math.sin
import kotlin.random.Random

/**
 * Two independent offline layers: low-level fireplace and short card foley.
 * Card events are band-limited paper/felt taps, never continuous white noise.
 * Volumes ramp smoothly to prevent slider clicks and start/stop discontinuities.
 */
class Soundscape(private val random: Random = Random.Default) {
    enum class Channel { FIREPLACE, JAZZ, ROOM, CARDS }
    enum class CardEvent { DEAL, PLAY, COLLECT }
    @Volatile private var fireplaceTarget = 0.22f
    @Volatile private var cardsTarget = 0.55f
    @Volatile private var enabled = false
    @Volatile private var running = false
    private var track: AudioTrack? = null
    private var thread: Thread? = null
    private val events = java.util.concurrent.ConcurrentLinkedQueue<CardEvent>()

    fun volume(channel: Channel): Float = when (channel) {
        Channel.FIREPLACE -> fireplaceTarget
        Channel.CARDS -> cardsTarget
        else -> 0f
    }

    fun setVolume(channel: Channel, volume: Float) {
        when (channel) {
            Channel.FIREPLACE -> fireplaceTarget = volume.coerceIn(0f, 1f)
            Channel.CARDS -> cardsTarget = volume.coerceIn(0f, 1f)
            else -> Unit
        }
    }

    fun setFireplaceEnabled(value: Boolean) {
        enabled = value
        if (value) start()
    }

    fun playCard(event: CardEvent) {
        if (cardsTarget <= 0f) return
        events.offer(event)
        start()
    }

    @Synchronized
    fun start() {
        if (running) return
        val rate = 22050
        val minimum = AudioTrack.getMinBufferSize(rate,
            AudioFormat.CHANNEL_OUT_MONO, AudioFormat.ENCODING_PCM_16BIT)
        if (minimum <= 0) return
        val audio = try {
            AudioTrack.Builder()
                .setAudioAttributes(AudioAttributes.Builder()
                    .setUsage(AudioAttributes.USAGE_GAME)
                    .setContentType(AudioAttributes.CONTENT_TYPE_SONIFICATION).build())
                .setAudioFormat(AudioFormat.Builder().setSampleRate(rate)
                    .setEncoding(AudioFormat.ENCODING_PCM_16BIT)
                    .setChannelMask(AudioFormat.CHANNEL_OUT_MONO).build())
                .setBufferSizeInBytes(maxOf(minimum * 2, 4096))
                .setTransferMode(AudioTrack.MODE_STREAM).build()
        } catch (_: RuntimeException) { return }
        if (audio.state != AudioTrack.STATE_INITIALIZED) {
            audio.release()
            return
        }
        track = audio
        running = true
        audio.play()
        thread = Thread {
            val samples = ShortArray(512)
            var low = 0.0
            var high = 0.0
            var fireGain = 0.0
            var cardGain = 0.0
            var envelope = 0.0
            var cardBody = 0.0
            var cardHigh = 0.0
            var cardAge = 0
            var cardDuration = 1
            var cardStrength = 0.0
            var phase = 0.0
            var ember = 0.0
            val reflections = DoubleArray(1901)
            var reflectionIndex = 0
            var quietFrames = 0
            while (running) {
                for (i in samples.indices) {
                    val event = events.poll()
                    if (event != null) {
                        cardAge = 0
                        cardDuration = when (event) {
                            CardEvent.DEAL -> 1550
                            CardEvent.PLAY -> 2300
                            CardEvent.COLLECT -> 3200
                        }
                        cardStrength = when (event) {
                            CardEvent.DEAL -> 0.14
                            CardEvent.PLAY -> 0.22
                            CardEvent.COLLECT -> 0.17
                        }
                    }
                    fireGain += ((if (enabled) fireplaceTarget else 0f) - fireGain) * 0.0008
                    cardGain += (cardsTarget - cardGain) * 0.001
                    val noise = random.nextDouble(-1.0, 1.0)
                    low = low * 0.995 + noise * 0.005
                    high = high * 0.88 + noise * 0.12
                    phase += 2 * PI * 0.17 / rate
                    val breathing = 0.82 + 0.18 * sin(phase)
                    ember *= 0.9991
                    if (random.nextDouble() < 0.000015) ember = random.nextDouble(0.03, 0.09)
                    val fire = (low * 1.65 * breathing + ember * low) * fireGain * 0.42
                    var paper = 0.0
                    if (cardAge < cardDuration) {
                        val t = cardAge.toDouble() / cardDuration
                        envelope = (1.0 - t) * (1.0 - t)
                        cardBody = cardBody * 0.83 + noise * 0.17
                        cardHigh = cardHigh * 0.52 + noise * 0.48
                        paper = (cardBody * 0.78 + cardHigh * 0.22) *
                            envelope * cardStrength * cardGain
                        cardAge++
                    }
                    val direct = fire + paper
                    val echo = reflections[reflectionIndex]
                    reflections[reflectionIndex] = direct * 0.12 + echo * 0.14
                    reflectionIndex = (reflectionIndex + 1) % reflections.size
                    val output = (direct + echo * 0.25).coerceIn(-0.85, 0.85)
                    samples[i] = (output * Short.MAX_VALUE).toInt().toShort()
                }
                val written = audio.write(samples, 0, samples.size)
                if (written <= 0) break
                if (!enabled && events.isEmpty() && cardAge >= cardDuration && fireGain < 0.0001) {
                    quietFrames++
                    if (quietFrames > 40) break
                } else quietFrames = 0
            }
            // The owner releases AudioTrack from stop(); do not race its lifecycle here.
        }.apply { name = "CARTE-audio-mixer"; isDaemon = true; start() }
    }

    @Synchronized
    fun stop() {
        running = false
        events.clear()
        thread?.join(500)
        track?.let {
            try { it.pause(); it.flush() } catch (_: IllegalStateException) {}
            it.release()
        }
        track = null
        thread = null
    }

    fun release() = stop()
}
