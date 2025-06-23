// Copyright (c) 2025-2025. Dr Sean Paul Parsons. All rights reserved.

package com.scepticalphysiologist.dmaple


import com.google.gson.GsonBuilder
import com.scepticalphysiologist.dmaple.geom.Edge
import com.scepticalphysiologist.dmaple.geom.Frame
import com.scepticalphysiologist.dmaple.geom.Point
import com.scepticalphysiologist.dmaple.map.field.FieldRoi
import org.junit.Test
import java.io.BufferedWriter
import java.io.File
import java.io.FileOutputStream
import java.io.FileWriter
import java.io.OutputStream
import java.nio.ByteBuffer
import kotlin.system.measureTimeMillis


class ExampleClass(
    val roi: FieldRoi?,
    val scalar: Float = 1f
)


class Scrap {

    @Test
    fun `average speed test`() {

        val arr = IntArray(1000)
        var mu: Double
        var t: Long

        t = measureTimeMillis {
            mu = arr.map{it.toFloat() + 3.2f}.average()
        }
        println("mapping\t$t\t$mu")

        t = measureTimeMillis {
            mu = 0.0
            for(i in arr.indices) mu += arr[i].toFloat() + 3.2f
            mu /= arr.size
        }
        println("cumulative\t$t\t$mu")

    }

    @Test
    fun `serialize nulls`() {

        println("=======================")
        val roi = FieldRoi(
            frame=Frame(Point(100f, 100f), orientation = 180),
            c0 = Point(34.6f, 9.5f), c1 = Point(98f, 34.5f)
        )

        // serailize
        val ex = ExampleClass(roi = null)
        val gson = GsonBuilder().serializeNulls().create()
        val s = gson.toJson(ex)
        println(s)

        // deserialize
        val exde = gson.fromJson(s, ExampleClass::class.java)
        println(exde.roi)


    }

    @Test
    fun `write roi`() {

        val roi = FieldRoi(
            frame = Frame(size = Point(500f, 500f)),
            c0 = Point(100f, 100f),
            c1 = Point(210f, 405f),
            seedingEdge = Edge.RIGHT,
            uid = "my_roi"
        )
        val folder = File("/Users/senparsons/Downloads")



        val buffer = ByteBuffer.allocate(100)
        buffer.put(byteArrayOf(73, 111, 117, 116))// "Iout"
        buffer.put(byteArrayOf())

        /*
        void putShort(int base, int v) {
            data[base] = (byte)(v>>>8);
            data[base+1] = (byte)v;
        }
        */

        val path = File(folder, "${roi.uid}.roi")
        val strm = FileOutputStream(path)

    }


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


