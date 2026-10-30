package it.studiosmus.carte.core

import org.junit.Assert.*
import org.junit.Test

class BriscolaRulesTest {
    private fun card(suit: Suit, rank: Rank) = Card(suit, rank)

    @Test fun trumpAlwaysBeatsNonTrump() {
        val lead = card(Suit.COPPE, Rank.ASSO)
        val challenger = card(Suit.DENARI, Rank.DUE)
        assertTrue(BriscolaRules.challengerWins(lead, challenger, Suit.DENARI))
    }

    @Test fun differentNonTrumpSuitCannotWin() {
        assertFalse(BriscolaRules.challengerWins(
            card(Suit.COPPE, Rank.DUE), card(Suit.SPADE, Rank.ASSO), Suit.DENARI))
    }

    @Test fun higherCardOfLedSuitWins() {
        assertTrue(BriscolaRules.challengerWins(
            card(Suit.COPPE, Rank.RE), card(Suit.COPPE, Rank.TRE), Suit.DENARI))
        assertFalse(BriscolaRules.challengerWins(
            card(Suit.COPPE, Rank.ASSO), card(Suit.COPPE, Rank.TRE), Suit.DENARI))
    }

    @Test fun nonTrumpCannotBeatLedTrump() {
        assertFalse(BriscolaRules.challengerWins(
            card(Suit.DENARI, Rank.DUE), card(Suit.COPPE, Rank.ASSO), Suit.DENARI))
    }

    @Test fun pointValuesAndDeckTotal() {
        assertEquals(21, BriscolaRules.trickPoints(
            card(Suit.COPPE, Rank.ASSO), card(Suit.SPADE, Rank.TRE)))
        assertEquals(120, Deck.ordered().sumOf { it.rank.briscolaPoints })
        assertEquals(40, Deck.ordered().toSet().size)
    }
}
