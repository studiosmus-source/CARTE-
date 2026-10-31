package it.studiosmus.carte.ui

import android.view.View
import android.view.animation.AccelerateDecelerateInterpolator

/** View-only movement. Game state is advanced exclusively by the session. */
object CardMotion {
    fun reveal(card: View?, fromAbove: Boolean, dp: (Int) -> Int) {
        travel(card, if (fromAbove) 0f else 0f,
            if (fromAbove) -dp(110).toFloat() else dp(110).toFloat(),
            300L)
    }

    fun deal(card: View?, fromAbove: Boolean, dp: (Int) -> Int, delay: Long = 0L) {
        travel(card, if (fromAbove) dp(44).toFloat() else -dp(44).toFloat(),
            if (fromAbove) -dp(140).toFloat() else -dp(65).toFloat(),
            460L, delay)
    }

    fun collect(card: View?, toComputer: Boolean, dp: (Int) -> Int) {
        if (card == null) return
        card.animate().cancel()
        card.animate()
            .translationX(dp(70).toFloat())
            .translationY(if (toComputer) -dp(180).toFloat() else dp(180).toFloat())
            .alpha(0f)
            .scaleX(.88f).scaleY(.88f)
            .setInterpolator(AccelerateDecelerateInterpolator())
            .setDuration(320L).start()
    }

    private fun travel(card: View?, x: Float, y: Float, duration: Long, delay: Long = 0L) {
        if (card == null) return
        card.animate().cancel()
        card.alpha = 0.35f
        card.translationX = x
        card.translationY = y
        card.scaleX = .92f
        card.scaleY = .92f
        card.animate()
            .translationX(0f).translationY(0f)
            .alpha(1f).scaleX(1f).scaleY(1f)
            .setInterpolator(AccelerateDecelerateInterpolator())
            .setStartDelay(delay)
            .setDuration(duration).start()
    }
}
