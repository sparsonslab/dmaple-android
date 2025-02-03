package com.scepticalphysiologist.dmaple.etc

import android.widget.FrameLayout
import android.widget.LinearLayout
import androidx.constraintlayout.widget.ConstraintLayout


/** Get a layout with side padding. */
fun sidePaddedLayout(padding: Int): ConstraintLayout.LayoutParams {
    val layoutParams = ConstraintLayout.LayoutParams(
        ConstraintLayout.LayoutParams.MATCH_PARENT,
        ConstraintLayout.LayoutParams.WRAP_CONTENT
    )
    layoutParams.leftMargin = padding
    layoutParams.rightMargin = padding
    layoutParams.bottomMargin = 2
    layoutParams.topMargin = 2
    return layoutParams
}

/** Get a layout with side padding. */
fun paddedFrameLayout(padding: Int): FrameLayout.LayoutParams {
    val layoutParams = FrameLayout.LayoutParams(
        FrameLayout.LayoutParams.MATCH_PARENT,
        FrameLayout.LayoutParams.WRAP_CONTENT
    )
    layoutParams.leftMargin = padding
    layoutParams.rightMargin = padding
    layoutParams.bottomMargin = 2
    layoutParams.topMargin = 2
    return layoutParams
}


/**
 * Margined parameters for a standard linear or relative layout.
 *
 * Apply to a child view of the layout element.
 */
fun marginedLayoutParams(
    weight: Float = 1f,
    bottomMargin : Int = 2,
    topMargin : Int = 2,
    leftMargin : Int = 2,
    rightMargin : Int = 2,
) : LinearLayout.LayoutParams {
    val layoutParams = LinearLayout.LayoutParams(
        LinearLayout.LayoutParams.MATCH_PARENT,
        LinearLayout.LayoutParams.MATCH_PARENT,
        weight
    )
    layoutParams.bottomMargin = bottomMargin
    layoutParams.topMargin = topMargin
    layoutParams.rightMargin = rightMargin
    layoutParams.leftMargin = leftMargin
    return layoutParams
}