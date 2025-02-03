package com.scepticalphysiologist.dmaple

import android.os.SystemClock.sleep
import com.scepticalphysiologist.dmaple.io.FileBackedBuffer
import java.io.File
import java.io.FileOutputStream
import java.io.RandomAccessFile
import java.nio.channels.FileChannel
import java.util.concurrent.TimeUnit
import kotlin.random.Random


fun main() {


    val dirName = "/Users/senparsons/Documents/programming/personal/dmaple_android"
    val fileName = "large_buffer.dat"

    // Check for file.
    val file = File(dirName, fileName)
    if(!file.exists()) file.createNewFile()

    // Make sure file is large enough.
    val targetSize = 10_000_000L
    val fileSize = file.length()
    if(fileSize < targetSize) {
        /*
        val strm = FileOutputStream(file, true)
        val n = targetSize - fileSize
        for(i in 1..n) strm.write(0)
        strm.close()
         */

        val strm = RandomAccessFile(file, "rw")
        strm.setLength(targetSize)
        strm.close()
    }

    //
    val strm = RandomAccessFile(file, "rw")
    val buffer = strm.channel.map(FileChannel.MapMode.READ_WRITE, 0, 11_000)


    //
    buffer.putDouble(10_450, 1.2345678)
    println(buffer.getDouble(10_450))
    println(buffer.getDouble(10_449))
    println(buffer.getDouble(10_400))


    strm.close()

    //TimeUnit.SECONDS.sleep(30)

}

