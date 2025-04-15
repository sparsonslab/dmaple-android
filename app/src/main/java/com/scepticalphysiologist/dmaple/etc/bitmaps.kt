package com.scepticalphysiologist.dmaple.etc

import android.graphics.Bitmap
import android.graphics.Matrix
import androidx.core.graphics.blue
import androidx.core.graphics.green
import androidx.core.graphics.red


/** Transform a bitmap with a matrix. */
fun transformBitmap(bitmap: Bitmap, transform: Matrix): Bitmap {
    return Bitmap.createBitmap(bitmap, 0, 0, bitmap.width, bitmap.height, transform, false)
}

/** Rotate a bitmap. */
fun rotateBitmap(bitmap: Bitmap, degrees: Int): Bitmap {
    return transformBitmap(bitmap, Matrix().also{it.setRotate(degrees.toFloat())})
}
