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
    /** The number of bytes required per space-time sample/pixel per map. */
    val bytesPerSample: Int
){
    DIAMETER(title = "diameter", nMaps = 1, bytesPerSample = 2),
    RADIUS(title = "radius", nMaps = 2, bytesPerSample = 2),
    SPINE(title = "spine profile", nMaps = 1, bytesPerSample = 3);
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
    val segmentor: GutSegmentor
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
        segmentor = GutSegmentor()
        segmentor.threshold = roi.threshold.toFloat()
        segmentor.gutIsHorizontal = roi.seedingEdge.isVertical()
        segmentor.gutIsAboveThreshold = !ThresholdBitmap.highlightAbove
        segmentor.setLongSection(longAxis.first.toInt(), longAxis.second.toInt())
        seedRange = Pair(transAxis.first.toInt(), transAxis.second.toInt())
        ns = segmentor.longIdx.size
    }

    /** Provide buffers for holding map data. */
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

    /** The number of spatial samples in the maps. */
    fun spaceSamples(): Int { return ns }

    /** The current number of temporal samples in the maps. */
    fun timeSamples(): Int { return nt }

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
            var j: Int
            var k: Int
            var p: Int
            for(i in 0 until ns) {
                diameterMap?.add(segmentor.getDiameter(i).toShort())
                radiusMapLeft?.add(segmentor.getLowerRadius(i).toShort())
                radiusMapRight?.add(segmentor.getUpperRadius(i).toShort())
                spineMap?.let { map ->
                    j = segmentor.getSpine(i)
                    k = segmentor.longIdx[i]
                    p = if(segmentor.gutIsHorizontal) bitmap.getPixel(k, j) else bitmap.getPixel(j, k)
                    map.add(p)
                }
            }
            nt += 1
        } catch (_: java.nio.BufferOverflowException) { reachedEnd = true }
    }

    /** Set the temporal resolution given a recording duration. */
    fun setTemporalResolution(durationSec: Float) {
        temporalRes = Pair(ns.toFloat() / durationSec, "s")
    }

    /** Set the spatial resolution from a ruler. */
    fun setSpatialResolution(ruler: FieldRuler) { spatialRes = ruler.getResolution() }

    /** At least one buffer has reached capacity and no more samples will be added to the maps,
     * irrespective of calls to [updateWithCameraBitmap]. */
    fun hasReachedBufferLimit(): Boolean { return reachedEnd }

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

// -------------------------------------------------------------------------------------------------
// I/O
// -------------------------------------------------------------------------------------------------

/** Find a tiff directory with a matching identifier. */
fun findTiff(tiffs: List<FileDirectory>, identifier: String): FileDirectory? {
    return tiffs.filter{it.getStringEntryValue(FieldTagType.ImageUniqueID) == identifier}.firstOrNull()
}

/** Set the x and y resolutions of a tiff directory according to the ImageJ format. */
fun setResolution(tiff: FileDirectory, xr: Pair<Float, String>, yr: Pair<Float, String>) {
    tiff.setRationalEntryValue(FieldTagType.XResolution, floatToRational(xr.first))
    tiff.setRationalEntryValue(FieldTagType.YResolution, floatToRational(yr.first))
    val imagejDescription = "ImageJ=1.53k\nunit=${xr.second}\nyunit=${yr.second}\nzunit=${xr.second}"
    tiff.setStringEntryValue(FieldTagType.ImageDescription, imagejDescription)
    // todo - set ImageJ pixel depth scale factor.
}

/** Get the x and y resolutions of a tiff directory. */
fun getResolution(tiff: FileDirectory): Pair<Pair<Float, String>, Pair<Float, String>> {
    try {
        val xr = rationalToFloat(tiff.getLongListEntryValue(FieldTagType.XResolution))
        val yr = rationalToFloat(tiff.getLongListEntryValue(FieldTagType.YResolution))
        var xu = ""
        var yu = ""
        val description = tiff.getStringEntryValue(FieldTagType.ImageDescription)
        for(line in description.split("\n")){
            val entry = line.split("=")
            if(entry.size != 2) continue
            when(entry[0]) {
                "unit" -> xu = entry[1]
                "yunit" -> yu = entry[1]
            }
        }
        return Pair(Pair(xr, xu), Pair(yr, yu))
    } catch(_: java.lang.ClassCastException) {}
    return Pair(Pair(1f, ""), Pair(1f, ""))
}

fun rangeSize(range: Int, step: Int): Int {
    return ceil(range.toFloat() / step.toFloat()).toInt()
}

/** Convert a float to a pair of numerator and denominator long integers. */
fun floatToRational(value: Float, denom: Long = 100_000): List<Long> {
    val num = (value * denom).toLong()
    return listOf(num, denom)
}

/** Convert a pair of numerator and denominator long integers to a float. */
fun rationalToFloat(ratio: List<Long>): Float {
    if(ratio.size < 2) return 1f
    return ratio[0].toFloat() / ratio[1].toFloat()
}
