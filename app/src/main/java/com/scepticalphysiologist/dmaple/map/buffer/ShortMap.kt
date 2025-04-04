package com.scepticalphysiologist.dmaple.map.buffer

import mil.nga.tiff.FieldType
import mil.nga.tiff.FileDirectory
import mil.nga.tiff.Rasters
import java.nio.ByteBuffer

/** A map consisting of short integers.
 *
 * We could maximize the range of distances encodable by using both negative and positive parts
 * of the (signed) short integers. However this requires adding and taking away Short.MIN values
 * which noticeably slows down processing and display of the map. Also just the positive portion
 * of the short gives 32767 values - more than enough!
 *
 * Using single bytes (256 values) may not be enough of a range for distance  as the
 * screen can be wider than 256 pixels.
 * */
class ShortMap(buffer: ByteBuffer, nx: Int): MapBufferView<Short>(buffer, nx) {

    override val fieldType = FieldType.SHORT

    override val bytesPerChannel = listOf(2)

    /** The maximum sample value. */
    private var maxv: Short = 0

    fun addDistance(value: Int) { add(value.toShort()) }

    override fun set(i: Int, j: Int, value: Short) { buffer.putShort(bufferPosition(i, j), value) }

    override fun get(i: Int, j: Int): Short { return buffer.getShort(bufferPosition(i, j)) }

    override fun add(value: Short) {
        buffer.putShort(value)
        if(value > maxv) maxv = value
    }

    override fun getColorInt(i: Int, j: Int): Int {
        val v = (255f * get(i, j).toFloat() / maxv).toInt()
        return (255 shl 24) or
                (v and 0xff shl 16) or
                (v and 0xff shl 8) or
                (v and 0xff shl 0)
    }

    override fun toTiffDirectory(identifier: String, y: Int?): FileDirectory  {
        // Directory
        val dir = super.toTiffDirectory(identifier, y)
        dir.bitsPerSample = listOf(16)
        dir.samplesPerPixel = 1
        val dimen = Pair(dir.imageWidth.toInt(), dir.imageHeight.toInt())

        // Raster.
        val raster = Rasters(dimen.first, dimen.second, 1,  FieldType.SHORT, buffer.order())
        copyBuffer(src=buffer, dst=raster.sampleValues[0])

        // Add raster to directory.
        dir.setRowsPerStrip(raster.calculateRowsPerStrip(dir.planarConfiguration))
        dir.writeRasters = raster
        return dir
    }

    override fun fromTiffDirectory(dir: FileDirectory): Pair<Int, Int> {
        val raster = dir.readRasters()
        val dimen = Pair(raster.width, raster.height)
        nx = dimen.first
        copyBuffer(src = raster.sampleValues[0], dst = buffer)

        // Reset maximum.
        maxv = 0
        val n = dimen.first * dimen.second
        for(i in 0 until n) {
            val v = buffer.getShort(i * 2)
            if(v > maxv) maxv = v
        }
        return dimen
    }

}
