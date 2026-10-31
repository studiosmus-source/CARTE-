package it.studiosmus.carte.ui

import it.studiosmus.carte.core.Card
import it.studiosmus.carte.core.ScopaGame

/** Scopa engine ownership and computer turn sequencing, separate from the screen. */
class ScopaSession(private val changed: () -> Unit) {
    var game: ScopaGame? = null
        private set

    fun start() {
        game = ScopaGame()
        advanceComputer()
        changed()
    }

    fun stop() {
        game = null
    }

    fun options(card: Card): List<List<Card>> = game?.options(card).orEmpty()

    fun play(card: Card, capture: List<Card>?): Boolean {
        val match = game ?: return false
        if (!match.play(0, card, capture)) return false
        advanceComputer()
        changed()
        return true
    }

    private fun advanceComputer() {
        val match = game ?: return
        while (!match.finished && match.turn == 1) {
            val (card, capture) = match.aiMove()
            if (!match.play(1, card, capture)) break
        }
    }
}
