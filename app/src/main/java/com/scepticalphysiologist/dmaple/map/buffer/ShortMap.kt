// Copyright (c) 2025-2025. Dr Sean Paul Parsons. All rights reserved.

package com.scepticalphysiologist.dmaple.map.buffer

import mil.nga.tiff.FieldType
import mil.nga.tiff.FileDirectory
import java.io.RandomAccessFile
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

    override val channelTypes = listOf(FieldType.SHORT).toTypedArray()

    /** The maximum sample value. */
    private var maxv: Short = 0

    override fun addSample(value: Short) {
        buffer.putShort(value)
        if(value > maxv) maxv = value
    }

    override fun getSample(i: Int): Short { return buffer.getShort(i * 2) }

    override fun setSample(i: Int, value: Short) { buffer.putShort(i * 2, value) }

    override fun getColorInt(i: Int, j: Int): Int {
        val v = (255f * getPixel(i, j).toFloat() / maxv).toInt()
        return (255 shl 24) or
                (v and 0xff shl 16) or
                (v and 0xff shl 8) or
                (v and 0xff shl 0)
    }

    /** Add a distance. */
    fun addDistance(value: Int) { addSample(value.toShort()) }

    override fun fromTiffDirectory(dir: FileDirectory, stream: RandomAccessFile) {
        super.fromTiffDirectory(dir, stream)
        // Reset maximum.
        maxv = 0
        for(i in 0 until buffer.position() / 2) {
            val v = getSample(i)
            if(v > maxv) maxv = v
        }
    }
}
