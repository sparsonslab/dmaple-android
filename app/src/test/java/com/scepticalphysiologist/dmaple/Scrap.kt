package com.scepticalphysiologist.dmaple

import com.scepticalphysiologist.dmaple.map.buffer.copyBuffer
import mil.nga.tiff.TIFFImage
import mil.nga.tiff.TiffReader
import org.junit.Test
import java.io.File
import java.io.RandomAccessFile
import java.nio.ByteBuffer
import java.nio.channels.FileChannel
import kotlin.math.floor
import kotlin.random.Random
import kotlin.system.measureTimeMillis

class Scrap {

    @Test
    fun `short map`() {

        val file = File("/Users/senparsons/Downloads/250402_134213_refac/mfgUiiSCnMjToNHiqldr_diameter.tiff")
        val np = 20

        val img = TiffReader.readTiff(file)
        val dir = img.fileDirectories[0]
        println("image = ${dir.imageWidth} x ${dir.imageHeight}")
        val t = measureTimeMillis {

            val buffer = dir.readRasters().sampleValues[0]
            val values = (0 until np).map{ buffer.getShort(it * 2) }
            println(values)

        }
        println("NGA = $t")


        assert(true)

    }

    @Test
    fun `rgb map`() {

        val file = File("/Users/senparsons/Downloads/250404_165551_oneminwithspine/ZOBmaycbEwhcjRYEtBDx_spine.tiff")
        val np = 20

        val img = TiffReader.readTiff(file, true)
        val dir = img.fileDirectories[0]
        val nx = dir.imageWidth.toInt()
        val ny = dir.imageHeight.toInt()
        println("$nx x $ny")
        val buffer = ByteBuffer.allocate(nx * ny * 3)

        val t = measureTimeMillis {
            val raster = dir.readInterleavedRasters()
            copyBuffer(src=raster.interleaveValues, dst=buffer)
        }
        val values = (0 until np).map{ buffer.get(it) }
        println(values)
        println("NGA = $t")


        assert(true)


    }

    @Test
    fun `by strips`() {

        // Get directory
        val file = File("/Users/senparsons/Downloads/250404_165551_oneminwithspine/ZOBmaycbEwhcjRYEtBDx_spine.tiff")
        val img = TiffReader.readTiff(file, true)
        val dir = img.fileDirectories[0]
        val nx = dir.imageWidth.toInt()
        val ny = dir.imageHeight.toInt()
        println("$nx x $ny")

        // Buffer
        val nb = nx * ny * 3
        val dstBuffer1 = ByteBuffer.allocate(nb)
        val dstBuffer2 = ByteBuffer.allocate(nb)

        // Full read.
        var t = measureTimeMillis {
            val raster = dir.readInterleavedRasters()
            copyBuffer(src=raster.interleaveValues, dst=dstBuffer1)
        }
        println("via raster = $t")

        // File-mapped read by strip
        t = measureTimeMillis {
            val offsets = dir.stripOffsets.map{it.toLong()}
            val lengths = dir.stripByteCounts.map{it.toLong()}
            val strm = RandomAccessFile(file, "r")
            var j: Long = 0
            for (i in offsets.indices) {
                val mbuffer = strm.channel.map(FileChannel.MapMode.READ_ONLY, offsets[i], lengths[i])
                dstBuffer2.put(mbuffer)
                j += lengths[i]
            }
            strm.channel.close()
            strm.close()
        }
        println("via mapped = $t")

        var nNotEqual = 0
        for(i in 0 until 1000) {
            val j = Random.nextInt(0, nb)
            if(dstBuffer1[j] != dstBuffer2[j]) nNotEqual += 1
        }
        println("N not equal = $nNotEqual")


    }





}
