package com.scepticalphysiologist.dmaple.map.creator

import android.graphics.Bitmap
import android.graphics.Rect
import android.util.Size
import com.scepticalphysiologist.dmaple.map.buffer.ByteMap
import com.scepticalphysiologist.dmaple.map.buffer.MapBufferProvider
import com.scepticalphysiologist.dmaple.map.buffer.MapBufferView
import com.scepticalphysiologist.dmaple.map.buffer.ShortMap
import com.scepticalphysiologist.dmaple.map.field.FieldRoi
import com.scepticalphysiologist.dmaple.map.field.FieldRuler
import mil.nga.tiff.FileDirectory
import mil.nga.tiff.TiffReader
import java.io.File
import java.io.RandomAccessFile
import java.lang.IllegalArgumentException
import java.lang.IndexOutOfBoundsException
import java.nio.ByteBuffer
import java.util.concurrent.ForkJoinPool
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

    // Map Display
    // -----------
    /** A pool of forked threads for parallelized bitmap (map image) creation.*/
    private val forkedPool = ForkJoinPool.commonPool()

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
                spineMap = ByteMap(buffers[i], ns)
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

    /** Update the maps from a new luminance image from the camera. */
    fun updateWithCameraImage(image: LumaImage) {
        if(reachedEnd) return
        try {
            // Analyse the bitmap.
            segmentor.setFieldImage(image)
            if(nt == 0) segmentor.detectGutAndSeedSpine()
            else segmentor.updateBoundaries()

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
                    p = if(segmentor.gutIsHorizontal) image.getPixel(k, j) else image.getPixel(j, k)
                    map.addSample(p.toByte())
                }
            }
            nt += 1
        } catch (_: java.nio.BufferOverflowException) { reachedEnd = true }
    }

    /** Set the temporal resolution the current recording duration. */
    fun setTemporalResolutionFromDuration(durationSec: Float) {
        temporalRes = Pair(nt.toFloat() / durationSec, "s")
    }

    /** Set the temporal resolution based upon an estimated frame rate (frames/sec). */
    fun setTemporalResolutionFromFPS(fps: Float) { temporalRes = Pair(fps, "s") }

    /** Set the spatial resolution from a ruler. */
    fun setSpatialResolutionFromRuler(ruler: FieldRuler) {
        val fullResolution = ruler.getResolution()
        val mapStep = abs(segmentor.longIdx[1] - segmentor.longIdx[0]).toFloat()
        spatialRes = Pair(fullResolution.first / mapStep, fullResolution.second)
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
     * @param live The bitmap is intended to be shown as part of a live display. i.e.
     * this function will be called at a high frequency).
     * */
    fun getMapBitmap(
        idx: Int,
        crop: Rect?,
        stepX: Int = 1, stepY: Int = 1,
        backing: IntArray,
        live: Boolean,
    ): Bitmap? {

        val buffer = mapBuffers.mapNotNull {it.second}.getOrNull(idx) ?: return null

        try {
            // Only allow a valid area of the map to be returned,
            val area = Rect(0, 0, ns, nt)
            crop?.let { area.intersect(crop) }
            val bs = Size(rangeSize(area.width(), stepX), rangeSize(area.height(), stepY))
            if(bs.width * bs.height > backing.size) return null

            // Pass values from buffer to bitmap backing in parallel.
            // https://stackoverflow.com/questions/30802463/how-many-threads-are-spawned-in-parallelstream-in-java-8
            val job = forkedPool.submit {
                sequence {
                    var k = -1
                    for (j in area.top until area.bottom step stepY)
                        for (i in area.left until area.right step stepX) {
                            k += 1
                            yield(listOf(i, j, k))
                        }
                }.toList().parallelStream().forEach {
                    backing[it[2]] = buffer.getColorInt(it[0], it[1])
                }
            }

            // If we are using the bitmap for live display allow the bitmap to be returned
            // before we have actually finished updating the backing. This massively speeds
            // up the frame rate apparent to the user (especially for maps with a large number
            // of spatial samples), with only the small side effect that
            // the last few time pixels of the map's view might look un-updated.
            if(!live) job.join()

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
