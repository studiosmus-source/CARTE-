package it.studiosmus.carte.ui

import android.view.View

/** Lightweight card motion; no timers, bitmap allocations or engine mutations. */
object CardMotion {
    fun reveal(card: View?, fromAbove: Boolean, dp: (Int) -> Int) {
        if (card == null) return
        card.alpha = 0f
        card.translationY = if (fromAbove) -dp(24).toFloat() else dp(24).toFloat()
        card.animate().alpha(1f).translationY(0f).setDuration(230L).start()
    }
}
