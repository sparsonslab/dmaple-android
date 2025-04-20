// Copyright (c) 2025-2025. Dr Sean Paul Parsons. All rights reserved.

package com.scepticalphysiologist.dmaple.map.buffer

import mil.nga.tiff.FieldType
import java.nio.ByteBuffer

/** A map consisting of RGB color values. */
class RGBMap(buffer: ByteBuffer, nx: Int): MapBufferView<Int>(buffer, nx) {

    override var channelTypes = listOf(FieldType.BYTE, FieldType.BYTE, FieldType.BYTE).toTypedArray()

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

}
