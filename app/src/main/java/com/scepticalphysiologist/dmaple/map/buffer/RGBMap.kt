package com.scepticalphysiologist.dmaple.map.buffer

import mil.nga.tiff.FieldType
import mil.nga.tiff.FileDirectory
import mil.nga.tiff.Rasters
import java.nio.ByteBuffer

/** A map consisting of RGB color values. */
class RGBMap(buffer: ByteBuffer, nx: Int): MapBufferView<Int>(buffer, nx) {

    override val fieldType = FieldType.BYTE

    override var bytesPerChannel = listOf(1, 1, 1)

    override fun addSample(value: Int) {
        buffer.put((value shr 16).toByte())
        buffer.put((value shr 8).toByte())
        buffer.put((value shr 0).toByte())
    }

    override fun getSample(i: Int): Int {
        val k = i * 3
        return  (255 shl 24) or
                (buffer[k].toInt() and 0xff shl 16) or
                (buffer[k + 1].toInt() and 0xff shl 8) or
                (buffer[k + 2].toInt() and 0xff shl 0)
    }

    override fun setSample(i: Int, value: Int) {
        val k = i * 3
        buffer.put(k,     (value shr 16).toByte())
        buffer.put(k + 1, (value shr 8).toByte())
        buffer.put(k + 2, (value shr 0).toByte())
    }

    override fun getColorInt(i: Int, j: Int): Int { return getPixel(i, j) }

    override fun toTiffDirectory(identifier: String, y: Int?): FileDirectory {
        // Directory
        val dir = super.toTiffDirectory(identifier, y)
        dir.bitsPerSample = listOf(8, 8, 8)
        dir.samplesPerPixel = 3
        val dimen = Pair(dir.imageWidth.toInt(), dir.imageHeight.toInt())

        // Interleaved raster.
        val interleave =  ByteBuffer.allocate(dimen.first * dimen.second * 3) // might be larger than memory!!
        interleave.order(buffer.order())
        val raster = Rasters(
            dimen.first, dimen.second,
            listOf(FieldType.BYTE, FieldType.BYTE, FieldType.BYTE).toTypedArray(),
            interleave  // hand the raster the file-mapped buffer directly?
        )
        copyBuffer(src=buffer, dst=raster.interleaveValues)

        // Add raster to directory.
        dir.setRowsPerStrip(raster.calculateRowsPerStrip(dir.planarConfiguration))
        dir.writeRasters = raster
        return dir
    }


}
