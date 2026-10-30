package it.studiosmus.carte.audio

import android.media.AudioAttributes
import android.media.AudioFormat
import android.media.AudioTrack
import kotlin.math.PI
import kotlin.math.sin
import kotlin.random.Random

/**
 * Offline ambient mixer. Synthesized fireplace is real audio, not a placeholder.
 * Jazz / glasses / doors / whispers require cleared sound assets and are not faked.
 */
class Soundscape(private val random: Random = Random.Default) {
    enum class Channel { FIREPLACE, JAZZ, ROOM, CARDS }
    private val levels = Channel.entries.associateWith { if (it == Channel.FIREPLACE) 0.22f else 0.35f }.toMutableMap()
    private var track: AudioTrack? = null
    private var running = false
    private var thread: Thread? = null

    fun volume(channel: Channel): Float = levels.getValue(channel)

    fun setVolume(channel: Channel, volume: Float) {
        levels[channel] = volume.coerceIn(0f, 1f)
    }

    @Synchronized
    fun start() {
        if (running) return
        val sampleRate = 22050
        val bufferSize = AudioTrack.getMinBufferSize(
            sampleRate, AudioFormat.CHANNEL_OUT_MONO, AudioFormat.ENCODING_PCM_16BIT
        )
        if (bufferSize <= 0) return
        val audio = AudioTrack.Builder()
            .setAudioAttributes(
                AudioAttributes.Builder()
                    .setUsage(AudioAttributes.USAGE_MEDIA)
                    .setContentType(AudioAttributes.CONTENT_TYPE_MUSIC)
                    .build()
            )
            .setAudioFormat(
                AudioFormat.Builder()
                    .setSampleRate(sampleRate)
                    .setEncoding(AudioFormat.ENCODING_PCM_16BIT)
                    .setChannelMask(AudioFormat.CHANNEL_OUT_MONO)
                    .build()
            )
            .setBufferSizeInBytes(bufferSize * 2)
            .setTransferMode(AudioTrack.MODE_STREAM)
            .build()
        if (audio.state != AudioTrack.STATE_INITIALIZED) {
            audio.release()
            return
        }
        track = audio
        running = true
        audio.play()
        thread = Thread {
            val samples = ShortArray(1024)
            var low = 0.0
            var phase = 0.0
            var crackle = 0.0
            var warmth = 0.0
            var crackleTone = 0.0
            var crackleSamples = 0
            var cracklePolarity = 1.0
            // Short, quiet room reflections: subtle ambience rather than an audible echo.
            val reflections = DoubleArray(4096)
            var reflectionIndex = 0
            while (running) {
                val gain = volume(Channel.FIREPLACE).toDouble()
                for (i in samples.indices) {
                    val noise = random.nextDouble(-1.0, 1.0)
                    low = low * 0.997 + noise * 0.003
                    warmth = warmth * 0.965 + low * 0.035
                    // Occasional wood snaps with short textured tails, not a continuous hiss.
                    if (crackleSamples == 0 && random.nextDouble() < 0.00013) {
                        crackleSamples = random.nextInt(90, 360)
                        crackle = random.nextDouble(0.35, 0.85)
                        cracklePolarity = if (random.nextBoolean()) 1.0 else -1.0
                    }
                    var snap = 0.0
                    if (crackleSamples > 0) {
                        // A quick attack followed by a damped, irregular crackle.
                        crackleTone = crackleTone * 0.62 + noise * 0.38
                        snap = crackle * (0.55 * crackleTone + 0.45 * cracklePolarity)
                        crackle *= 0.974
                        crackleSamples--
                    } else {
                        crackleTone *= 0.62
                    }
                    phase += 2 * PI * 0.21 / sampleRate
                    val breath = 0.75 + 0.25 * sin(phase)
                    // Background is intentionally very quiet; snaps carry the fireplace identity.
                    val signal = (warmth * 0.95 * breath + low * 0.22 + snap * 0.70) * gain * 0.75
                    val reflected = reflections[reflectionIndex]
                    val roomSignal = signal * 0.88 + reflected * 0.20
                    reflections[reflectionIndex] = signal * 0.38 + reflected * 0.10
                    reflectionIndex = (reflectionIndex + 1) % reflections.size
                    samples[i] = (roomSignal.coerceIn(-1.0, 1.0) * Short.MAX_VALUE).toInt().toShort()
                }
                var written = 0
                while (running && written < samples.size) {
                    val count = audio.write(samples, written, samples.size - written)
                    if (count <= 0) break
                    written += count
                }
            }
        }.apply { name = "CARTE-fireplace"; isDaemon = true; start() }
    }

    @Synchronized
    fun stop() {
        running = false
        thread?.join(350)
        track?.pause()
        track?.flush()
        track?.release()
        track = null
        thread = null
    }

    fun release() = stop()
}
