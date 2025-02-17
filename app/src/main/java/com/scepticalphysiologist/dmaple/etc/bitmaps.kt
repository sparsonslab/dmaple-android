package com.scepticalphysiologist.dmaple.etc

import android.graphics.Bitmap
import android.graphics.Canvas
import android.graphics.Color
import android.graphics.Rect
import android.graphics.RectF
import androidx.core.graphics.blue
import androidx.core.graphics.get
import androidx.core.graphics.green
import androidx.core.graphics.red
import androidx.core.graphics.set


class ThresholdBitmap(val input: Bitmap, val drawRoi: Rect) {

    private val output: Bitmap = input.copy(input.config, true)
    private val under = Color.argb(0, 0, 0, 0)
    private val over = Color.argb(125, 0, 255, 0)

    companion object {
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
                output[i, j] = if(ntscGrey(input[i, j]) > threshold) under else over
    }

    fun draw(canvas: Canvas) {
        canvas.drawBitmap(output, null, drawRoi, null)
    }

}



/** Convert a color to its NTSC greyscale ('luminance') value.
 */
fun ntscGrey(color: Int): Float {
    return 0.299f * color.red + 0.587f * color.green + 0.114f * color.blue
}
