package com.scepticalphysiologist.dmaple.map.creator.buffer

import mil.nga.tiff.FieldType
import mil.nga.tiff.FileDirectory
import mil.nga.tiff.Rasters
import java.nio.ByteBuffer


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

    override fun fromTiffDirectory(dir: FileDirectory): Pair<Int, Int> {
        val rasterDimen =  super.fromTiffDirectory(dir)
        maxv = Short.MIN_VALUE
        for(i in 0 until currentSample()) {
            val v = buffer.getShort(i * bytesPerSample)
            if(v > maxv) maxv = v
        }
        return rasterDimen
    }
}
