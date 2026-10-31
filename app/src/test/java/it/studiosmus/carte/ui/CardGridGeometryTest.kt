package it.studiosmus.carte.ui

import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class CardGridGeometryTest {
    private fun geometry(count: Int, width: Int = 310, height: Int = 182) =
        CardGridGeometry.fit(count, width, height, 78, 129, 6, 6)

    @Test fun allTableSizesFitWithoutCropping() {
        for (count in 1..40) {
            val layout = geometry(count)
            assertTrue("Missing cards for count $count", layout.rows * layout.columns >= count)
            assertTrue("Width clipped for count $count",
                layout.columns * layout.cardWidthPx +
                    (layout.columns - 1) * layout.horizontalGapPx <= 310)
            assertTrue("Height clipped for count $count",
                layout.rows * layout.cardHeightPx +
                    (layout.rows - 1) * layout.verticalGapPx <= 182)
            assertTrue(layout.cardWidthPx in 1..78)
            assertTrue(layout.cardHeightPx in 1..129)
        }
    }

    @Test fun smallHandStaysOnOneLine() {
        for (count in 1..3) {
            val layout = geometry(count)
            assertEquals(1, layout.rows)
        }
    }

    @Test fun crowdedTableUsesMultipleRows() {
        val layout = geometry(10)
        assertTrue(layout.rows >= 2)
        assertTrue(layout.cardWidthPx >= 40)
    }

    @Test fun narrowScreenStillFitsAllCards() {
        for (count in listOf(4, 7, 10, 15, 20, 40)) {
            val plan = geometry(count, 230, 160)
            assertTrue(plan.columns * plan.cardWidthPx +
                (plan.columns - 1) * plan.horizontalGapPx <= 230)
            assertTrue(plan.rows * plan.cardHeightPx +
                (plan.rows - 1) * plan.verticalGapPx <= 160)
            assertTrue(plan.columns * plan.rows >= count)
        }
    }
}
