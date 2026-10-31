package it.studiosmus.carte.ui

import android.os.Handler
import android.os.Looper
import it.studiosmus.carte.core.Card
import it.studiosmus.carte.core.ScopaGame

/** Serializes Scopa engine events; input is locked during every visual transition. */
class ScopaSession(private val changed: () -> Unit) {
    private val handler = Handler(Looper.getMainLooper())
    var game: ScopaGame? = null
        private set
    var busy = false
        private set
    var dealing = false
        private set
    var preview: Card? = null
        private set
    var collecting = false
        private set

    fun start() {
        stop()
        val match = ScopaGame()
        game = match
        busy = true
        dealing = true
        changed()
        handler.postDelayed({
            if (game !== match) return@postDelayed
            dealing = false
            busy = false
            changed()
            advanceComputer()
        }, 620L)
    }

    fun stop() {
        handler.removeCallbacksAndMessages(null)
        game = null
        busy = false
        dealing = false
        preview = null
        collecting = false
    }

    fun options(card: Card): List<List<Card>> =
        if (busy || game?.turn != 0) emptyList() else game?.options(card).orEmpty()

    fun play(card: Card, capture: List<Card>?): Boolean {
        val match = game ?: return false
        if (busy || match.finished || match.turn != 0 || card !in match.hands[0]) return false
        val choices = match.options(card)
        if (choices.isEmpty() && capture != null) return false
        if (choices.size > 1 && capture == null) return false
        if (capture != null && choices.none { it.size == capture.size && it.toSet() == capture.toSet() }) return false
        queueMove(match, 0, card, capture)
        return true
    }

    private fun queueMove(match: ScopaGame, player: Int, card: Card, capture: List<Card>?) {
        if (game !== match || busy) return
        busy = true
        preview = card
        changed()
        handler.postDelayed({
            if (game !== match) return@postDelayed
            val oldHandSize = match.hands[0].size
            if (!match.play(player, card, capture)) {
                preview = null
                busy = false
                changed()
                return@postDelayed
            }
            preview = null
            collecting = capture != null
            changed()
            handler.postDelayed({
                if (game !== match) return@postDelayed
                collecting = false
                dealing = !match.finished && match.hands[0].size > oldHandSize
                changed()
                handler.postDelayed({
                    if (game !== match) return@postDelayed
                    dealing = false
                    busy = false
                    changed()
                    advanceComputer()
                }, if (dealing) 500L else 120L)
            }, if (collecting) 400L else 170L)
        }, 380L)
    }

    private fun advanceComputer() {
        val match = game ?: return
        if (busy || match.finished || match.turn != 1) return
        busy = true
        changed()
        handler.postDelayed({
            if (game !== match) return@postDelayed
            busy = false
            if (match.finished || match.turn != 1) return@postDelayed
            val (card, capture) = match.aiMove()
            queueMove(match, 1, card, capture)
        }, 650L)
    }
}
