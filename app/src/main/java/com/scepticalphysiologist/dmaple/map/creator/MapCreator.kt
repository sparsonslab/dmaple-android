package com.scepticalphysiologist.dmaple.map.creator

import android.graphics.Bitmap

import android.graphics.Rect
import android.util.Size
import com.scepticalphysiologist.dmaple.etc.Point
import com.scepticalphysiologist.dmaple.map.MappingRoi
import java.io.File

import kotlin.math.ceil


abstract class MapCreator(val roi: MappingRoi) {

    abstract fun size(): Size

    abstract fun updateWithCameraBitmap(bitmap: Bitmap)

    abstract fun getMapBitmap(crop: Rect?, backing: IntArray, stepX: Int = 1, stepY: Int = 1): Bitmap?

    abstract fun saveAndClose(file: File? = null)

}

// -------------------------------------------------------------------------------------------------
// Useful functions
// -------------------------------------------------------------------------------------------------


fun rangeSize(range: Int, step: Int): Int {
    return ceil(range.toFloat() / step.toFloat()).toInt()
}


fun orderedX(pp: Pair<Point, Point>): Pair<Int, Int> {
    val p0 = pp.first.x.toInt()
    val p1 = pp.second.x.toInt()
    return Pair(minOf(p0, p1), maxOf(p0, p1))
}

fun orderedY(pp: Pair<Point, Point>): Pair<Int, Int> {
    val p0 = pp.first.y.toInt()
    val p1 = pp.second.y.toInt()
    return Pair(minOf(p0, p1), maxOf(p0, p1))
}



