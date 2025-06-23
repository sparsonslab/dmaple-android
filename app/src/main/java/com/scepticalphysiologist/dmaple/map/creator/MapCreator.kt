// Copyright (c) 2025-2025. Dr Sean Paul Parsons. All rights reserved.

package com.scepticalphysiologist.dmaple.map.creator

import android.graphics.Bitmap
import android.graphics.Rect
import android.util.Size
import com.scepticalphysiologist.dmaple.map.buffer.ByteMap
import com.scepticalphysiologist.dmaple.map.buffer.MapBufferProvider
import com.scepticalphysiologist.dmaple.map.buffer.MapBufferView
import com.scepticalphysiologist.dmaple.map.buffer.ShortMap
import com.scepticalphysiologist.dmaple.map.field.FieldRoi
import mil.nga.tiff.FileDirectory
import mil.nga.tiff.TiffReader
import java.io.File
import java.io.RandomAccessFile
import java.lang.IllegalArgumentException
import java.lang.IndexOutOfBoundsException
import java.nio.ByteBuffer
import kotlin.math.abs

/** Handles the creation of spatio-temporal maps for a single ROI. */
class MapCreator(val roi: FieldRoi, val params: FieldParams) {

    /** The number of maps produced by the creator. */
    val nMaps: Int = roi.maps.sumOf { it.nMaps }

    // Map geometry
    // ------------
    /** The number of spatial pixels. */
    private var ns: Int
    /** The number of temporal samples. */
    private var nt: Int = 0
    /** The spatial resolution (pixels/unit) and unit. */
    var spatialRes = Pair(1f, "")
    /** The temporal resolution (pixels/unit) and unit. */
    var temporalRes = Pair(1f, "s")

    // Map calculation
    // ---------------
    /** The segmentor used for calculating the map values. */
    val segmentor: GutSegmentor
    /** Segmentation is required. */
    private var segmentationRequired: Boolean = false

    // Buffering
    // ---------
    /** The diameter map.  */
    private var diameterMap: ShortMap? = null
    /** The left-radius map. */
    private var radiusMapLeft: ShortMap? = null
    /** The right-radius map. */
    private var radiusMapRight: ShortMap? = null
    /** The spine intensity map. */
    private var spineMap: ByteMap? = null
    /** The light intensity map. */
    private var lightMap: ByteMap? = null
    /** The maps and their descriptions.
     * The description is used both as the prefix to the map's file name (and so must be valid as
     * part of a file name) and as a TIFF tag to identify the map's creator.
     * */
    private val mapBuffers: List<Pair<String, MapBufferView<*>?>> get() = listOf(
        Pair("diameter", diameterMap),
        Pair("radius_left", radiusMapLeft),
        Pair("radius_right", radiusMapRight),
        Pair("spine", spineMap),
        Pair("light", lightMap)
    )
    /** The end of any one of the buffers has been reached. */
    private var reachedEnd = false

    // ---------------------------------------------------------------------------------------------
    // Construction
    // ---------------------------------------------------------------------------------------------

    init {
        // Make sure the ROI fits in the image frame.
        roi.cropToFrame()
        // Gut segmentor.
        segmentor = GutSegmentor(roi, params)
        ns = segmentor.longIdx.size
    }

    /** Provide buffers for holding map data. */
    fun provideBuffers(buffers: List<ByteBuffer>): Boolean {
        // Check there are enough buffers.
        if(buffers.size < nMaps) return false

        // Allocate the buffers.
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
                spineMap = ByteMap(buffers[i], ns)
                i += 1
            }
            MapType.LIGHT -> {
                lightMap = ByteMap(buffers[i], ns)
                i += 1
            }
        }

        // Segmentation is required if more than the light map is being created.
        segmentationRequired = !((nMaps == 1) && (lightMap != null))
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

    /** Update the maps from a new luminance image from the camera. */
    fun updateWithCameraImage(image: LumaReader) {
        if(reachedEnd) return
        try {
            // Analyse the bitmap.
            segmentor.setFieldImage(image)
            if(segmentationRequired) {
                if(nt == 0) segmentor.detectGutAndSeedSpine()
                else segmentor.updateBoundaries()
            }

            // Update the map values.
            var j: Int
            var k: Int
            var p: Int
            for(i in 0 until ns) {
                diameterMap?.addDistance(segmentor.getDiameter(i))
                radiusMapLeft?.addDistance(segmentor.getLowerRadius(i))
                radiusMapRight?.addDistance(segmentor.getUpperRadius(i))
                spineMap?.let { map ->
                    j = segmentor.getSpine(i)
                    k = segmentor.longIdx[i]
                    p = if(segmentor.gutIsHorizontal) image.getPixelLuminance(k, j) else image.getPixelLuminance(j, k)
                    map.addSample(p.toByte())
                }
                lightMap?.let { map ->
                    k = segmentor.longIdx[i]
                    // Note, Calculating the transverse axis mean: accumulation of sum is MUCH
                    // faster than mapping the transIdx and then calling average().
                    // This makes a real difference on slower tablets like the Lenovo.
                    var sum: Double = 0.0
                    if(segmentor.gutIsHorizontal) for(j in segmentor.transIdx) sum += image.getPixelLuminance(k, j).toFloat()
                    else for(j in segmentor.transIdx) sum += image.getPixelLuminance(j, k).toFloat()
                    map.addSample((sum / segmentor.transIdx.size).toInt().toByte())
                }
            }
            nt += 1
        } catch (_: java.nio.BufferOverflowException) { reachedEnd = true }
    }

    /** Set the temporal resolution from the frame interval (sec). */
    fun setFrameIntervalSec(frameIntervalSec: Float) { temporalRes = Pair(1f / frameIntervalSec, "s") }

    /** Set the temporal resolution from the frame rate (frames/sec). */
    fun setFrameRatePerSec(framesPerSec: Float) { temporalRes = Pair(framesPerSec, "s") }

    /** Set the spatial resolution from the frame pixels per spatial unit. */
    fun setSpatialPixelsPerUnit(resolution: Pair<Float, String>) {
        val mapStep = abs(segmentor.longIdx[1] - segmentor.longIdx[0]).toFloat()
        spatialRes = Pair(resolution.first / mapStep, resolution.second)
    }

    /** At least one buffer has reached capacity and no more samples will be added to the maps,
     * irrespective of calls to [updateWithCameraImage]. */
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
            // Transfer values to bitmap backing array.
            // It would be nice to parallise this without creating a intermediary list of indexes
            // that takes up lots of memory.
            var k = 0
            for (j in area.top until area.bottom step stepY)
                for (i in area.left until area.right step stepX) {
                    backing[k] = buffer.getColorInt(i, j)
                    k += 1
                }
            // Return bitmap.
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

    fun loadFromTiffs(recordFolder: File, bufferProvider: MapBufferProvider): Boolean {
        // TIFF files associated with the creator.
        val files = recordFolder.listFiles()?.filter{
            it.name.startsWith(roi.uid) && it.name.endsWith(".tiff")
        } ?: listOf()
        if(files.isEmpty()) return false

        // Provide buffers
        if(bufferProvider.nFreeBuffers() < nMaps) return false
        val buffers = (0 until nMaps).map{bufferProvider.getFreeBuffer()}.filterNotNull()
        val provided = provideBuffers(buffers)
        if(!provided) return false

        // Load.
        for((description, buffer) in mapBuffers) {
            // Check buffer and get file to read from.
            if(buffer == null) continue
            val file = files.firstOrNull { it.name.contains(description) } ?: continue

            // Read directory from file.
            val dir = TiffReader.readTiff(file).fileDirectories[0]
            ns = dir.imageWidth.toInt()
            nt = dir.imageHeight.toInt()
            val (xr, yr) = getResolution(dir)
            spatialRes = xr
            temporalRes = yr

            // Read directory into buffer.
            val strm = RandomAccessFile(file, "r")
            buffer.fromTiffDirectory(dir, strm)
            strm.channel.close()
            strm.close()
        }
        return true
    }
}
