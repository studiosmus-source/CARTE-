package it.studiosmus.carte.ui

/** Pure screen-coordinate transformation used by the Android card animations. */
internal object CardMotionGeometry {
    fun stockToCard(
        stockLeft: Float, stockTop: Float, stockWidth: Float, stockHeight: Float,
        cardLeft: Float, cardTop: Float, cardWidth: Float, cardHeight: Float
    ): Pair<Float, Float> =
        (stockLeft + stockWidth / 2f - cardLeft - cardWidth / 2f) to
            (stockTop + stockHeight / 2f - cardTop - cardHeight / 2f)
}
