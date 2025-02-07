package com.scepticalphysiologist.dmaple.io

import android.os.Build
import androidx.annotation.RequiresApi
import androidx.core.graphics.blue
import androidx.core.graphics.green
import androidx.core.graphics.red
import mil.nga.tiff.FieldType
import mil.nga.tiff.FileDirectory
import mil.nga.tiff.Rasters
import mil.nga.tiff.util.TiffConstants
import java.lang.IndexOutOfBoundsException
import java.lang.Math.floorDiv
import java.nio.ByteBuffer


abstract class BufferView<T : Number>(
    protected val buffer: ByteBuffer,
    protected val nx: Int
) {

    protected abstract val samplesPerPixel: Int

    protected abstract val bitsPerSample: Int

    protected abstract val fieldType: FieldType

    fun tiffDirectory(y: Int? = null): FileDirectory {

        val ny = y ?: floorDiv(buffer.position(), nx)

        val dir = FileDirectory()
        dir.setImageWidth(nx)
        dir.setImageHeight(ny)
        dir.samplesPerPixel = samplesPerPixel
        dir.setBitsPerSample(bitsPerSample)

        val raster = Rasters(nx, ny, samplesPerPixel, fieldType)
        dir.compression = TiffConstants.COMPRESSION_NO
        dir.planarConfiguration = TiffConstants.PLANAR_CONFIGURATION_CHUNKY
        dir.setRowsPerStrip(raster.calculateRowsPerStrip(dir.planarConfiguration))
        dir.writeRasters = raster

        try {
            for(j in 0 until ny)
                for(i in 0 until nx)
                    setPixel(i, j, raster)
        } catch(_: IndexOutOfBoundsException) {}
        return dir
    }

    abstract fun setPixel(i: Int, j: Int, raster: Rasters)

    abstract fun get(i: Int, j: Int): T

    abstract fun set(i: Int, j: Int, value: T)

    abstract fun add(value: T)

    abstract fun getColorInt(i: Int, j: Int): Int

}


class RGBView(buffer: ByteBuffer, nx: Int): BufferView<Int>(buffer, nx) {

    override val samplesPerPixel = 3

    override val bitsPerSample = 8

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


class ShortView(buffer: ByteBuffer, nx: Int): BufferView<Short>(buffer, nx) {

    override val samplesPerPixel = 1

    override val bitsPerSample = 16

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


