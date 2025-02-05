package com.scepticalphysiologist.dmaple


import com.scepticalphysiologist.dmaple.io.createByteChannelTiff
import mil.nga.tiff.TiffWriter
import java.io.File
import java.nio.ByteBuffer
import kotlin.math.PI
import kotlin.math.sin



fun main() {

    val w = 100
    val h = 128
    val waveLength = 52f
    val bytes = ByteArray(w * h)
    for(i in bytes.indices)
        bytes[i] = (127 * sin(i.toFloat() * 2 * PI / waveLength)).toInt().toByte()
    //println(bytes.slice(0..100).toList())

    val img = createByteChannelTiff(ByteBuffer.wrap(bytes), w, h, 1)
    var path = "/Users/senparsons/Documents/programming/personal/dmaple_android/example.tiff"
    TiffWriter.writeTiff(File(path), img)


    val buff = ByteBuffer.allocate(w * h * 3)
    var k: Int
    for(j in 20..60)
        for(i in 20..60) {
            k = 3 * (j * w + i)
            buff.put(k, -128)
            buff.put(k + 1, 127)
            buff.put(k + 2, 0)
        }

    val img2 = createByteChannelTiff(buff, w, h, 3)
    path = "/Users/senparsons/Documents/programming/personal/dmaple_android/example2.tiff"
    TiffWriter.writeTiff(File(path), img2)



}

