package it.studiosmus.carte.ui

import android.content.Context
import android.graphics.Color
import android.graphics.Typeface
import android.graphics.drawable.GradientDrawable
import android.view.Gravity
import android.view.View
import android.widget.Button
import android.widget.FrameLayout
import android.widget.LinearLayout
import android.widget.TextView

/** Pure screen composition: state is owned by BriscolaSession; artwork by CardRenderer. */
class BriscolaScreen(
    private val context: Context,
    private val session: BriscolaSession,
    private val renderer: CardRenderer,
    private val onBack: () -> Unit,
    private val onSettings: () -> Unit,
    private val onNewGame: () -> Unit
) {
    private val ivory = Color.rgb(247, 233, 203)
    private val gold = Color.rgb(218, 174, 94)
    private val forest = Color.rgb(13, 54, 45)
    private fun dp(value: Int) = (value * context.resources.displayMetrics.density).toInt()
    private fun panel(color: Int, radius: Int = 22, stroke: Int = gold) =
        GradientDrawable().apply {
            setColor(color)
            cornerRadius = dp(radius).toFloat()
            setStroke(dp(1), stroke)
        }
    private fun label(value: String, size: Float, color: Int, bold: Boolean = false) =
        TextView(context).apply {
            text = value
            textSize = size
            setTextColor(color)
            gravity = Gravity.CENTER
            typeface = Typeface.create(Typeface.SERIF,
                if (bold) Typeface.BOLD else Typeface.NORMAL)
        }

    fun render(): View {
        val match = session.game ?: return View(context)
        val root = LinearLayout(context).apply {
            orientation = LinearLayout.VERTICAL
            gravity = Gravity.CENTER_HORIZONTAL
            setPadding(dp(10), dp(8), dp(10), dp(10))
            background = GradientDrawable(GradientDrawable.Orientation.TL_BR,
                intArrayOf(Color.rgb(24, 30, 26), Color.rgb(39, 43, 35), Color.rgb(20, 25, 23)))
        }
        val header = LinearLayout(context).apply {
            orientation = LinearLayout.HORIZONTAL
            gravity = Gravity.CENTER_VERTICAL
        }
        header.addView(label("‹", 30f, gold, true).apply {
            setPadding(dp(12), dp(4), dp(12), dp(4))
            setOnClickListener {
                onBack()
            }
        })
        header.addView(label("BRISCOLA", 19f, ivory, true),
            LinearLayout.LayoutParams(0, -2, 1f))
        header.addView(label("⚙", 23f, gold).apply {
            setPadding(dp(12), dp(4), dp(12), dp(4))
            setOnClickListener {
                // Finish the current trick before opening settings: a pending
                // callback must never replace the settings screen unexpectedly.
                if (!session.trickResolving && !session.computerPending) {
                    onSettings()
                }
            }
        })
        root.addView(header, LinearLayout.LayoutParams(-1, -2))
        root.addView(renderer.cardBackRow(match.hands[1].size))
        root.addView(label("COMPUTER  ·  ${match.points[1]} PUNTI", 13f, gold, true))

        val table = SalonTable(context)
        root.addView(table, LinearLayout.LayoutParams(-1, 0, 1f).apply {
            topMargin = dp(8)
            bottomMargin = dp(8)
        })
        if (match.remainingCards > 0) {
            table.addView(
                BriscolaStock(context, renderer, match.trumpCard, match.remainingCards),
                FrameLayout.LayoutParams(dp(150), dp(160),
                    Gravity.START or Gravity.CENTER_VERTICAL)
            )
        }
        val lead = match.lead
        val leadView = lead?.let { renderer.cardTile(it.second) }
        val replyView = session.trickPreview?.let { renderer.cardTile(it.second) }
        TrickLayout.bind(
            table, leadView, replyView, match.remainingCards > 0
        ) { dp(it) }
        val tableMessage = when {
            session.trickResolving -> "PRESA IN CORSO"
            lead?.first == 1 -> "IL COMPUTER HA GIOCATO"
            lead?.first == 0 -> "HAI GIOCATO"
            else -> match.lastTrick.ifEmpty { "TOCCA A TE" }
        }
        table.addView(label(tableMessage, 12f, ivory, true),
            FrameLayout.LayoutParams(-1, dp(38), Gravity.BOTTOM))
        root.addView(label("TU  ·  ${match.points[0]} PUNTI", 14f, gold, true))
        if (match.finished) {
            root.addView(label(match.result(), 24f, ivory, true))
            root.addView(Button(context).apply {
                text = "NUOVA PARTITA"
                setTextColor(ivory)
                background = panel(Color.rgb(70, 48, 33), 14, Color.rgb(124, 89, 55))
                setOnClickListener { onNewGame() }
            })
        } else {
            root.addView(renderer.cardRow(match.hands[0]) { card ->
                if (match.turn == 0 && !session.trickResolving) session.play(0, card)
            })
        }
        return root
    }

}
