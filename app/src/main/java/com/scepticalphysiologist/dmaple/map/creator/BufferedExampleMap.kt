package com.scepticalphysiologist.dmaple.map.creator

import android.graphics.Bitmap
import android.graphics.Color
import android.graphics.Rect
import android.util.Size
import com.scepticalphysiologist.dmaple.etc.Point
import com.scepticalphysiologist.dmaple.map.MappingRoi
import mil.nga.tiff.FieldTagType
import mil.nga.tiff.FileDirectory
import java.lang.IllegalArgumentException
import java.lang.IndexOutOfBoundsException
import java.nio.ByteBuffer
import kotlin.math.abs

class BufferedExampleMap(roi: MappingRoi): MapCreator(roi) {

    override val nMaps = 1

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

    // Buffering
    // ---------
    private var mapView: ShortMap? = null
    private var reachedEnd = false

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

    override fun provideBuffers(buffers: List<ByteBuffer>): Boolean {
        if(buffers.size < nMaps) return false
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
                mapView?.addNTSCGrey(if(isVertical) bitmap.getPixel(pL, it) else bitmap.getPixel(it, pL))
            }
            nt += 1
        } catch (_: java.lang.IndexOutOfBoundsException) { reachedEnd = true }
    }

    override fun getMapBitmap(
        idx: Int,
        crop: Rect?,
        stepX: Int, stepY: Int,
        backing: IntArray,
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
                    backing[k] = mapView?.getColorInt(i, j) ?: Color.BLACK
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

    override fun toTiff(): List<FileDirectory>? {
        return mapView?.let{ bufferView ->
            val description = "${MapType.getMapType(this).title}:0"
            return listOf(bufferView.toTiffDirectory(description, nt))
        }
    }

    override fun fromTiff(tiff: List<FileDirectory>) {
        mapView?.let { bufferView ->
            val description = "${MapType.getMapType(this).title}:0"
            bufferView.fromTiffDirectory(description, tiff)?.let { nt = it }
        }
    }
}
