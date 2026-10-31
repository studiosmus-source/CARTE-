package it.studiosmus.carte.audio

import kotlin.random.Random

/**
 * Pure selector for physically distinct recordings. Repeat-free when two or
 * more loaded takes exist; handles partial resource loads safely.
 */
internal object CardCuePicker {
    fun next(previous: Int, size: Int, random: Random): Int {
        require(size > 0)
        if (size == 1) return 0
        if (previous !in 0 until size) return random.nextInt(size)
        val candidate = random.nextInt(size - 1)
        return if (candidate >= previous) candidate + 1 else candidate
    }
}
