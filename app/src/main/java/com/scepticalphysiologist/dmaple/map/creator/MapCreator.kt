package com.scepticalphysiologist.dmaple.map.creator

import android.graphics.Bitmap
import android.graphics.Rect
import android.util.Size
import com.scepticalphysiologist.dmaple.etc.Edge
import com.scepticalphysiologist.dmaple.etc.ThresholdBitmap
import com.scepticalphysiologist.dmaple.etc.ntscGrey
import com.scepticalphysiologist.dmaple.map.field.FieldRoi
import mil.nga.tiff.FileDirectory
import java.lang.IllegalArgumentException
import java.lang.IndexOutOfBoundsException
import java.nio.ByteBuffer
import kotlin.math.abs


enum class MapType (
    val title: String,
    val nMaps: Int,
){
    DIAMETER(
        title = "diameter",
        nMaps = 1,
    ),
    RADIUS(
        title = "radius",
        nMaps = 2
    ),
    SPINE(
        title = "spine profile",
        nMaps = 1
    );
}


/** Handles the creation of spatio-temporal maps for a single ROI. */
class MapCreator(val roi: FieldRoi) {

    /** The number of maps produced by the creator. */
    val nMaps: Int = roi.maps.sumOf { it.nMaps }

    // Map geometry
    // ------------
    /** The map seeding edge orientation within the input images. */
    val gutIsHorizontal: Boolean = roi.seedingEdge.isVertical()
    private val offset = -roi.threshold.toFloat()
    private val gain = if(ThresholdBitmap.highlightAbove) -1f else 1f

    /** Sample size of map - space and time. */
    private val ns: Int
    private var nt: Int = 0


    // Coordinate arrays
    // -----------------
    val analyser: BitmapFieldAnalyser
    val longIdx: IntArray
    val transIdx: IntArray

    val spine: IntArray
    val upperBound: IntArray
    val lowerBound: IntArray


    // Buffering
    // ---------
    /** The diameter map.  */
    private var diameterMap: ShortMap? = null
    /** The left-radius map. */
    private var radiusMapLeft: ShortMap? = null
    /** The right-radius map. */
    private var radiusMapRight: ShortMap? = null
    /** The spine intensity map. */
    private var spineMap: RGBMap? = null
    /** The maps and their descriptions.
     * The description is used both as the prefix to the map's file name (and so must be valid as
     * part of a file name) and as a TIFF tag to identify the map's creator.
     * */
    private val mapBuffers: List<Pair<String, MapBufferView<*>?>> get() = listOf(
        Pair("diameter", diameterMap),
        Pair("radius_left", radiusMapLeft),
        Pair("radius_right", radiusMapRight),
        Pair("spine", spineMap)
    )
    /** The end of any one of the buffers has been reached. */
    private var reachedEnd = false

    // ---------------------------------------------------------------------------------------------
    // Construction
    // ---------------------------------------------------------------------------------------------

    init {

        // Make sure the ROI fits in the image frame.
        roi.cropToFrame()

        analyser = BitmapFieldAnalyser()
        analyser.threshold = roi.threshold.toFloat()
        analyser.gutIsHorizontal = roi.seedingEdge.isVertical()
        analyser.gutIsAboveThreshold = !ThresholdBitmap.highlightAbove


        // Axes longitudinal and transverse to gut.
        val axesLongAndTrans = when(roi.seedingEdge) {
            Edge.LEFT -> Pair(Pair(roi.left, roi.right), Pair(roi.top, roi.bottom))
            Edge.RIGHT -> Pair(Pair(roi.right, roi.left), Pair(roi.top, roi.bottom))
            Edge.TOP -> Pair(Pair(roi.top, roi.bottom), Pair(roi.left, roi.right))
            Edge.BOTTOM -> Pair(Pair(roi.bottom, roi.top), Pair(roi.left, roi.right))
        }
        val (longAxis, transAxis) = axesLongAndTrans
        longIdx = pointsAlong(longAxis)
        transIdx = pointsAlong(transAxis)

        //println("long idx")
        //println(longIdx.toList())
        //println("trans idx")
        //println(transIdx.toList())

        // Spine positions.
        ns = longIdx.size
        spine = IntArray(ns)
        upperBound = IntArray(ns)
        lowerBound = IntArray(ns)


        // (as example)
        val j = transIdx.first() + (transIdx.last() - transIdx.first()) / 2
        println("j = $j")
        for(i in 0 until ns) {
            spine[i] = j
            upperBound[i] = j + 10
            lowerBound[i] = j - 10
        }


    }

    fun provideBuffers(buffers: List<ByteBuffer>): Boolean {
        if(buffers.size < nMaps) return false
        var i = 0
        for(map in roi.maps) when(map) {
            MapType.DIAMETER -> {
                diameterMap = ShortMap(buffers[i], ns)
                i += 1
            }
            MapType.RADIUS -> {
                radiusMapLeft = ShortMap(buffers[i], ns)
                radiusMapRight = ShortMap(buffers[i + 1], ns)
                i += 2
            }
            MapType.SPINE -> {
                spineMap = RGBMap(buffers[i], ns)
                i += 1
            }
        }
        return true
    }

    /** The current sample width (space) and height (time) of the map. */
    fun spaceTimeSampleSize(): Size { return Size(ns, nt) }

    // ---------------------------------------------------------------------------------------------
    // Calculation
    // ---------------------------------------------------------------------------------------------

    /** Update the maps from a new camera bitmap. */
    fun updateWithCameraBitmap(bitmap: Bitmap) {
        if(reachedEnd) return
        try {

            analyser.setImage(bitmap)
            if(nt == 0) seedSpine()

            var p = 0
            val j = spine[0]
            for(i in longIdx) {

                p = if(gutIsHorizontal) bitmap.getPixel(i, j) else bitmap.getPixel(j, i)
                diameterMap?.addNTSCGrey(p)
                radiusMapLeft?.addNTSCGrey(p)
                radiusMapRight?.addNTSCGrey(p)
                spineMap?.add(p)
            }

            nt += 1
        } catch (_: java.lang.IndexOutOfBoundsException) { reachedEnd = true }
    }


    private fun seedSpine() {
        analyser.findGut(longIdx[0], Pair(transIdx[0], transIdx[1]), minWidth = 10)?.let { gut ->
            val j = gut.first + (gut.second - gut.first)
            for(i in 0 until ns) spine[i] = j
        }
    }

    // ---------------------------------------------------------------------------------------------
    // Display
    // ---------------------------------------------------------------------------------------------

    /** Get a portion of one of the maps as a bitmap.
     *
     * @param idx The index of the map (if the creator makes more than one map).
     * @param crop The space-time area (samples) of the map to return.
     * @param stepX The number of space samples to step when sampling the map.
     * i.e. the spatial down-sampling of the map.
     * @param stepY The number of time samples to step when sampling the map.
     * @param backing A backing array for the bitmap, into the map samples will be put.
     * */
    fun getMapBitmap(
        idx: Int,
        crop: Rect?,
        stepX: Int = 1, stepY: Int = 1,
        backing: IntArray,
    ): Bitmap? {

        val buffer = mapBuffers.mapNotNull {it.second}.getOrNull(idx) ?: return null
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
                    backing[k] = buffer.getColorInt(i, j)
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
    // I/O
    // ---------------------------------------------------------------------------------------------

    /** Save the maps to TIFF slices/directories. */
    fun toTiff(): List<FileDirectory> {
        return mapBuffers.map{ (description, buffer) ->
            buffer?.toTiffDirectory(description, nt)
        }.filterNotNull()
    }

    /** Load the maps from TIFF slices/directories. */
    fun fromTiff(tiff: List<FileDirectory>) {
        mapBuffers.map { (description, buffer) ->
            buffer?.fromTiffDirectory(description, tiff)?.let { nt = it }
        }
    }

}
