package com.scepticalphysiologist.dmaple.ui.camera

import android.graphics.Bitmap
import android.graphics.Rect
import android.util.Size
import java.lang.IllegalArgumentException
import java.lang.IndexOutOfBoundsException
import kotlin.math.abs

abstract class GutAnalyser(val area: MappingRoi) {

    abstract fun analyse(bitmap: Bitmap)

    abstract fun size(): Size

    abstract fun getImage(crop: Rect?): Bitmap?

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

    override fun getImage(crop: Rect?): Bitmap? {
        val area = Rect(0, 0, ns, nt)
        crop?.let { area.intersect(crop) }
        try {
            var k = 0
            val arr = IntArray(area.width() * area.height())
            for(j in area.top until area.bottom)
                for(i in area.left until area.right) {
                    arr[k] = map[j * ns + i]
                    k += 1
                }
            return Bitmap.createBitmap(arr, area.width(), area.height(), bconfig)
        }
        catch (e: IndexOutOfBoundsException) { return null }
        catch (e: IllegalArgumentException) {return null }
    }

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



