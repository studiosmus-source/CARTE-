package it.studiosmus.carte.ui

import android.os.Handler
import android.os.Looper
import it.studiosmus.carte.core.BriscolaGame
import it.studiosmus.carte.core.Card

/** Owns Briscola turns and delayed actions, independent of Activity rendering. */
class BriscolaSession(private val changed: () -> Unit) {
    private val handler = Handler(Looper.getMainLooper())
    var game: BriscolaGame? = null
        private set
    var computerPending = false
        private set
    var trickPreview: Pair<Card, Card>? = null
        private set
    var trickResolving = false
        private set

    fun start() {
        stop()
        game = BriscolaGame()
        advanceComputer()
        changed()
    }

    fun stop() {
        handler.removeCallbacksAndMessages(null)
        computerPending = false
        trickPreview = null
        trickResolving = false
        game = null
    }

    fun play(player: Int, card: Card) {
        val match = game ?: return
        if (trickResolving || match.finished || match.turn != player) return
        val first = match.lead
        if (first == null) {
            if (match.play(player, card)) {
                changed()
                if (match.turn == 1) advanceComputer()
            }
            return
        }
        trickPreview = first.second to card
        trickResolving = true
        changed()
        handler.postDelayed({
            if (game !== match) return@postDelayed
            match.play(player, card)
            trickPreview = null
            trickResolving = false
            changed()
            if (match.turn == 1) advanceComputer()
        }, 1000L)
    }

    fun advanceComputer() {
        val match = game ?: return
        if (match.finished || match.turn != 1 || computerPending || trickResolving) return
        computerPending = true
        handler.postDelayed({
            computerPending = false
            if (game !== match || match.finished || match.turn != 1) return@postDelayed
            play(1, match.aiCard())
        }, 850L)
    }
}
