package com.scepticalphysiologist.dmaple.map.creator

import android.graphics.Bitmap
import android.graphics.Color
import android.graphics.Rect
import android.util.Size
import com.scepticalphysiologist.dmaple.MainActivity
import com.scepticalphysiologist.dmaple.etc.Point
import com.scepticalphysiologist.dmaple.io.FileBackedBuffer
import com.scepticalphysiologist.dmaple.map.MappingRoi
import java.io.File
import java.lang.IllegalArgumentException
import java.lang.IndexOutOfBoundsException
import kotlin.math.abs


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
    /** The buffer for incoming map data.
     *
     * I did try using an ArrayDeque, but after 10+ minutes the dynamic memory allocation starts
     * to slow things (map update and image show) to a crawl.
     * */
    private lateinit var mapBuffer: FileBackedBuffer<Int>

    // ---------------------------------------------------------------------------------------------
    // Creation and memory allocation
    // ---------------------------------------------------------------------------------------------

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
    }

    fun bytesPerTimeSample(): Int { return ns * 4 }

    fun allocateBufferAndBackUp(timeSamples: Int, writeFraction: Float) {
        println("allocation: samples = $timeSamples, fraction = $writeFraction")
        mapBuffer = FileBackedBuffer(
            capacity = timeSamples * ns,
            directory = MainActivity.storageDirectory!!,
            default = Color.BLACK,
            backUpFraction = writeFraction
        )
    }

    // ---------------------------------------------------------------------------------------------
    // Update and bitmap creation.
    // ---------------------------------------------------------------------------------------------

    /** The space-time size of the map (samples). */
    override fun size(): Size { return Size(ns, nt) }

    /** Update the map with a new camera frame. */
    override fun updateWithCameraBitmap(bitmap: Bitmap) {
        (pE.first until pE.second).map { mapBuffer.add(
            if(isVertical) bitmap.getPixel(pL, it) else bitmap.getPixel(it, pL)
        )}
        nt += 1
    }

    /** Get the map as a bitmap (space = x/width, time = y/height).
     *
     * @param crop The area of the map to return.
     * @param stepX A step in the spatial pixels (for pixel skip).
     * @param stepY A step in the time pixels (for pixel skip).
     * */
    override fun getMapBitmap(
        crop: Rect?,
        backing: IntArray,
        stepX: Int, stepY: Int,
    ): Bitmap? {
        if(!this::mapBuffer.isInitialized) return null
        try {
            // Only allow a valid area of the map to be returned,
            val area = Rect(0, 0, ns, nt)
            crop?.let { area.intersect(crop) }
            val bs = Size(rangeSize(area.width(), stepX), rangeSize(area.height(), stepY))
            if(bs.width * bs.height > backing.size) return null
            // Pass values from buffer to bitmap backing and return bitmap.
            var k = 0
            for(j in area.top until area.bottom step stepY)
                for(i in area.left until area.right step stepX) {
                    backing[k] = mapBuffer.get(j * ns + i)
                    k += 1
                }
            return Bitmap.createBitmap(backing, bs.width, bs.height, Bitmap.Config.ARGB_8888)
        }
        // On start and rare occasions these might be thrown.
        catch (_: IndexOutOfBoundsException) {}
        catch (_: IllegalArgumentException) {}
        catch (_: NullPointerException) {}
        return null
    }

    // ---------------------------------------------------------------------------------------------
    // Map save.
    // ---------------------------------------------------------------------------------------------

    override fun saveAndClose(file: File?) {

        // save
        mapBuffer.writeRemainingSamples()
        // stream samples from the buffer file into a jpeg/etc.
        // ?????

        // Release the buffer.
        mapBuffer.release()
    }

}

