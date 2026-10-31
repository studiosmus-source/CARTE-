package it.studiosmus.carte.ui

/**
 * Pure sizing for a Scopa table full of cards.
 *
 * All card faces must fit: no horizontal scrolling, no clipping and no
 * opaque overlaps. The artwork's native aspect ratio is always maintained.
 */
internal data class CardGridPlan(
    val columns: Int,
    val rows: Int,
    val cardWidthPx: Int,
    val cardHeightPx: Int,
    val horizontalGapPx: Int,
    val verticalGapPx: Int
)

internal object CardGridGeometry {
    const val ART_WIDTH = 1920
    const val ART_HEIGHT = 3173

    fun fit(
        count: Int,
        availableWidthPx: Int,
        availableHeightPx: Int,
        maxCardWidthPx: Int,
        maxCardHeightPx: Int,
        horizontalGapPx: Int,
        verticalGapPx: Int
    ): CardGridPlan {
        require(count in 1..40)
        require(availableWidthPx > 0 && availableHeightPx > 0)
        require(maxCardWidthPx > 0 && maxCardHeightPx > 0)
        val dx = horizontalGapPx.coerceAtLeast(0)
        val dy = verticalGapPx.coerceAtLeast(0)
        var best: CardGridPlan? = null
        for (columns in 1..count) {
            val rows = (count + columns - 1) / columns
            val widthRemaining = availableWidthPx - (columns - 1) * dx
            val heightRemaining = availableHeightPx - (rows - 1) * dy
            if (widthRemaining < columns || heightRemaining < rows) continue
            val widthFromHeight = ((heightRemaining / rows).toLong() *
                ART_WIDTH / ART_HEIGHT).toInt()
            val width = minOf(
                maxCardWidthPx,
                widthRemaining / columns,
                widthFromHeight,
                (maxCardHeightPx.toLong() * ART_WIDTH / ART_HEIGHT).toInt()
            )
            if (width < 1) continue
            val height = (width.toLong() * ART_HEIGHT / ART_WIDTH).toInt()
                .coerceAtLeast(1)
            val candidate = CardGridPlan(columns, rows, width, height, dx, dy)
            // Prefer large, readable cards; if sizes tie, fewer rows.
            if (best == null ||
                candidate.cardWidthPx > best.cardWidthPx ||
                (candidate.cardWidthPx == best.cardWidthPx && candidate.rows < best.rows)
            ) best = candidate
        }
        return requireNotNull(best) {
            "Not enough visible space for $count full card faces"
        }
    }
}
