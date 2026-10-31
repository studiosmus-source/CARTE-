package it.studiosmus.carte.ui

import android.content.Context
import android.view.View
import android.widget.FrameLayout

/**
 * A genuinely face-down card while the computer slides it onto the table.
 * The hidden face replaces the back only at the midpoint of the flip.
 */
class FlipCardView(context: Context, front: View, back: View) : FrameLayout(context) {
    private val face = front
    private val reverse = back
    private var revealed = false

    init {
        clipChildren = false
        clipToPadding = false
        addView(face, LayoutParams(LayoutParams.MATCH_PARENT, LayoutParams.MATCH_PARENT))
        addView(reverse, LayoutParams(LayoutParams.MATCH_PARENT, LayoutParams.MATCH_PARENT))
        face.visibility = View.INVISIBLE
        cameraDistance = resources.displayMetrics.density * 7000f
    }

    fun reveal(afterMs: Long = 210L) {
        if (revealed) return
        revealed = true
        postDelayed({
            if (!isAttachedToWindow) return@postDelayed
            reverse.animate().rotationY(90f).setDuration(75L).withEndAction {
                reverse.visibility = View.INVISIBLE
                face.visibility = View.VISIBLE
                face.rotationY = -90f
                face.animate().rotationY(0f).setDuration(95L).start()
            }.start()
        }, afterMs)
    }
}
