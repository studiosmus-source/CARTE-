package it.studiosmus.carte.ui

import org.junit.Assert.assertEquals
import org.junit.Test

class CardMotionGeometryTest {
    @Test fun mapsPhysicalDeckCenterToCardCenter() {
        val (dx, dy) = CardMotionGeometry.stockToCard(
            240f, 350f, 60f, 90f,
            60f, 1080f, 78f, 129f
        )
        assertEquals(171f, dx, .001f)
        assertEquals(-749.5f, dy, .001f)
    }

    @Test fun independentOfPhoneDensityAndOrientation() {
        val a = CardMotionGeometry.stockToCard(200f, 100f, 50f, 80f,
            120f, 300f, 50f, 80f)
        val b = CardMotionGeometry.stockToCard(400f, 200f, 100f, 160f,
            240f, 600f, 100f, 160f)
        assertEquals(a.first * 2, b.first, .001f)
        assertEquals(a.second * 2, b.second, .001f)
    }
}
