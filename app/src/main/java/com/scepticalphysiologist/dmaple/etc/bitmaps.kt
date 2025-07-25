// Copyright (c) 2025-2025. Dr Sean Paul Parsons. All rights reserved.

package com.scepticalphysiologist.dmaple.etc

import android.graphics.Bitmap
import android.graphics.Matrix

/** Transform a bitmap with a matrix. */
fun transformBitmap(bitmap: Bitmap, transform: Matrix): Bitmap {
    return Bitmap.createBitmap(bitmap, 0, 0, bitmap.width, bitmap.height, transform, false)
}

/** Rotate a bitmap. */
fun rotateBitmap(bitmap: Bitmap, degrees: Int): Bitmap {
    return transformBitmap(bitmap, Matrix().also{it.setRotate(degrees.toFloat())})
}
