package com.scepticalphysiologist.dmaple.map.creator

import android.graphics.Bitmap
import android.graphics.Rect
import android.util.Size
import com.scepticalphysiologist.dmaple.etc.Edge
import com.scepticalphysiologist.dmaple.ui.camera.ThresholdBitmap
import com.scepticalphysiologist.dmaple.map.field.FieldRoi
import com.scepticalphysiologist.dmaple.map.field.FieldRuler
import mil.nga.tiff.FieldTagType
import mil.nga.tiff.FileDirectory
import java.lang.IllegalArgumentException
import java.lang.IndexOutOfBoundsException
import java.nio.ByteBuffer
import kotlin.math.ceil

/** The types of maps that be created. */
enum class MapType (
    /** The title of the map. */
    val title: String,
    /** The number of maps created for the type. */
    val nMaps: Int,
){
    DIAMETER(title = "diameter", nMaps = 1,),
    RADIUS(title = "radius", nMaps = 2),
    SPINE(title = "spine profile", nMaps = 1);
}

/** Handles the creation of spatio-temporal maps for a single ROI. */
class MapCreator(val roi: FieldRoi) {

    /** The number of maps produced by the creator. */
    val nMaps: Int = roi.maps.sumOf { it.nMaps }

    // Map geometry
    // ------------
    /** The number of spatial pixels. */
    private val ns: Int
    /** The number of temporal samples. */
    private var nt: Int = 0
    /** The spatial resolution (pixels/unit) and unit. */
    private var spatialRes = Pair(1f, "")
    /** The temporal resolution (pixels/unit) and unit. */
    private var temporalRes = Pair(1f, "s")

    // Map calculation
    // ---------------
    /** The segmentor used for calculating the map values. */
    val segmentor: BitmapGutSegmentor
    /** The range of pixels along the seeding edge, used to detect the gut. */
    private val seedRange: Pair<Int, Int>

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

        // Axes longitudinal and transverse to gut.
        val axesLongAndTrans = when(roi.seedingEdge) {
            Edge.LEFT -> Pair(Pair(roi.left, roi.right), Pair(roi.top, roi.bottom))
            Edge.RIGHT -> Pair(Pair(roi.right, roi.left), Pair(roi.top, roi.bottom))
            Edge.TOP -> Pair(Pair(roi.top, roi.bottom), Pair(roi.left, roi.right))
            Edge.BOTTOM -> Pair(Pair(roi.bottom, roi.top), Pair(roi.left, roi.right))
        }
        val (longAxis, transAxis) = axesLongAndTrans

        // Gut segmentor.
        segmentor = BitmapGutSegmentor()
        segmentor.threshold = roi.threshold.toFloat()
        segmentor.gutIsHorizontal = roi.seedingEdge.isVertical()
        segmentor.gutIsAboveThreshold = !ThresholdBitmap.highlightAbove
        segmentor.setLongSection(longAxis.first.toInt(), longAxis.second.toInt())
        seedRange = Pair(transAxis.first.toInt(), transAxis.second.toInt())
        ns = segmentor.longIdx.size
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

    fun setSpatialResolution(ruler: FieldRuler) { spatialRes = ruler.getResolution() }

    /** The current sample width (space) and height (time) of the map. */
    fun spaceTimeSampleSize(): Size { return Size(ns, nt) }

    // ---------------------------------------------------------------------------------------------
    // Calculation
    // ---------------------------------------------------------------------------------------------

    /** Update the maps from a new camera bitmap. */
    fun updateWithCameraBitmap(bitmap: Bitmap) {
        if(reachedEnd) return
        try {
            // Analyse the bitmap.
            segmentor.setFieldImage(bitmap)
            if(nt == 0) segmentor.detectGutAndSeedSpine(seedRange)
            else segmentor.updateBoundaries()

            // Update the map values.
            var j = 0
            var p = 0
            for(i in 0 until ns) {
                diameterMap?.add(segmentor.getDiameter(i).toShort())
                radiusMapLeft?.add(segmentor.getLowerRadius(i).toShort())
                radiusMapRight?.add(segmentor.getUpperRadius(i).toShort())
                spineMap?.let { map ->
                    j = segmentor.getSpine(i)
                    p = if(segmentor.gutIsHorizontal) bitmap.getPixel(i, j) else bitmap.getPixel(j, i)
                    map.add(p)
                }
            }
            nt += 1
        } catch (_: java.lang.IndexOutOfBoundsException) { reachedEnd = true }
    }

    /** Set the temporal resolution given a recording duration. */
    fun setTemporalResolution(durationSec: Float) {
        temporalRes = Pair(ns.toFloat() / durationSec, "s")
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
            buffer?.toTiffDirectory(description, nt)?.also { tiff ->
                setResolution(tiff, spatialRes, temporalRes)
            }
        }.filterNotNull()
    }

    /** Load the maps from TIFF slices/directories. */
    fun fromTiff(tiffs: List<FileDirectory>) {
        mapBuffers.map { (description, buffer) ->
            buffer?.let { buff ->
                findTiff(tiffs, description)?.let { tiff ->
                    nt = buff.fromTiffDirectory(tiff)
                    val (xr, yr) = getResolution(tiff)
                    spatialRes = xr
                    temporalRes = yr
                }
            }
        }
    }

}

/** Find a tiff directory with a matching identifier (description filed). */
fun findTiff(tiffs: List<FileDirectory>, identifier: String): FileDirectory? {
    return tiffs.filter{it.getStringEntryValue(FieldTagType.ImageDescription) == identifier}.firstOrNull()
}

/** Set the x and y resolutions of a tiff directory. */
fun setResolution(
    tiff: FileDirectory,
    xr: Pair<Float, String>,
    yr: Pair<Float, String>
) {
    // todo - these are being written to tiff file, but are not picked up by ImageJ
    //    to set it's pixel width/height fields. How is ImageJ using saving this info?
    //    Also there is no tiff field for "pixel depth" i.e. short to diameter.
    tiff.setStringEntryValue(FieldTagType.ResolutionUnit, xr.second)
    tiff.setStringEntryValue(FieldTagType.XResolution, xr.first.toString())
    tiff.setStringEntryValue(FieldTagType.YResolution, yr.first.toString())
}

/** Get the x and y resolutions of a tiff directory. */
fun getResolution(tiff: FileDirectory): Pair<Pair<Float, String>, Pair<Float, String>> {
    val runit = tiff.getStringEntryValue(FieldTagType.ResolutionUnit)
    val xr = tiff.getStringEntryValue(FieldTagType.XResolution).toFloatOrNull() ?: 1f
    val yr = tiff.getStringEntryValue(FieldTagType.YResolution).toFloatOrNull() ?: 1f
    return Pair(Pair(xr, runit), Pair(yr, runit))
}

fun rangeSize(range: Int, step: Int): Int {
    return ceil(range.toFloat() / step.toFloat()).toInt()
}

