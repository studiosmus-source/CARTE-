package it.studiosmus.carte.core

enum class Suit { COPPE, DENARI, BASTONI, SPADE }
enum class Rank(val number: Int, val briscolaPoints: Int, val strength: Int) {
    ASSO(1, 11, 10), DUE(2, 0, 1), TRE(3, 10, 9),
    QUATTRO(4, 0, 2), CINQUE(5, 0, 3), SEI(6, 0, 4),
    SETTE(7, 0, 5), FANTE(8, 2, 6), CAVALLO(9, 3, 7), RE(10, 4, 8)
}
data class Card(val suit: Suit, val rank: Rank) {
    val id: String get() = "${suit.name.lowercase()}_${rank.number.toString().padStart(2, '0')}"
}
object Deck {
    fun ordered(): List<Card> = Suit.entries.flatMap { suit -> Rank.entries.map { Card(suit, it) } }
    fun shuffled(random: kotlin.random.Random = kotlin.random.Random.Default): List<Card> = ordered().shuffled(random)
}
