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
    private val onPlay: (Card) -> Unit,
    private val fireplaceLight: Float = 0f
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
            // Cards travel between deck, table and hand across sibling bounds.
            clipChildren = false
            clipToPadding = false
            setPadding(dp(7), dp(10), dp(7), dp(14))
            background = GradientDrawable(GradientDrawable.Orientation.TL_BR, intArrayOf(Color.rgb(24, 30, 26), Color.rgb(39, 43, 35), Color.rgb(20, 25, 23)))
        }
        val scroll = ScrollView(context).apply {
            isFillViewport = true
            clipChildren = false
            clipToPadding = false
        }
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
        val opponentBackRow = renderer.cardBackRow(match.hands[1].size)
        root.addView(opponentBackRow)
        val felt = SalonTable(context, fireplaceLight).apply {
            minimumHeight = dp(365)
        }
        // The deck has a real location on the felt, so distribution can
        // originate from that location rather than from offscreen.
        val stockView = if (match.remainingCards > 0 || session.dealing) {
            renderer.cardBackTile(87).also { stock ->
                felt.addView(stock, android.widget.FrameLayout.LayoutParams(
                    dp(42), dp(68), Gravity.TOP or Gravity.END).apply {
                    rightMargin = dp(15)
                    topMargin = dp(26)
                })
            }
        } else null
        val tableContent = LinearLayout(context).apply {
            orientation = LinearLayout.VERTICAL
            gravity = Gravity.CENTER
            // Leave the full width for table cards on narrow Android devices.
            setPadding(dp(8), dp(8), dp(8), dp(8))
        }
        tableContent.addView(label(
            when {
                session.dealing -> "DISTRIBUZIONE"
                session.collecting -> "RACCOLTA DELLA PRESA"
                session.preview != null -> "CARTA IN GIOCO"
                else -> "SUL TAVOLO"
            }, 12f, gold, true))
        if (match.table.isEmpty()) {
            tableContent.addView(label("Tavolo libero", 15f, ivory).apply {
                setPadding(dp(8), dp(30), dp(8), dp(30))
            })
        } else {
            tableContent.addView(renderer.tableCards(match.table),
                LinearLayout.LayoutParams(-1, dp(218)))
        }
        felt.addView(tableContent, android.widget.FrameLayout.LayoutParams(
            -1, -1, Gravity.CENTER))
        if (session.collecting) {
            val captured = renderer.tableCards(
                session.collectedCards, collectTo = session.captureWinner == 1
            )
            felt.addView(captured, android.widget.FrameLayout.LayoutParams(
                -1, dp(218), Gravity.CENTER))
        }
        val flyingCard = session.preview?.let { card ->
            renderer.playedCard(card, computer = match.turn == 1).also {
                felt.addView(it, android.widget.FrameLayout.LayoutParams(
                    dp(65), dp(108), Gravity.CENTER))
            }
        }
        if (flyingCard != null && match.turn == 1) {
            val sourceIndex = match.hands[1].indexOf(session.preview)
            val source = (opponentBackRow as? LinearLayout)
                ?.takeIf { sourceIndex in 0 until it.childCount }
                ?.getChildAt(sourceIndex)
            if (source != null) {
                source.alpha = 0f
                CardMotion.playFromHand(flyingCard, source)
            } else CardMotion.reveal(flyingCard, true, ::dp)
        }
        session.scopaFlashPlayer?.let { who ->
            felt.addView(label(
                if (who == 0) "SCOPA!  +1 PER TE" else "SCOPA!  +1 COMPUTER",
                23f, ivory, true
            ).apply {
                setPadding(dp(10), dp(8), dp(10), dp(8))
                background = panel(Color.argb(225, 103, 47, 27), 11)
                elevation = dp(14).toFloat()
            }, android.widget.FrameLayout.LayoutParams(-1, dp(58),
                Gravity.TOP or Gravity.CENTER_HORIZONTAL).apply {
                leftMargin = dp(16)
                rightMargin = dp(16)
                topMargin = dp(22)
            })
        }
        root.addView(felt, LinearLayout.LayoutParams(-1, dp(365)))
        root.addView(label(
            if (session.dealing) "Si distribuiscono le carte…"
            else if (session.collecting) "Si raccolgono le carte…"
            else match.lastMove, 13f, gold))
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
                setTextColor(ivory)
                background = panel(Color.rgb(70, 48, 33), 14, Color.rgb(124, 89, 55))
                setOnClickListener { onNewGame() }
            })
        } else {
            root.addView(label("LE TUE CARTE · TOCCA PER GIOCARE", 13f, gold, true))
            val playerRow = renderer.cardRow(match.hands[0],
                { card -> if (match.turn == 0 && !session.busy) onPlay(card) })
            root.addView(playerRow)
            if (flyingCard != null && match.turn == 0) {
                val sourceIndex = match.hands[0].indexOf(session.preview)
                val source = (playerRow as? android.widget.HorizontalScrollView)
                    ?.getChildAt(0)?.let { it as? LinearLayout }
                    ?.takeIf { sourceIndex in 0 until it.childCount }
                    ?.getChildAt(sourceIndex)
                if (source != null) {
                    source.alpha = 0f
                    CardMotion.playFromHand(flyingCard, source)
                } else CardMotion.reveal(flyingCard, false, ::dp)
            }
            if (session.dealing && stockView != null) {
                (playerRow as? android.widget.HorizontalScrollView)
                    ?.getChildAt(0)?.let { hand ->
                        (hand as? LinearLayout)?.let { tiles ->
                            for (index in 0 until tiles.childCount) {
                                CardMotion.dealFromStock(tiles.getChildAt(index),
                                    stockView, ::dp, index * 110L + 55L)
                            }
                        }
                    }
            }
        }
        if (session.dealing && stockView != null) {
            (opponentBackRow as? LinearLayout)?.let { row ->
                for (index in 0 until row.childCount) {
                    CardMotion.dealFromStock(row.getChildAt(index), stockView,
                        ::dp, index * 110L)
                }
            }
        }
        return scroll
    }


}
