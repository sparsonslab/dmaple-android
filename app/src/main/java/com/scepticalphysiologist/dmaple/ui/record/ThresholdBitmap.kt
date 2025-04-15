package com.scepticalphysiologist.dmaple.ui.record

import android.graphics.Bitmap
import android.graphics.Canvas
import android.graphics.Color
import android.graphics.Rect
import androidx.core.graphics.set
import com.scepticalphysiologist.dmaple.etc.ntscGrey

class ThresholdBitmap(val input: Bitmap, val drawRoi: Rect) {

    // Overlay colors
    // --------------
    /** The transparent color for overlaying non-background pixels. */
    private val transparent = Color.argb(0, 0, 0, 0)
    /** The green highlight for overlaying background pixels. */
    private val highlight = Color.argb(125, 0, 255, 0)

    // Images
    // ------
    /***/
    private var luma = Array(input.width){FloatArray(input.height){ 0f } }
    private val overlay: Bitmap = input.copy(input.config, true)

    init {
        for(j in 0 until input.height)
            for(i in 0 until input.width)
                luma[i][j] = ntscGrey(input.getPixel(i, j))
    }

    fun updateThreshold(threshold: Float) {
        for(j in 0 until input.height)
            for(i in 0 until input.width)
                overlay[i, j] = if((luma[i][j] < threshold) xor highlightAbove) highlight else transparent
    }

    fun draw(canvas: Canvas) {
        canvas.drawBitmap(overlay, null, drawRoi, null)
    }

    companion object {

        /** Whether to highlight above threshold or below. */
        var highlightAbove: Boolean = true

        fun fromImage(image: Bitmap, r: Rect): ThresholdBitmap? {
            try {
                if(!r.intersect(Rect(0, 0, image.width, image.height))) return null
                return ThresholdBitmap(
                    input = Bitmap.createBitmap(image, r.left, r.top, r.width(), r.height()),
                    drawRoi = r
                )
            }catch(_: IllegalArgumentException) {}
            return null
        }
    }

}
