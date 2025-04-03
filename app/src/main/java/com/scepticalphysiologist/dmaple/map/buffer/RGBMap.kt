package com.scepticalphysiologist.dmaple.map.buffer

import android.graphics.Color
import androidx.core.graphics.blue
import androidx.core.graphics.green
import androidx.core.graphics.red
import mil.nga.tiff.FieldType
import mil.nga.tiff.Rasters
import java.nio.ByteBuffer

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

    override fun directFromRaster(raster: Rasters) {
        nx = raster.width
        buffer.position(0)
        //println("n raster buffer = ${raster.sampleValues.size}")

        /// makes the app crash! causes the external files directory to not be mounted,
        //val n = 1 //raster.interleaveValues.capacity()
        //val m = 3 * raster.width * raster.height
        //println("n = $n, m = $m")

        println("interleaved = ${raster.hasInterleaveValues()}")

        val channels = raster.sampleValues
        val n = raster.width * raster.height
        try {
            for (i in 0 until n)
                buffer.putInt(
                    Color.rgb(
                        channels[0].getInt(i), channels[1].getInt(i), channels[2].getInt(i)
                    )
                )
        } catch(_: java.lang.IndexOutOfBoundsException) {}
/*

        buffer.put(raster.sampleValues[0])


        for(j in 0 until raster.height)
            for(i in 0 until raster.width)
                buffer.putInt(
                    Color.argb(
                        255,
                        raster.getPixelSample(0, i, j).toInt(),
                        raster.getPixelSample(1, i, j).toInt(),
                        raster.getPixelSample(2, i, j).toInt()
                    )
                )
        */

    }

}
