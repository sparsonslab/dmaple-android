package com.scepticalphysiologist.dmaple.map.creator

import android.graphics.Bitmap
import android.graphics.Color

/** Create a bitmap of the given dimensions and background color. */
fun createBitmap(w: Int, h: Int, background: Int): Bitmap {
    val bm = Bitmap.createBitmap(w, h, Bitmap.Config.ARGB_8888)
    for(i in 0 until w) for(j in 0 until h) bm.setPixel(i, j, background)
    return bm
}

/** Swap the black and white values in a bitmap. */
fun invertBitmap(image: Bitmap) {
    for(i in 0 until image.width)
        for(j in 0 until image.height)
            image.setPixel(
                i, j, if(image.getPixel(i, j) == Color.BLACK) Color.WHITE else Color.BLACK
            )
}

/** Paint a slice through a bitmap. */
fun paintSlice(image: Bitmap, i: Int, cent: Int, width: Int, value: Int, horizontal: Boolean) {
    val p = cent - width / 2
    val q = p + width - 1
    if(horizontal) for(j in p..q) image.setPixel(j, i, value)
    else for(j in p..q) image.setPixel(i, j, value)
}

/** Create a series of bitmaps of a horizontal gut (white against black) with diameter
 * varying from frame to frame. */
fun horizontalGutSeries(
    diameters: List<Int>,
    fieldWidth: Int,
): List<Bitmap> {
    val fieldHeight = diameters.max() + 50
    val cent = fieldHeight / 2
    return diameters.map{ diam ->
        createBitmap(fieldWidth, fieldHeight, Color.BLACK).also { image ->
            for(i in 0 until fieldWidth) paintSlice(
                image, i, cent, diam, Color.WHITE, false
            )
        }
    }
}



