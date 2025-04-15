package com.scepticalphysiologist.dmaple.ui.record

import android.graphics.Bitmap
import android.graphics.Canvas
import android.graphics.Color
import android.graphics.Rect
import androidx.core.graphics.set
import com.scepticalphysiologist.dmaple.etc.ntscGrey

/** Draws an overlay onto a canvas to highlight background (above or below threshold) pixels.
 *
 * @param input The bitmap of pixels to be highlighted.
 * @param drawRoi The region of the canvas on which to draw the overlay.
 */
class BackgroundHighlight(val input: Bitmap, val drawRoi: Rect) {

    // Overlay colors
    // --------------
    /** The colorless, transparent overlay color for foreground pixels. */
    private val transparent = Color.argb(0, 0, 0, 0)
    /** The green, semi-transparent overlay color for background pixels. */
    private val highlight = Color.argb(125, 0, 255, 0)

    // Images
    // ------
    /** Luminance values. */
    private var luma = Array(input.width){FloatArray(input.height){ 0f } }
    /** Overlay image to highlight background pixels. */
    private val overlay: Bitmap = input.copy(input.config, true)

    init {
        for(j in 0 until input.height)
            for(i in 0 until input.width)
                luma[i][j] = ntscGrey(input.getPixel(i, j))
    }

    /** Update the threshold between background and foreground. */
    fun updateThreshold(threshold: Float) {
        for(j in 0 until input.height)
            for(i in 0 until input.width)
                overlay[i, j] = if((luma[i][j] < threshold) xor highlightAbove) highlight else transparent
    }

    /** Draw the overlay. */
    fun draw(canvas: Canvas) {
        canvas.drawBitmap(overlay, null, drawRoi, null)
    }

    companion object {

        /** Whether background is above threshold. */
        var highlightAbove: Boolean = true

        /** Create an overlay from an image and a region of that image to overlay. */
        fun fromImage(image: Bitmap, r: Rect): BackgroundHighlight? {
            try {
                if(!r.intersect(Rect(0, 0, image.width, image.height))) return null
                return BackgroundHighlight(
                    input = Bitmap.createBitmap(image, r.left, r.top, r.width(), r.height()),
                    drawRoi = r
                )
            }catch(_: IllegalArgumentException) {}
            return null
        }
    }

}
