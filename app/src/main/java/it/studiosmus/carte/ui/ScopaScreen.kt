package it.studiosmus.carte.ui

import android.content.Context
import android.graphics.Color
import android.graphics.Typeface
import android.graphics.drawable.GradientDrawable
import android.view.Gravity
import android.view.View
import android.widget.Button
import android.widget.LinearLayout
import android.widget.ScrollView
import android.widget.TextView
import it.studiosmus.carte.core.Card

/** Scopa view composition; never mutates the engine directly. */
class ScopaScreen(
    private val context: Context,
    private val session: ScopaSession,
    private val renderer: CardRenderer,
    private val onBack: () -> Unit,
    private val onNewGame: () -> Unit,
    private val onPlay: (Card) -> Unit
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
            setPadding(dp(12), dp(12), dp(12), dp(16))
            background = GradientDrawable(GradientDrawable.Orientation.TL_BR, intArrayOf(Color.rgb(7, 30, 26), forest, Color.rgb(5, 25, 22)))
        }
        val scroll = ScrollView(context).apply { isFillViewport = true }
        scroll.addView(root)
        val header = LinearLayout(context).apply {
            orientation = LinearLayout.HORIZONTAL
            gravity = Gravity.CENTER_VERTICAL
        }
        header.addView(label("‹ MENU", 14f, gold, true).apply {
            setPadding(dp(8), dp(10), dp(8), dp(10))
            setOnClickListener { onBack() }
        })
        header.addView(label("SCOPA", 25f, ivory, true),
            LinearLayout.LayoutParams(0, -2, 1f))
        root.addView(header, LinearLayout.LayoutParams(-1, -2))
        root.addView(label("SCOPE · TU ${match.scope[0]} — ${match.scope[1]} COMPUTER", 15f, gold, true))
        root.addView(label("Carte raccolte: ${match.captured[0].size} — ${match.captured[1].size}", 12f, ivory))
        root.addView(label("AVVERSARIO · ${match.hands[1].size} carte", 12f, gold))
        root.addView(renderer.cardBackRow(match.hands[1].size))
        val felt = LinearLayout(context).apply {
            orientation = LinearLayout.VERTICAL
            gravity = Gravity.CENTER
            setPadding(dp(8), dp(12), dp(8), dp(12))
            background = panel(Color.rgb(18, 83, 62), 22, Color.rgb(121, 139, 82))
        }
        felt.addView(label("SUL TAVOLO", 13f, gold, true))
        if (match.table.isEmpty()) {
            felt.addView(label("Tavolo libero", 15f, ivory).apply {
                setPadding(dp(8), dp(24), dp(8), dp(24))
            })
        } else {
            felt.addView(renderer.cardRow(match.table))
        }
        root.addView(felt, LinearLayout.LayoutParams(-1, -2))
        root.addView(label(match.lastMove, 13f, gold))
        if (match.finished) {
            val score = match.score()
            val outcome = when {
                score[0] > score[1] -> "HAI VINTO!"
                score[0] < score[1] -> "HA VINTO IL COMPUTER"
                else -> "PAREGGIO"
            }
            root.addView(label(outcome, 26f, gold, true))
            root.addView(label("Punti finali: ${score[0]} — ${score[1]}", 20f, ivory))
            root.addView(Button(context).apply {
                text = "NUOVA PARTITA"
                setOnClickListener { onNewGame() }
            })
        } else {
            root.addView(label("LE TUE CARTE · TOCCA PER GIOCARE", 13f, gold, true))
            root.addView(renderer.cardRow(match.hands[0]) { card -> if (match.turn == 0) onPlay(card) })
        }
        return scroll
    }


}
