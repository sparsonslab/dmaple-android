package com.scepticalphysiologist.dmaple.map.creator

import android.graphics.Color
import androidx.core.graphics.blue
import androidx.core.graphics.green
import androidx.core.graphics.red
import mil.nga.tiff.FieldTagType
import mil.nga.tiff.FieldType
import mil.nga.tiff.FileDirectory
import mil.nga.tiff.Rasters
import mil.nga.tiff.util.TiffConstants
import java.lang.IndexOutOfBoundsException
import java.lang.Math.floorDiv
import java.nio.ByteBuffer

/** A wrapper ("view") around a byte buffer that holds a map's data.
 *
 * The map is arranged spatial-axis-major order in the buffer.
 *
 * @param T The type of the map's pixels.
 * @property buffer The byte buffer that holds the map's data.
 * @property nx The number of spatial pixels in the map.
 */
abstract class MapBufferView<T : Number>(
    protected val buffer: ByteBuffer,
    protected var nx: Int
) {

    /** The "field type" for the TIFF image. */
    protected abstract val fieldType: FieldType
    /** The number of bytes in each channel. */
    protected abstract val bytesPerChannel: List<Int>
    /** The number of bits in each channel. */
    protected val bitsPerChannel: List<Int> get() = bytesPerChannel.map{it * 8}
    /** The number of bytes per space-time sample. */
    protected val bytesPerSample: Int get() = bytesPerChannel.sum()

    // ---------------------------------------------------------------------------------------------
    // Buffer indexing
    // ---------------------------------------------------------------------------------------------

    /** The buffer position of the (i, j) pixel. */
    protected fun bufferPosition(i: Int, j: Int): Int { return bytesPerSample * (j * nx + i) }

    /** The current space-time sample. */
    protected fun currentSample(): Int { return floorDiv(buffer.position(),  bytesPerSample) }

    /** The current time sample. */
    protected fun currentTimeSample(): Int { return floorDiv(buffer.position(), bytesPerSample * nx) }

    // ---------------------------------------------------------------------------------------------
    // Buffer access (to be implemented by concrete subclasses)
    // ---------------------------------------------------------------------------------------------

    /** Set the value of the (i, j) pixel. */
    abstract fun set(i: Int, j: Int, value: T)

    /** Get the value of the (i, j) pixel. */
    abstract fun get(i: Int, j: Int): T

    /** Add a value to the map. */
    abstract fun add(value: T)

    /** Get a color integer to show the (i, j) pixel in a bitmap. */
    abstract fun getColorInt(i: Int, j: Int): Int

    // ---------------------------------------------------------------------------------------------
    // TIFF I/O
    // ---------------------------------------------------------------------------------------------

    /** Set the (i, j) pixel of an image raster. */
    abstract fun toRaster(i: Int, j: Int, raster: Rasters)

    /** Get the (i, j) pixel of an image raster. */
    abstract fun fromRaster(i: Int, j: Int, raster: Rasters)

    /** Convert the map into a TIFF slice/directory.
     *
     * @param identifier A string used to identify the map in the TIFF.
     * @param y The yth time pixel up to which to use. Null to use the current position.
     * @return A TIFF slice with the map's data.
     * */
    fun toTiffDirectory(
        identifier: String = "",
        y: Int? = null,
    ): FileDirectory {

        // y (row/temporal) position
        val currentTime = currentTimeSample()
        val ny = minOf(y ?: currentTime, currentTime)

        // Basic image properties
        val dir = FileDirectory()
        dir.setImageWidth(nx)
        dir.setImageHeight(ny)
        dir.samplesPerPixel = bitsPerChannel.size
        dir.setBitsPerSample(bitsPerChannel)
        dir.setStringEntryValue(FieldTagType.ImageUniqueID, identifier)

        // Write in map data.
        val raster = Rasters(nx, ny, bitsPerChannel.size, fieldType)
        dir.compression = TiffConstants.COMPRESSION_NO
        dir.planarConfiguration = TiffConstants.PLANAR_CONFIGURATION_CHUNKY
        dir.setRowsPerStrip(raster.calculateRowsPerStrip(dir.planarConfiguration))
        dir.writeRasters = raster
        // todo - set raster from buffer directly using raster.setinterleaved method. Tried this
        //    but not working???
        try {
            for(j in 0 until ny)
                for(i in 0 until nx)
                    toRaster(i, j, raster)
        } catch(_: IndexOutOfBoundsException) {}
        return dir
    }

    /** Load the map from a TIFF image slice/directory.
     *
     * @param dir The slice/directory with the map
     * @return The number of time samples in the map or null if the map's slice could not be found.
     * */
    open fun fromTiffDirectory(dir: FileDirectory): Int {
        // Read from the raster into the buffer.
        buffer.position(0)
        val raster = dir.readRasters()
        nx = raster.width
        try {
            for(j in 0 until raster.height)
                for(i in 0 until raster.width)
                    fromRaster(i, j, raster)
        } catch(_: IndexOutOfBoundsException) {}

        // Set the buffer position to end of the tiff data.
        buffer.position(bufferPosition(raster.width - 1, raster.height - 1))
        return raster.height
    }
}

/** A map consisting of RGB color values. */
class RGBMap(buffer: ByteBuffer, nx: Int): MapBufferView<Int>(buffer, nx) {

    override val fieldType = FieldType.BYTE

    override var bytesPerChannel = listOf(1, 1, 1)

    override fun set(i: Int, j: Int, value: Int) {
        val k = bufferPosition(i, j)
        buffer.put(k,     (value shr 16).toByte())
        buffer.put(k + 1, (value shr 8).toByte())
        buffer.put(k + 2, (value shr 0).toByte())
    }

    override fun get(i: Int, j: Int): Int {
        val k = bufferPosition(i, j)
        return  (255 shl 24) or
                (buffer[k].toInt() and 0xff shl 16) or
                (buffer[k + 1].toInt() and 0xff shl 8) or
                (buffer[k + 2].toInt() and 0xff shl 0)
    }

    override fun add(value: Int) {
        buffer.put((value shr 16).toByte())
        buffer.put((value shr 8).toByte())
        buffer.put((value shr 0).toByte())
    }

    override fun getColorInt(i: Int, j: Int): Int { return get(i, j) }

    override fun toRaster(i: Int, j: Int, raster: Rasters) {
        val color = getColorInt(i, j)
        raster.setPixelSample(0, i, j, color.red)
        raster.setPixelSample(1, i, j, color.green)
        raster.setPixelSample(2, i, j, color.blue)
    }

    override fun fromRaster(i: Int, j: Int, raster: Rasters) {
        set(i, j, Color.argb(
            255,
            raster.getPixelSample(0, i, j).toInt(),
            raster.getPixelSample(1, i, j).toInt(),
            raster.getPixelSample(2, i, j).toInt()
        ))
    }

}

/** A map consisting of short integers. */
class ShortMap(buffer: ByteBuffer, nx: Int): MapBufferView<Short>(buffer, nx) {

    override val fieldType = FieldType.SHORT

    override val bytesPerChannel = listOf(2)

    /** The smallest possible short value.
     * For converting between signed (buffer) and unsigned (raster/distance) short.
     */
    private val s0 = Short.MIN_VALUE.toFloat()

    /** The maximum sample value. */
    private var maxv: Short = Short.MIN_VALUE

    fun addDistance(value: Int) { add((value + Short.MIN_VALUE.toInt()).toShort()) }

    override fun set(i: Int, j: Int, value: Short) { buffer.putShort(bufferPosition(i, j), value) }

    override fun get(i: Int, j: Int): Short { return buffer.getShort(bufferPosition(i, j)) }

    override fun add(value: Short) {
        buffer.putShort(value)
        if(value > maxv) maxv = value
    }

    override fun getColorInt(i: Int, j: Int): Int {
        val denom = maxv.toFloat() - s0
         val v = (255f * (get(i, j).toFloat() - s0) / denom).toInt()
         return (255 shl 24) or
                (v and 0xff shl 16) or
                (v and 0xff shl 8) or
                (v and 0xff shl 0)
    }

    override fun toRaster(i: Int, j: Int, raster: Rasters) {
        raster.setPixelSample(0, i, j, get(i, j).toInt() - s0.toInt())
    }

    override fun fromRaster(i: Int, j: Int, raster: Rasters) {
        val v = (raster.getPixelSample(0, i, j).toInt() + s0.toInt()).toShort()
        set(i, j, v)
    }

    override fun fromTiffDirectory(dir: FileDirectory): Int {
        val nt =  super.fromTiffDirectory(dir)
        maxv = Short.MIN_VALUE
        for(i in 0 until currentSample()) {
            val v = buffer.getShort(i * bytesPerSample)
            if(v > maxv) maxv = v
        }
        return nt
    }
}
