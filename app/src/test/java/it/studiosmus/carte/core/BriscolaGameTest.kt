package it.studiosmus.carte.core

import kotlin.random.Random
import org.junit.Assert.*
import org.junit.Test

class BriscolaGameTest {
    @Test fun deckIsCompleteAndUnique() {
        assertEquals(40, Deck.ordered().size)
        assertEquals(40, Deck.ordered().toSet().size)
        assertEquals(120, Deck.ordered().sumOf { it.rank.briscolaPoints })
    }

    @Test fun trumpAndSameSuitRules() {
        assertTrue(BriscolaRules.challengerWins(Card(Suit.COPPE, Rank.DUE), Card(Suit.DENARI, Rank.QUATTRO), Suit.DENARI))
        assertFalse(BriscolaRules.challengerWins(Card(Suit.COPPE, Rank.ASSO), Card(Suit.COPPE, Rank.TRE), Suit.DENARI))
    }

    @Test fun fiveHundredCompleteGamesConserveCardsAndPoints() {
        repeat(500) { seed ->
            val game = BriscolaGame(Random(seed))
            var moves = 0
            while (!game.finished) {
                val player = game.turn
                val card = if (player == 1) game.aiCard() else game.hands[0].first()
                assertTrue(game.play(player, card))
                assertTrue(++moves <= 40)
            }
            assertEquals(40, moves)
            assertEquals(20, game.tricksPlayed)
            assertEquals(120, game.points.sum())
            assertEquals(0, game.remainingCards)
            assertTrue(game.hands.all { it.isEmpty() })
        }
    }

    @Test fun cannotPlayOutsideTurnOrUseCardNotInHand() {
        val game = BriscolaGame(Random(4))
        assertFalse(game.play(-1, game.hands[0].first()))
        assertFalse(game.play(2, game.hands[0].first()))
        assertFalse(game.play(1, game.hands[1].first()))
        assertFalse(game.play(0, game.hands[1].first()))
        assertEquals(0, game.tricksPlayed)
    }
    @Test fun exposedTrumpIsDrawnOnlyAfterCoveredStockRunsOut() {
        repeat(100) { seed ->
            val game = BriscolaGame(Random(seed))
            assertEquals(34, game.remainingCards)
            assertFalse(game.hands.flatten().contains(game.trumpCard))
            while (game.remainingCards > 1) {
                val player = game.turn
                val card = if (player == 1) game.aiCard() else game.hands[0].first()
                assertTrue(game.play(player, card))
            }
            assertEquals(1, game.remainingCards)
            assertFalse(game.hands.flatten().contains(game.trumpCard))
            // The exposed trump stays visible until the next trick is resolved.
            while (game.remainingCards > 0) {
                val player = game.turn
                val card = if (player == 1) game.aiCard() else game.hands[0].first()
                assertTrue(game.play(player, card))
            }
            assertEquals(0, game.remainingCards)
            assertTrue(game.hands.flatten().contains(game.trumpCard))
        }
    }

}
