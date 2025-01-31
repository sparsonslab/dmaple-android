package com.scepticalphysiologist.dmaple

import com.scepticalphysiologist.dmaple.io.FileBackedBuffer
import java.io.File
import kotlin.random.Random


fun main() {


    val dir = File("/Users/senparsons/Documents/programming/personal/dmaple_android/buffer.dat")


    val buffer = FileBackedBuffer<Int>(
        capacity = 100,
        default = 0,
        directory = File("/Users/senparsons/Documents/programming/personal/dmaple_android")
    )
    for(i in 0..450) {
        buffer.add(i)
        println("size = ${buffer.nBuffer()}")
    }

    println(buffer.nSamples())
    println(buffer.read(90, 1000))


    /*
    val numbFile = NumericFile(dir, 0f)
    numbFile.write((0..10).map{it * 0.1f}.toList(), append = false)

    var arr = numbFile.read()
    println(arr)

    numbFile.write((0..10).map{it * 1.2f}.toList(), append = true)
    arr = numbFile.read(3, 10)
    println(arr)
    */

}

