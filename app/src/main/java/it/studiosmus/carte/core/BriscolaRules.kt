package it.studiosmus.carte.core

object BriscolaRules {
    /** true when challenger wins a trick led by lead. */
    fun challengerWins(lead: Card, challenger: Card, trump: Suit): Boolean =
        when {
            challenger.suit == lead.suit -> challenger.rank.strength > lead.rank.strength
            challenger.suit == trump && lead.suit != trump -> true
            else -> false
        }

    fun trickPoints(a: Card, b: Card): Int = a.rank.briscolaPoints + b.rank.briscolaPoints
}
