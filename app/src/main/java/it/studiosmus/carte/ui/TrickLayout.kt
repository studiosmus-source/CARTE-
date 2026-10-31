package it.studiosmus.carte.ui

import android.view.Gravity
import android.view.View
import android.widget.FrameLayout

/** Measured geometry for two played cards, independent of game rules and Activity. */
object TrickLayout {
    fun bind(table: FrameLayout, lead: View?, reply: View?, hasStock: Boolean, dp: (Int) -> Int,
             playedBy: Int = -1, collecting: Boolean = false, winner: Int = 0) {
        if (lead != null) {
            table.addView(lead)
            if (collecting) CardMotion.collect(lead, winner == 1, dp)
            else CardMotion.reveal(lead, playedBy == 1, dp)
        }
        if (reply != null) {
            table.addView(reply)
            if (collecting) CardMotion.collect(reply, winner == 1, dp)
            else CardMotion.reveal(reply, playedBy == 1, dp)
        }
        var previousWidth = -1
        var previousHeight = -1
        table.addOnLayoutChangeListener { _, left, top, right, bottom, _, _, _, _ ->
            val width = right - left
            val height = bottom - top
            if (width <= 0 || height <= 0 || (width == previousWidth && height == previousHeight)) {
                return@addOnLayoutChangeListener
            }
            previousWidth = width
            previousHeight = height
            val stock = if (hasStock) dp(125).coerceAtMost(width / 2) else dp(8)
            val available = (width - stock - dp(8)).coerceAtLeast(1)
            val cardWidth = minOf(dp(86), available, (height * 86 / 141).coerceAtLeast(1))
            val cardHeight = (cardWidth * 141f / 86f).toInt()
            val centerX = stock + available / 2
            val shift = if (reply != null) minOf(dp(20), available / 8) else 0
            fun place(card: View?, x: Int, offset: Int) {
                if (card == null) return
                card.layoutParams = FrameLayout.LayoutParams(cardWidth, cardHeight,
                    Gravity.TOP or Gravity.START).apply {
                    leftMargin = x.coerceIn(0, (width - cardWidth).coerceAtLeast(0))
                    topMargin = ((height - cardHeight) / 2 + offset)
                        .coerceIn(0, (height - cardHeight).coerceAtLeast(0))
                }
            }
            place(lead, centerX - cardWidth / 2 - shift, -dp(10))
            place(reply, centerX - cardWidth / 2 + shift, dp(16))
        }
    }
}
