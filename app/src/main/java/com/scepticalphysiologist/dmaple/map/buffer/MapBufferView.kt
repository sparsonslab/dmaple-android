package com.scepticalphysiologist.dmaple.map.buffer

import mil.nga.tiff.FieldTagType
import mil.nga.tiff.FieldType
import mil.nga.tiff.FileDirectory
import mil.nga.tiff.util.TiffConstants
import java.io.RandomAccessFile
import java.lang.Math.floorDiv
import java.nio.ByteBuffer
import java.nio.ByteOrder
import java.nio.channels.FileChannel

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

    /** Add a value to the map. */
    abstract fun addSample(value: T)

    /** Get the ith sample. */
    abstract fun getSample(i: Int): T

    /** Set the ith sample. */
    abstract fun setSample(i: Int, value: T)

    /** Get the (i, j)th pixel. */
    fun getPixel(i: Int, j: Int): T { return getSample(i + j * nx) }

    /** Set the (i, j)th pixel. */
    fun setPixel(i: Int, j: Int, value: T) { setSample(i + j * nx, value) }

    /** Get a color integer to show the (i, j) pixel in a bitmap. */
    abstract fun getColorInt(i: Int, j: Int): Int

    // ---------------------------------------------------------------------------------------------
    // TIFF I/O
    // ---------------------------------------------------------------------------------------------

    /** Convert the map into a TIFF slice/directory.
     *
     * @param identifier A string used to identify the map in the TIFF.
     * @param y The yth time pixel up to which to use. Null to use the current position.
     * @return A TIFF slice with the map's data.
     * */
    open fun toTiffDirectory(
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
        dir.setStringEntryValue(FieldTagType.ImageUniqueID, identifier)
        dir.compression = TiffConstants.COMPRESSION_NO
        dir.planarConfiguration = TiffConstants.PLANAR_CONFIGURATION_CHUNKY
        return dir
    }

    /** Load the map from a TIFF image slice/directory.
     *
     * @param dir The slice/directory with the map
     * @return The x-y pixel size (space and time sample size of the map).
     * */
    open fun fromTiffDirectory(dir: FileDirectory, stream: RandomAccessFile) {
        nx = dir.imageWidth.toInt()
        val offsets = dir.stripOffsets.map{it.toLong()}
        val lengths = dir.stripByteCounts.map{it.toLong()}
        var j: Long = 0
        buffer.position(0)
        buffer.order(ByteOrder.nativeOrder())
        for (i in offsets.indices) {
            val mbuffer = stream.channel.map(FileChannel.MapMode.READ_ONLY, offsets[i], lengths[i])
            // Although the channel's mapped buffer might say it is any byte order,
            // it is ALWAYS native order.
            // Therefore set both src and dst to native order.
            mbuffer.order(ByteOrder.nativeOrder())
            buffer.put(mbuffer)
            j += lengths[i]
        }
    }
}

/** Copy the content of one buffer to another. */
fun copyBuffer(src: ByteBuffer, dst: ByteBuffer) {
    // Make sure we are copying from the start of both buffers.
    src.position(0)
    dst.position(0)

    // Adjust the limit of the source to that of the destination,
    // so we don't get buffer overflow on the destination.
    val srcOldLimit = src.limit()
    if(dst.limit() < src.limit()) src.limit(dst.limit())

    // Do the copy, setting the destination endianness to that of the source.
    try {
        dst.put(src)
        dst.order(src.order())
    } catch(_: java.nio.BufferOverflowException) {}

    // Re-set the source limit.
    src.limit(srcOldLimit)
}

