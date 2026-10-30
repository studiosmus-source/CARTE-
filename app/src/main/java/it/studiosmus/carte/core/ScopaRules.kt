package it.studiosmus.carte.core

object ScopaRules {
    /** Exact-rank captures take precedence over sums. */
    fun captures(card: Card, table: List<Card>): List<List<Card>> {
        val exact = table.filter { it.rank.number == card.rank.number }
        if (exact.isNotEmpty()) return exact.map { listOf(it) }
        val result = mutableListOf<List<Card>>()
        fun walk(index: Int, chosen: List<Card>, total: Int) {
            if (total == card.rank.number) {
                if (chosen.isNotEmpty()) result.add(chosen)
                return
            }
            if (total > card.rank.number) return
            for (i in index until table.size) {
                walk(i + 1, chosen + table[i], total + table[i].rank.number)
            }
        }
        walk(0, emptyList(), 0)
        return result
    }
}
