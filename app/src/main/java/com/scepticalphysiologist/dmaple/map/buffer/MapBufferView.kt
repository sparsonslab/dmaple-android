package com.scepticalphysiologist.dmaple.map.buffer

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

    /** The buffer position of the (i, j)th (space, time) sample. */
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
     * @return The x-y pixel size (space and time sample size of the map).
     * */
    open fun fromTiffDirectory(dir: FileDirectory): Pair<Int, Int> {
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
        return Pair(raster.width, raster.height)
    }
}
