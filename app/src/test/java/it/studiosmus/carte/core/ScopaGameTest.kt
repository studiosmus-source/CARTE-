package it.studiosmus.carte.core

import kotlin.random.Random
import org.junit.Assert.*
import org.junit.Test

class ScopaGameTest {
    @Test fun fiveHundredGamesPreserveAllCardsAndFinish() {
        repeat(500) { seed ->
            val game = ScopaGame(Random(seed))
            var moves = 0
            while (!game.finished) {
                val player = game.turn
                val card: Card
                val capture: List<Card>?
                if (player == 1) {
                    val move = game.aiMove()
                    card = move.first
                    capture = move.second
                } else {
                    card = game.hands[0].first()
                    capture = game.options(card).firstOrNull()
                }
                assertTrue("seed=$seed move=$moves", game.play(player, card, capture))
                assertTrue("Game never finished seed=$seed", ++moves <= 36)
                val all = game.hands.flatten() + game.table + game.captured.flatten()
                assertEquals("Duplicate cards seed=$seed", all.size, all.toSet().size)
                assertTrue("More than 40 cards seed=$seed", all.size <= 40)
            }
            assertEquals(36, moves)
            assertTrue(game.hands.all { it.isEmpty() })
            assertTrue(game.table.isEmpty())
            assertEquals(40, game.captured.sumOf { it.size })
            assertTrue(game.score().all { it >= 0 })
        }
    }

    @Test fun invalidPlayerAndTurnCannotChangeState() {
        val game = ScopaGame(Random(7))
        val card = game.hands[0].first()
        assertFalse(game.play(-1, card))
        assertFalse(game.play(2, card))
        assertFalse(game.play(1, card))
        assertEquals(0, game.turn)
        assertEquals(3, game.hands[0].size)
    }

    @Test fun ambiguousCaptureRequiresExplicitChoice() {
        // Exercise real shuffled deals until a legal ambiguous capture appears.
        var found = false
        for (seed in 0 until 300) {
            val game = ScopaGame(Random(seed))
            val card = game.hands[0].firstOrNull { game.options(it).size > 1 } ?: continue
            val original = game.hands[0].toList()
            assertFalse(game.play(0, card))
            assertEquals(original, game.hands[0])
            assertEquals(0, game.turn)
            found = true
            break
        }
        assertTrue("Expected an ambiguous opening capture", found)
    }
}
