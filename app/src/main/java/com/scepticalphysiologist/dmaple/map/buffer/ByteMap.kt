// Copyright (c) 2025-2025. Dr Sean Paul Parsons. All rights reserved.

package com.scepticalphysiologist.dmaple.map.buffer

import android.graphics.Color
import mil.nga.tiff.FieldType
import java.nio.ByteBuffer

/** A map consisting of single byte values. */
class ByteMap(buffer: ByteBuffer, nx: Int): MapBufferView<Byte>(buffer, nx) {

    override val channelTypes = listOf(FieldType.SBYTE).toTypedArray()

    override fun addSample(value: Byte) { buffer.put(value) }

    override fun getSample(i: Int): Byte { return buffer.get(i) }

    override fun setSample(i: Int, value: Byte) { buffer.put(i, value) }

    override fun getColorInt(i: Int, j: Int): Int {
        val v = getPixel(i, j).toUByte().toInt()
        return Color.rgb(v, v, v)
    }

}
