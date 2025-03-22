package com.scepticalphysiologist.dmaple.geom


import android.graphics.Rect
import android.graphics.RectF
import android.view.Surface
import androidx.camera.core.AspectRatio

// -------------------------------------------------------------------------------------------------
// Conversion of geometric enum to real values
// -------------------------------------------------------------------------------------------------

/** Get the degree value of a Surface rotation enum. */
fun surfaceRotationDegrees(rotation: Int): Int {
    return when(rotation) {
        Surface.ROTATION_0 -> 0
        Surface.ROTATION_90 -> 90
        Surface.ROTATION_180 -> 180
        Surface.ROTATION_270 -> 270
        else -> 0
    }
}

/** Get the ratio value of an AspectRatio ratio enum. */
fun aspectRatioRatio(aspect: Int): Float {
    return when(aspect) {
        AspectRatio.RATIO_16_9 -> 16f/9f
        AspectRatio.RATIO_4_3 -> 4f/3f
        AspectRatio.RATIO_DEFAULT -> 1f
        else -> 1f
    }
}

// -------------------------------------------------------------------------------------------------
// Rectangle handling
// Rect(left, top, right, bottom)
// left < right, top < bottom
// -------------------------------------------------------------------------------------------------

fun validRect(rect: RectF): Rect {
    return Rect(
        minOf(rect.left, rect.right).toInt(),
        minOf(rect.bottom, rect.top).toInt(),
        maxOf(rect.left, rect.right).toInt(),
        maxOf(rect.top, rect.bottom).toInt()
    )
}

