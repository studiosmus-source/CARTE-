package it.studiosmus.carte.ui

import android.content.Context
import android.view.View
import android.view.ViewGroup

/**
 * Lays out the entire Scopa table in a measured grid, with every card fully
 * visible. No scrolling or overlap hides cards when the table grows.
 */
internal class AdaptiveCardGrid(
    context: Context,
    cardViews: List<View>
) : ViewGroup(context) {

    private fun dp(value: Int): Int =
        (value * resources.displayMetrics.density).toInt()

    private var geometry: CardGridPlan? = null

    init {
        setPadding(dp(3), dp(3), dp(3), dp(3))
        clipChildren = false
        clipToPadding = false
        cardViews.forEach { addView(it, LayoutParams(LayoutParams.WRAP_CONTENT, LayoutParams.WRAP_CONTENT)) }
    }

    override fun onMeasure(widthMeasureSpec: Int, heightMeasureSpec: Int) {
        val measuredW = if (MeasureSpec.getMode(widthMeasureSpec) == MeasureSpec.UNSPECIFIED)
            dp(320) else MeasureSpec.getSize(widthMeasureSpec)
        val measuredH = if (MeasureSpec.getMode(heightMeasureSpec) == MeasureSpec.UNSPECIFIED)
            dp(190) else MeasureSpec.getSize(heightMeasureSpec)
        val availableW = (measuredW - paddingLeft - paddingRight).coerceAtLeast(1)
        val availableH = (measuredH - paddingTop - paddingBottom).coerceAtLeast(1)
        geometry = if (childCount == 0) null else CardGridGeometry.fit(
            childCount,
            availableW,
            availableH,
            dp(78),
            dp(129),
            dp(6),
            dp(6)
        )
        val plan = geometry
        if (plan != null) {
            val childWidthSpec = MeasureSpec.makeMeasureSpec(plan.cardWidthPx, MeasureSpec.EXACTLY)
            val childHeightSpec = MeasureSpec.makeMeasureSpec(plan.cardHeightPx, MeasureSpec.EXACTLY)
            for (index in 0 until childCount)
                getChildAt(index).measure(childWidthSpec, childHeightSpec)
        }
        setMeasuredDimension(
            View.resolveSize(measuredW, widthMeasureSpec),
            View.resolveSize(measuredH, heightMeasureSpec)
        )
    }

    override fun onLayout(changed: Boolean, left: Int, top: Int, right: Int, bottom: Int) {
        val plan = geometry ?: return
        val innerWidth = width - paddingLeft - paddingRight
        val innerHeight = height - paddingTop - paddingBottom
        val usedHeight = plan.rows * plan.cardHeightPx +
            (plan.rows - 1) * plan.verticalGapPx
        val firstY = paddingTop + ((innerHeight - usedHeight) / 2).coerceAtLeast(0)
        for (row in 0 until plan.rows) {
            val indexFirst = row * plan.columns
            val number = minOf(plan.columns, childCount - indexFirst)
            if (number <= 0) break
            val usedWidth = number * plan.cardWidthPx +
                (number - 1) * plan.horizontalGapPx
            val firstX = paddingLeft + ((innerWidth - usedWidth) / 2).coerceAtLeast(0)
            val y = firstY + row * (plan.cardHeightPx + plan.verticalGapPx)
            for (column in 0 until number) {
                val x = firstX + column * (plan.cardWidthPx + plan.horizontalGapPx)
                getChildAt(indexFirst + column).layout(
                    x, y, x + plan.cardWidthPx, y + plan.cardHeightPx
                )
            }
        }
    }
}
