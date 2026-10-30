package it.studiosmus.carte.core

import kotlin.random.Random

/** Two-player Briscola. Player 0 is human; player 1 is computer. */
class BriscolaGame(random: Random = Random.Default) {
    private val stock = Deck.shuffled(random).toMutableList()
    val hands = listOf(mutableListOf<Card>(), mutableListOf<Card>())
    val points = intArrayOf(0, 0)
    val trumpCard: Card = stock.removeAt(0)
    val trump: Suit = trumpCard.suit
    var leader: Int = 0
        private set
    var turn: Int = 0
        private set
    var lead: Pair<Int, Card>? = null
        private set
    var finished: Boolean = false
        private set
    var tricksPlayed: Int = 0
        private set
    var lastTrick: String = ""
        private set

    init {
        repeat(3) {
            hands[0].add(stock.removeAt(0))
            hands[1].add(stock.removeAt(0))
        }
        require(stock.size == 33)
    }

    val remainingCards: Int get() = stock.size + if (trumpAvailable) 1 else 0
    private var trumpAvailable = true

    fun play(player: Int, card: Card): Boolean {
        if (finished || player !in 0..1 || turn != player || !hands[player].remove(card)) return false
        val first = lead
        if (first == null) {
            lead = player to card
            turn = 1 - player
            return true
        }
        val secondWins = BriscolaRules.challengerWins(first.second, card, trump)
        val winner = if (secondWins) player else first.first
        val score = BriscolaRules.trickPoints(first.second, card)
        points[winner] += score
        lastTrick = "Presa: ${if (winner == 0) "Tu" else "Computer"} (+$score)"
        tricksPlayed++
        lead = null
        leader = winner
        turn = winner
        // Winner draws first; the final exposed trump card is drawn last.
        draw(winner)
        draw(1 - winner)
        if (hands.all { it.isEmpty() }) finished = true
        return true
    }

    private fun draw(player: Int) {
        when {
            stock.isNotEmpty() -> hands[player].add(stock.removeAt(0))
            trumpAvailable -> {
                hands[player].add(trumpCard)
                trumpAvailable = false
            }
        }
    }

    fun aiCard(): Card {
        require(turn == 1 && !finished)
        val cards = hands[1]
        val first = lead?.second
        if (first != null) {
            val winning = cards.filter { BriscolaRules.challengerWins(first, it, trump) }
            if (winning.isNotEmpty() && (first.rank.briscolaPoints >= 3 || winning.any { it.suit != trump })) {
                return winning.minBy { it.rank.briscolaPoints * 20 + it.rank.strength + if (it.suit == trump) 30 else 0 }
            }
        }
        return cards.minBy { it.rank.briscolaPoints * 20 + it.rank.strength + if (it.suit == trump) 30 else 0 }
    }

    fun result(): String = when {
        !finished -> "Partita in corso"
        points[0] > points[1] -> "Hai vinto!"
        points[0] < points[1] -> "Ha vinto il computer"
        else -> "Pareggio!"
    }
}
