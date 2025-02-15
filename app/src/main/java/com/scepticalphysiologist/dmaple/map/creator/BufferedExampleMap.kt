package com.scepticalphysiologist.dmaple.map.creator

import android.graphics.Bitmap
import android.graphics.Rect
import android.util.Size
import com.scepticalphysiologist.dmaple.etc.Point
import com.scepticalphysiologist.dmaple.map.MappingRoi
import java.io.File
import java.lang.IllegalArgumentException
import java.lang.IndexOutOfBoundsException
import java.nio.ByteBuffer
import kotlin.math.abs

class BufferedExampleMap: MapCreator()  {

    // Map geometry
    // ------------
    /** The map seeding edge orientation within the input images. */
    private var isVertical: Boolean = false
    /** Long-axis coordinates of the seeding edge .*/
    private var pE: Pair<Int, Int> = Pair(0, 1)
    /** Short-axis coordinate of the seeding edge. */
    private var pL: Int = 0
    /** Sample size of map - space and time. */
    private var ns: Int = 0
    private var nt: Int = 0


    // Buffering
    // ---------
    private lateinit var mapView: ShortMap

    private var reachedEnd = false

    // ---------------------------------------------------------------------------------------------
    // Creation and memory allocation
    // ---------------------------------------------------------------------------------------------

    override fun nRequiredBuffers(): Int { return 1 }

    override fun setRoiAndBuffers(roi: MappingRoi, buffers: List<ByteBuffer>): Boolean {
        if(buffers.size < nRequiredBuffers()) return false

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

        mapView = ShortMap(buffers[0], ns)
        return true
    }


    // ---------------------------------------------------------------------------------------------
    // Update and bitmap creation.
    // ---------------------------------------------------------------------------------------------

    /** The space-time size of the map (samples). */
    override fun spaceTimeSampleSize(): Size { return Size(ns, nt) }

    /** Update the map with a new camera frame. */
    override fun updateWithCameraBitmap(bitmap: Bitmap) {
        if(reachedEnd) return
        try {
            (pE.first until pE.second).map {
                mapView.addNTSCGrey(if(isVertical) bitmap.getPixel(pL, it) else bitmap.getPixel(it, pL))
            }
            nt += 1
        } catch (_: java.lang.IndexOutOfBoundsException) { reachedEnd = true }
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
                    backing[k] = mapView.getColorInt(i, j)
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


    override fun destroy(file: File?) {

        println("DESTROYING MAP: saving to = ${file?.absolutePath}")

        // save
        // todo = put in coroutine so won't take for ever. (Need to pass new buffer view
        //    around buffer, so that this object can be destroyed?)
        /*
        val path = File(
            Environment.getExternalStoragePublicDirectory(Environment.DIRECTORY_DOCUMENTS),
            "xxxxmap.tiff"
        )
        val img = TIFFImage()
        img.add(mapView.tiffDirectory(nt))
        TiffWriter.writeTiff(path, img)
        */

    }
}
