package com.scepticalphysiologist.dmaple.ui.camera

import android.graphics.Bitmap
import android.graphics.Rect
import android.util.Size
import java.lang.IllegalArgumentException
import java.lang.IndexOutOfBoundsException
import java.nio.BufferOverflowException
import java.nio.IntBuffer
import kotlin.math.abs
import kotlin.math.ceil


abstract class MapCreator(val area: MappingRoi) {

    abstract fun size(): Size

    abstract fun updateWithCameraImage(bitmap: Bitmap)

    abstract fun getMapBitmap(crop: Rect?, stepX: Int = 1, stepY: Int = 1): Bitmap?

}

/** A map creator for development purposes, that simply creates the map from the pixels
 * along the seeding edge. */
class SubstituteMapCreator(roi: MappingRoi): MapCreator(roi) {

    // Map geometry
    // ------------
    /** The map seeding edge orientation within the input images. */
    private val isVertical: Boolean = roi.seedingEdge.isVertical()
    /** Long-axis coordinates of the seeding edge .*/
    private val pE: Pair<Int, Int>
    /** Short-axis coordinate of the seeding edge. */
    private val pL: Int
    /** Sample size of map - space and time. */
    private val ns: Int
    private var nt: Int = 0

    // Map data
    // --------
    // I did try using an ArrayDeque, but after 10+ minutes the dynamic memory allocation starts
    // to slow things (map update and image show) to a crawl.
    /** The buffer for incoming map data. */
    private val mapBuffer: IntBuffer
    /** If the end of the buffer has been reached. */
    private var endOfBuffer = false

    init {
        val edge = Point.ofRectEdge(roi, roi.seedingEdge)
        if(isVertical) {
            pE = orderedY(edge)
            pL = edge.first.x.toInt()
        } else {
            pE = orderedX(edge)
            pL = edge.first.y.toInt()
        }
        ns = abs(pE.first - pE.second)
        mapBuffer = IntBuffer.allocate(ns * MappingService.BUFFER_SIZE_PER_PIXEL)
    }

    /** The space-time size of the map (samples). */
    override fun size(): Size { return Size(ns, nt) }

    /** Update the map with a new camera frame. */
    override fun updateWithCameraImage(bitmap: Bitmap) {
        if(endOfBuffer) return
        try {
            (pE.first until pE.second).map { mapBuffer.put(
                if(isVertical) bitmap.getPixel(pL, it) else bitmap.getPixel(it, pL)
            )}
            nt += 1
        } catch(e: BufferOverflowException) { endOfBuffer = true }
    }

    /** Get the map as a bitmap (space = x/width, time = y/height).
     *
     * @param crop The area of the map to return.
     * @param stepX A step in the spatial pixels (for pixel skip).
     * @param stepY A step in the time pixels (for pixel skip).
     * */
    override fun getMapBitmap(crop: Rect?, stepX: Int, stepY: Int): Bitmap? {
        val area = Rect(0, 0, ns, nt)
        crop?.let { area.intersect(crop) }
        try {
            val bs = Size(rangeSize(area.width(), stepX), rangeSize(area.height(), stepY))
            val arr = IntArray(bs.width * bs.height)
            var k = 0
            for(j in area.top until area.bottom step stepY)
                for(i in area.left until area.right step stepX) {
                    arr[k] = mapBuffer[j * ns + i]
                    k += 1
                }
            return Bitmap.createBitmap(arr, bs.width, bs.height, Bitmap.Config.ARGB_8888)
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



