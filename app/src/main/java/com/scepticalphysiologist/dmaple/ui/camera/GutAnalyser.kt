package com.scepticalphysiologist.dmaple.ui.camera

import android.graphics.Bitmap
import android.graphics.Rect
import android.util.Size
import java.lang.IllegalArgumentException
import java.lang.IndexOutOfBoundsException
import kotlin.math.abs
import kotlin.math.ceil


abstract class GutAnalyser(val area: MappingRoi) {

    abstract fun analyse(bitmap: Bitmap)

    abstract fun size(): Size

    abstract fun getImage(crop: Rect?, stepX: Int = 1, stepY: Int = 1): Bitmap?

}


class GutMapper(roi: MappingRoi): GutAnalyser(roi) {


    val map = ArrayDeque<Int>()
    val bconfig = Bitmap.Config.ARGB_8888

    val isVertical: Boolean
    /** Coordinates at edge .*/
    val pE: Pair<Int, Int>

    /** Edge coordinate*/
    val pL: Int

    /** Sample size of map - space and time. */
    val ns: Int
    var nt: Int = 0

    init {

        isVertical = roi.seedingEdge.isVertical()
        val edge = Point.ofRectEdge(roi, roi.seedingEdge)

        if(isVertical) {
            pE = orderedY(edge)
            pL = edge.first.x.toInt()
        } else {
            pE = orderedX(edge)
            pL = edge.first.y.toInt()
        }

        ns = abs(pE.first - pE.second)
    }

    override fun analyse(bm: Bitmap) {
        (pE.first until pE.second).map { map.add(
            if(isVertical) bm.getPixel(pL, it) else bm.getPixel(it, pL)
        )}
        nt += 1
    }

    override fun size(): Size {
        return Size(ns, nt)
    }

    override fun getImage(crop: Rect?, stepX: Int, stepY: Int): Bitmap? {
        val area = Rect(0, 0, ns, nt)
        crop?.let { area.intersect(crop) }
        try {
            val bs = Size(rangeSize(area.width(), stepX), rangeSize(area.height(), stepY))
            val arr = IntArray(bs.width * bs.height)
            var k = 0
            for(j in area.top until area.bottom step stepY)
                for(i in area.left until area.right step stepX) {
                    arr[k] = map[j * ns + i]
                    k += 1
                }
            return Bitmap.createBitmap(arr, bs.width, bs.height, bconfig)
        }
        catch (e: IndexOutOfBoundsException) { return null }
        catch (e: IllegalArgumentException) { return null }
    }

}

private fun rangeSize(range: Int, step: Int): Int {
    //return range.floorDiv(step)
    return ceil(range.toFloat() / step.toFloat()).toInt()
}


private fun orderedX(pp: Pair<Point, Point>): Pair<Int, Int> {
    val p0 = pp.first.x.toInt()
    val p1 = pp.second.x.toInt()
    return Pair(minOf(p0, p1), maxOf(p0, p1))
}

private fun orderedY(pp: Pair<Point, Point>): Pair<Int, Int> {
    val p0 = pp.first.y.toInt()
    val p1 = pp.second.y.toInt()
    return Pair(minOf(p0, p1), maxOf(p0, p1))
}



