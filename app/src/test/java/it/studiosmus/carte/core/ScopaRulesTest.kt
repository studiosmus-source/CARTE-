package it.studiosmus.carte.core

import org.junit.Assert.*
import org.junit.Test

class ScopaRulesTest {
    private fun card(suit: Suit, rank: Rank) = Card(suit, rank)

    @Test fun exactRankMustOverrideSum() {
        val five = card(Suit.COPPE, Rank.CINQUE)
        val two = card(Suit.SPADE, Rank.DUE)
        val three = card(Suit.BASTONI, Rank.TRE)
        val result = ScopaRules.captures(card(Suit.DENARI, Rank.CINQUE), listOf(two, five, three))
        assertEquals(listOf(listOf(five)), result)
    }

    @Test fun multipleEqualRankCardsAreAlternativeSingleCaptures() {
        val a = card(Suit.COPPE, Rank.QUATTRO)
        val b = card(Suit.SPADE, Rank.QUATTRO)
        val result = ScopaRules.captures(card(Suit.DENARI, Rank.QUATTRO), listOf(a, b))
        assertEquals(2, result.size)
        assertEquals(setOf(listOf(a), listOf(b)), result.toSet())
    }

    @Test fun allValidCombinationsAreEnumeratedWithoutDuplicates() {
        val one = card(Suit.COPPE, Rank.ASSO)
        val two = card(Suit.SPADE, Rank.DUE)
        val three = card(Suit.BASTONI, Rank.TRE)
        val four = card(Suit.DENARI, Rank.QUATTRO)
        val result = ScopaRules.captures(card(Suit.COPPE, Rank.SETTE), listOf(one, two, three, four))
        assertEquals(setOf(setOf(three, four), setOf(one, two, four)), result.map { it.toSet() }.toSet())
    }

    @Test fun noMatchingCaptureReturnsEmpty() {
        assertTrue(ScopaRules.captures(card(Suit.COPPE, Rank.ASSO),
            listOf(card(Suit.DENARI, Rank.CINQUE))).isEmpty())
    }
}
