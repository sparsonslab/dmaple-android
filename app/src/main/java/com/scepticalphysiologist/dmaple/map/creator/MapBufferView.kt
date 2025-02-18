package com.scepticalphysiologist.dmaple.map.creator

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
 * @param T The type of the map's pixels.
 * @property buffer The byte buffer that holds the map's data.
 * @property nx The number of spatial pixels in the map.
 */
abstract class MapBufferView<T : Number>(
    protected val buffer: ByteBuffer,
    protected val nx: Int
) {
    /** The number of color channels per pixel. */
    protected abstract val nColorChannels: Int

    /** The number of bits per color channel. */
    protected abstract val bitsPerChannel: Int

    /** The "field type" for the TIFF image. */
    protected abstract val fieldType: FieldType

    /** Convert the map into a TIFF image "directory".
     * @param y The yth time pixel up to which to use. Null to use the current position.
     * */
    fun tiffDirectory(y: Int? = null): FileDirectory {
        val currentTime = floorDiv(buffer.position(), nx)
        val ny = minOf(y ?: currentTime, currentTime)

        val dir = FileDirectory()
        dir.setImageWidth(nx)
        dir.setImageHeight(ny)
        dir.samplesPerPixel = nColorChannels
        dir.setBitsPerSample(bitsPerChannel)

        val raster = Rasters(nx, ny, nColorChannels, fieldType)
        dir.compression = TiffConstants.COMPRESSION_NO
        dir.planarConfiguration = TiffConstants.PLANAR_CONFIGURATION_CHUNKY
        dir.setRowsPerStrip(raster.calculateRowsPerStrip(dir.planarConfiguration))
        dir.writeRasters = raster

        // todo - set raster from buffer directly using raster.setinterleaved method. Tried this
        //    but not working???
        try {
            for(j in 0 until ny)
                for(i in 0 until nx)
                    setPixel(i, j, raster)
        } catch(_: IndexOutOfBoundsException) {}
        return dir
    }

    /** Set the (i, j) pixel of the image raster. */
    abstract fun setPixel(i: Int, j: Int, raster: Rasters)

    /** Get the value of the (i, j) pixel. */
    abstract fun get(i: Int, j: Int): T

    /** Set the value of the (i, j) pixel. */
    abstract fun set(i: Int, j: Int, value: T)

    /** Add a value to the map. */
    abstract fun add(value: T)

    /** Get a color integer to show the (i, j) pixel in a bitmap. */
    abstract fun getColorInt(i: Int, j: Int): Int
}

/** A map consisting of RGB color values. */
class RGBMap(buffer: ByteBuffer, nx: Int): MapBufferView<Int>(buffer, nx) {

    override val nColorChannels = 3

    override val bitsPerChannel = 8

    override val fieldType = FieldType.BYTE

    override fun setPixel(i: Int, j: Int, raster: Rasters) {
        val color = getColorInt(i, j)
        raster.setPixelSample(0, i, j, color.red)
        raster.setPixelSample(1, i, j, color.green)
        raster.setPixelSample(2, i, j, color.blue)
    }

    override fun get(i: Int, j: Int): Int {
        val k = 3 * (j * nx + i)
        return  (255 shl 24) or
                (buffer[k].toInt() and 0xff shl 16) or
                (buffer[k + 1].toInt() and 0xff shl 8) or
                (buffer[k + 2].toInt() and 0xff shl 0)
    }


    override fun set(i: Int, j: Int, value: Int) {
        val k = 3 * (j * nx + i)
        buffer.put(k,     (value shr 16).toByte())
        buffer.put(k + 1, (value shr 8).toByte())
        buffer.put(k + 2, (value shr 0).toByte())
    }

    override fun add(value: Int) {
        buffer.put((value shr 16).toByte())
        buffer.put((value shr 8).toByte())
        buffer.put((value shr 0).toByte())
    }

    override fun getColorInt(i: Int, j: Int): Int { return get(i, j) }

}

/** A map consisting of short integers. */
class ShortMap(buffer: ByteBuffer, nx: Int): MapBufferView<Short>(buffer, nx) {

    override val nColorChannels = 1

    override val bitsPerChannel = 16

    override val fieldType = FieldType.SHORT

    val s0 = Short.MIN_VALUE.toFloat()
    val s1 = Short.MAX_VALUE.toFloat()
    val sr = s1 - s0
    val rt = sr / 255f

    override fun setPixel(i: Int, j: Int, raster: Rasters) {
        raster.setPixelSample(0, i, j, get(i, j) - s0)
    }

    override fun get(i: Int, j: Int): Short {
        return buffer.getShort(2 * (j * nx + i))
    }

    override fun set(i: Int, j: Int, value: Short) {
        buffer.putShort(2 * (j * nx + i), value)
    }

    override fun add(value: Short) {
        buffer.putShort(value)
    }

    override fun getColorInt(i: Int, j: Int): Int {
         val v = ((get(i, j) - s0) / rt).toInt()
         return (255 shl 24) or
                (v and 0xff shl 16) or
                (v and 0xff shl 8) or
                (v and 0xff shl 0)
    }

    fun addNTSCGrey(value: Int) {
        val v = (0.299f * value.red  +
                 0.587f * value.green +
                 0.114f * value.blue) * rt + s0
        add(v.toInt().toShort())
    }

}
