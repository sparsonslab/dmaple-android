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

    override fun toRaster(i: Int, j: Int, raster: Rasters) {
        raster.setPixelSample(0, i, j, get(i, j))
    }

    override fun fromRaster(i: Int, j: Int, raster: Rasters) {
        val v = raster.getPixelSample(0, i, j).toShort()
        set(i, j, v)
    }

    override fun fromTiffDirectory(dir: FileDirectory): Pair<Int, Int> {
        val rasterDimen =  super.fromTiffDirectory(dir)
        maxv = 0
        for(i in 0 until currentSample()) {
            val v = buffer.getShort(i * bytesPerSample)
            if(v > maxv) maxv = v
        }
        return rasterDimen
    }
}
