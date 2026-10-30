package it.studiosmus.carte.core

import kotlin.random.Random

/** Scopa for two players. Human is 0; computer is 1. Pure Kotlin, no Android dependencies. */
class ScopaGame(random: Random = Random.Default) {
    private val stock = Deck.shuffled(random).toMutableList()
    val hands = listOf(mutableListOf<Card>(), mutableListOf<Card>())
    val captured = listOf(mutableListOf<Card>(), mutableListOf<Card>())
    val table = mutableListOf<Card>()
    val scope = intArrayOf(0, 0)
    var turn: Int = 0
        private set
    var lastCapturer: Int? = null
        private set
    var finished: Boolean = false
        private set
    var lastMove: String = ""
        private set
    private var moves = 0

    init {
        repeat(4) { table.add(stock.removeAt(0)) }
        deal()
    }

    private fun deal() {
        repeat(3) {
            hands[0].add(stock.removeAt(0))
            hands[1].add(stock.removeAt(0))
        }
    }

    fun options(card: Card): List<List<Card>> = ScopaRules.captures(card, table)

    /** Returns false for illegal turn, missing card, or invalid capture selection. */
    fun play(player: Int, card: Card, capture: List<Card>? = null): Boolean {
        if (finished || player != turn || card !in hands[player]) return false
        val choices = options(card)
        val selected = when {
            choices.isEmpty() && capture == null -> null
            choices.isEmpty() -> return false
            capture == null && choices.size == 1 -> choices.first()
            capture == null -> return false
            else -> choices.firstOrNull { it.toSet() == capture.toSet() && it.size == capture.size }
                ?: return false
        }
        hands[player].remove(card)
        moves++
        if (selected == null) {
            table.add(card)
            lastMove = "${if (player == 0) "Tu" else "Computer"}: ${card.id}"
        } else {
            table.removeAll(selected.toSet())
            captured[player].add(card)
            captured[player].addAll(selected)
            lastCapturer = player
            // Final move's table clearance is not a scopa.
            if (table.isEmpty() && moves < 36) scope[player]++
            lastMove = "${if (player == 0) "Tu" else "Computer"}: presa di ${selected.size} carte"
        }
        turn = 1 - player
        if (hands.all { it.isEmpty() }) {
            if (stock.isNotEmpty()) {
                deal()
            } else {
                lastCapturer?.let { captured[it].addAll(table) }
                table.clear()
                finished = true
            }
        }
        return true
    }

    /** Baseline AI; uses only its own cards and public table. */
    fun aiMove(): Pair<Card, List<Card>?> {
        require(turn == 1 && !finished)
        val plays = hands[1].flatMap { card ->
            val choices = options(card)
            if (choices.isEmpty()) listOf(card to null)
            else choices.map { card to it }
        }
        return plays.maxBy { (card, taken) ->
            if (taken == null) -card.rank.number
            else taken.size * 5 + taken.count { it.suit == Suit.DENARI } * 4 +
                (if (table.size == taken.size) 10 else 0)
        }
    }

    fun score(): IntArray {
        require(finished)
        val result = scope.copyOf()
        val cards = captured.map { it.size }
        val coins = captured.map { pile -> pile.count { it.suit == Suit.DENARI } }
        val sevens = captured.map { pile -> pile.count { it.rank == Rank.SETTE } }
        fun award(a: Int, b: Int) {
            if (a > b) result[0]++ else if (b > a) result[1]++
        }
        award(cards[0], cards[1])
        award(coins[0], coins[1])
        award(sevens[0], sevens[1]) // Temporary simplified primiera, to be replaced.
        val setteBello = Card(Suit.DENARI, Rank.SETTE)
        result[if (setteBello in captured[0]) 0 else 1]++
        return result
    }
}
