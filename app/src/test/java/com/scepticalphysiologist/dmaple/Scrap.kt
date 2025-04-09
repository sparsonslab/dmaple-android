package com.scepticalphysiologist.dmaple


import java.nio.ByteBuffer


class Scrap {

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


