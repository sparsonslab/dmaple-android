package com.scepticalphysiologist.dmaple

import android.graphics.Color
import android.os.Build
import android.view.Surface
import androidx.annotation.RequiresApi
import androidx.core.graphics.alpha
import androidx.core.graphics.blue
import androidx.core.graphics.green
import androidx.core.graphics.red
import com.google.gson.Gson
import com.scepticalphysiologist.dmaple.etc.Edge
import com.scepticalphysiologist.dmaple.etc.Frame
import com.scepticalphysiologist.dmaple.etc.Point
import com.scepticalphysiologist.dmaple.map.MappingRoi
import com.scepticalphysiologist.dmaple.map.creator.MapType
import com.scepticalphysiologist.dmaple.map.creator.RGBMap
import mil.nga.tiff.TIFFImage
import mil.nga.tiff.TiffWriter
import java.io.File
import java.nio.ByteBuffer

import android.graphics.RectF

fun printColor(color: Int) {
    println("${color.alpha}, ${color.red}, ${color.green}, ${color.blue}")
}

fun main() {



    val root = File("/Users/senparsons/Downloads/250219_103317_gloop")


    /*

    val nx = 100
    val ny = 100
    val buffer = ByteBuffer.allocate(nx * ny * 3)
    val map = RGBMap(buffer, nx)
    for(i in 20..60)
        for(j in 40..60)
            map.set(i, j, Color.RED)

    buffer.position(nx * ny * 3 - 1)


    println(map.getColorInt(50, 50))

    val img = TIFFImage()
    img.add(map.tiffDirectory(ny))
    img.add(map.tiffDirectory(ny))

    val path = "/Users/senparsons/Documents/programming/personal/dmaple_android/example10.tiff"
    TiffWriter.writeTiff(File(path), img)


    //val g: Int = (c shr 8).and(0xff)
    //print(g)

*/

    /*
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
*/


}

