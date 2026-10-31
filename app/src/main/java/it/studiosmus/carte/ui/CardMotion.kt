package it.studiosmus.carte.ui

import android.view.View
import android.view.animation.DecelerateInterpolator
import android.view.animation.AccelerateInterpolator

/**
 * Only moves view properties; the game engine and the turn/timer remain in
 * the two Session classes. A played card has a physical departure and landing,
 * then a computer-owned FlipCardView turns to show its face.
 */
object CardMotion {
    fun reveal(card: View?, fromAbove: Boolean, dp: (Int) -> Int) {
        if (card == null) return
        slide(card,
            if (fromAbove) dp(31).toFloat() else -dp(28).toFloat(),
            if (fromAbove) -dp(140).toFloat() else dp(160).toFloat(),
            if (fromAbove) -7f else 6f, 205L)
        if (card is FlipCardView) card.reveal(afterMs = 160L)
    }

    fun deal(card: View?, fromAbove: Boolean, dp: (Int) -> Int, delay: Long = 0L) {
        // Both hands get cards from the tabletop stock, rather than appearing
        // out of nothing at the top/bottom edge.
        slide(card, if (fromAbove) dp(85).toFloat() else -dp(80).toFloat(),
            if (fromAbove) dp(140).toFloat() else -dp(160).toFloat(),
            if (fromAbove) 9f else -8f, 380L, delay)
    }

    /**
     * Cards really leave the on-screen stock and land in the correct hand.
     * Both coordinates are converted from screen-space AFTER layout, rather
     * than guessing based on phone size or density.
     */
    fun dealFromStock(card: View?, stock: View?, dp: (Int) -> Int,
                      delay: Long = 0L) {
        if (card == null) return
        if (stock == null) {
            deal(card, false, dp, delay)
            return
        }
        card.alpha = 0f
        card.post {
            if (!card.isAttachedToWindow || !stock.isAttachedToWindow ||
                card.width <= 0 || stock.width <= 0) {
                card.alpha = 1f
                return@post
            }
            val stockAt = IntArray(2)
            val targetAt = IntArray(2)
            stock.getLocationOnScreen(stockAt)
            card.getLocationOnScreen(targetAt)
            val (dx, dy) = CardMotionGeometry.stockToCard(
                stockAt[0].toFloat(), stockAt[1].toFloat(),
                stock.width.toFloat(), stock.height.toFloat(),
                targetAt[0].toFloat(), targetAt[1].toFloat(),
                card.width.toFloat(), card.height.toFloat()
            )
            slide(card, dx, dy, if (dx > 0) 10f else -10f, 390L, delay)
        }
    }

    fun collect(card: View?, toComputer: Boolean, dp: (Int) -> Int) =
        collect(card, toComputer, dp, 0L)

    fun collect(card: View?, toComputer: Boolean, dp: (Int) -> Int, delay: Long) {
        if (card == null) return
        card.animate().cancel()
        card.animate()
            .translationX(if (toComputer) dp(132).toFloat() else -dp(132).toFloat())
            .translationY(if (toComputer) -dp(165).toFloat() else dp(170).toFloat())
            .rotation(if (toComputer) 12f else -10f)
            .alpha(0f).scaleX(.67f).scaleY(.67f)
            .setInterpolator(AccelerateInterpolator(1.35f))
            .setStartDelay(delay)
            .setDuration(320L).start()
    }

    private fun slide(card: View, x: Float, y: Float, angle: Float,
                      duration: Long, delay: Long = 0L) {
        card.animate().cancel()
        val destinationAngle = card.rotation
        card.alpha = 1f
        card.translationX = x
        card.translationY = y
        card.rotation = angle
        card.scaleX = .89f
        card.scaleY = .89f
        card.animate()
            .translationX(0f).translationY(0f)
            .rotation(destinationAngle)
            .alpha(1f).scaleX(1f).scaleY(1f)
            .setInterpolator(DecelerateInterpolator(1.55f))
            .setStartDelay(delay)
            .setDuration(duration).start()
    }
}
