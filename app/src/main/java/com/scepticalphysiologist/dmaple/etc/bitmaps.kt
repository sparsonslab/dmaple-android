package com.scepticalphysiologist.dmaple.etc

import android.graphics.Bitmap
import android.graphics.Canvas
import android.graphics.Color
import android.graphics.Matrix
import android.graphics.Rect
import android.graphics.RectF
import androidx.core.graphics.blue
import androidx.core.graphics.get
import androidx.core.graphics.green
import androidx.core.graphics.red
import androidx.core.graphics.set


class ThresholdBitmap(val input: Bitmap, val drawRoi: Rect) {

    private val output: Bitmap = input.copy(input.config, true)
    private val neutral = Color.argb(0, 0, 0, 0)
    private val highlight = Color.argb(125, 0, 255, 0)

    companion object {

        /** Whether to highlight above threshold or below. */
        var highlightAbove: Boolean = true

        fun fromImage(image: Bitmap, roi: RectF): ThresholdBitmap? {
            try {
                val r = validRect(roi)
                return ThresholdBitmap(
                    input = Bitmap.createBitmap(image, r.left, r.top, r.width(), r.height()),
                    drawRoi = r
                )
            }catch(_: IllegalArgumentException) {}
            return null
        }
    }

    fun updateThreshold(threshold: Float) {
        for(i in 0 until input.width)
            for(j in 0 until input.height)
                output[i, j] = if((ntscGrey(input[i, j]) < threshold) xor highlightAbove) highlight else neutral
    }

    fun draw(canvas: Canvas) {
        canvas.drawBitmap(output, null, drawRoi, null)
    }

}

/** Transform a bitmap with a matrix. */
fun transformBitmap(bitmap: Bitmap, transform: Matrix): Bitmap {
    return Bitmap.createBitmap(bitmap, 0, 0, bitmap.width, bitmap.height, transform, false)
}

/** Rotate a bitmap. */
fun rotateBitmap(bitmap: Bitmap, degrees: Int): Bitmap {
    return transformBitmap(bitmap, Matrix().also{it.setRotate(degrees.toFloat())})
}

/** Convert a color to its NTSC greyscale ('luminance') value.
 */
fun ntscGrey(color: Int): Float {
    return 0.299f * color.red + 0.587f * color.green + 0.114f * color.blue
}
