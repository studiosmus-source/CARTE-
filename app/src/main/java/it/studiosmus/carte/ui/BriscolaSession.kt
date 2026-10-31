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
    var dealing = false
        private set
    var collecting = false
        private set
    var lastWinner = 0
        private set
    var playedBy = -1
        private set

    fun start() {
        stop()
        game = BriscolaGame()
        dealing = true
        changed()
        handler.postDelayed({
            if (game != null) {
                dealing = false
                changed()
                advanceComputer()
            }
        }, 600L)
    }

    fun stop() {
        handler.removeCallbacksAndMessages(null)
        computerPending = false
        trickPreview = null
        trickResolving = false
        dealing = false
        collecting = false
        lastWinner = 0
        playedBy = -1
        game = null
    }

    fun play(player: Int, card: Card) {
        val match = game ?: return
        if (dealing || collecting || trickResolving || match.finished || match.turn != player) return
        val first = match.lead
        if (first == null) {
            if (match.play(player, card)) {
                playedBy = player
                changed()
                if (match.turn == 1) advanceComputer()
            }
            return
        }
        trickPreview = first.second to card
        playedBy = player
        trickResolving = true
        changed()
        handler.postDelayed({
            if (game !== match) return@postDelayed
            if (!match.play(player, card)) {
                trickPreview = null
                trickResolving = false
                changed()
                return@postDelayed
            }
            collecting = true
            lastWinner = match.leader
            trickPreview = null
            changed()
            handler.postDelayed({
                if (game !== match) return@postDelayed
                collecting = false
                trickResolving = false
                dealing = !match.finished && match.remainingCards > 0
                changed()
                if (dealing) {
                    handler.postDelayed({
                        if (game !== match) return@postDelayed
                        dealing = false
                        changed()
                        advanceComputer()
                    }, 450L)
                } else advanceComputer()
            }, 350L)
        }, 1000L)
    }

    fun advanceComputer() {
        val match = game ?: return
        if (match.finished || match.turn != 1 || computerPending || trickResolving || dealing || collecting) return
        computerPending = true
        handler.postDelayed({
            computerPending = false
            if (game !== match || match.finished || match.turn != 1) return@postDelayed
            play(1, match.aiCard())
        }, 850L)
    }
}
