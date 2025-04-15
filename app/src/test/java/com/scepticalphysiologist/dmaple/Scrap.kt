package com.scepticalphysiologist.dmaple


import com.scepticalphysiologist.dmaple.map.creator.rationalToFloat
import org.junit.Test
import java.nio.ByteBuffer


class Scrap {

    @Test
    fun `yuv back forward`(){

        val x = listOf<Byte>(-120, -54, -10, 23, 88, 112)

        val y = x.map{it.toInt() and 0xff}

        val z = y.map{it.toFloat().toInt().toByte()}

        for(i in 0 until x.size)
            println("${x[i]} > ${y[i]} > ${z[i]}")



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


