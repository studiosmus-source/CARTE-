package it.studiosmus.carte.ui

import android.content.Context
import android.graphics.Color
import android.graphics.Typeface
import android.view.Gravity
import android.widget.FrameLayout
import android.widget.TextView
import it.studiosmus.carte.core.Card

/**
 * Correct Briscola stock: exposed trump turned sideways under the face-down
 * pack, with the remaining-card count unobtrusively below it.
 */
class BriscolaStock(
    context: Context,
    renderer: CardRenderer,
    trumpCard: Card,
    remaining: Int
) : FrameLayout(context) {
    private fun dp(value: Int) = (value * resources.displayMetrics.density).toInt()
    init {
        clipChildren = false
        clipToPadding = false
        if (remaining > 0) {
            val trump = renderer.cardTile(trumpCard).apply {
                rotation = 90f
                elevation = dp(3).toFloat()
            }
            addView(trump, LayoutParams(dp(66), dp(109), Gravity.TOP or Gravity.START).apply {
                leftMargin = dp(66)
                topMargin = dp(18)
            })
            if (remaining > 1) {
                val deck = renderer.cardBackRow(1)
                deck.elevation = dp(7).toFloat()
                addView(deck, LayoutParams(dp(86), dp(116), Gravity.TOP or Gravity.START).apply {
                    leftMargin = dp(8)
                    topMargin = dp(16)
                })
            }
            addView(TextView(context).apply {
                text = "$remaining"
                textSize = 12f
                typeface = Typeface.create(Typeface.SERIF, Typeface.NORMAL)
                setTextColor(Color.rgb(221, 203, 162))
                gravity = Gravity.CENTER
                contentDescription = "$remaining carte nel mazzo"
            }, LayoutParams(dp(38), dp(24), Gravity.BOTTOM or Gravity.START).apply {
                leftMargin = dp(30)
            })
        }
    }
}
