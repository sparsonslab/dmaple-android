package com.scepticalphysiologist.dmaple



import android.graphics.Color
import android.os.Build
import androidx.annotation.RequiresApi
import androidx.core.graphics.alpha
import androidx.core.graphics.blue
import androidx.core.graphics.green
import androidx.core.graphics.red
import com.scepticalphysiologist.dmaple.map.creator.RGBMap
import mil.nga.tiff.TIFFImage
import mil.nga.tiff.TiffWriter
import java.io.File
import java.nio.ByteBuffer

fun printColor(color: Int) {
    println("${color.alpha}, ${color.red}, ${color.green}, ${color.blue}")
}

@RequiresApi(Build.VERSION_CODES.UPSIDE_DOWN_CAKE)
fun main() {


    val nx = 100
    val ny = 100
    val buffer = RGBMap(ByteBuffer.allocate(nx * ny * 3), nx)
    for(i in 20..60)
        for(j in 40..60)
            buffer.set(i, j, Color.RED)

    println(Color.BLUE)
    println(buffer.getColorInt(50, 50))

    val dir = buffer.tiffDirectory(ny)
    val img = TIFFImage()
    img.add(dir)

    val path = "/Users/senparsons/Documents/programming/personal/dmaple_android/example6.tiff"
    TiffWriter.writeTiff(File(path), img)


    //val g: Int = (c shr 8).and(0xff)
    //print(g)


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

