package com.scepticalphysiologist.dmaple.map.creator.buffer

import java.io.File
import java.io.IOException
import java.io.RandomAccessFile
import java.nio.MappedByteBuffer
import java.nio.channels.FileChannel

/** Provides file-mapped byte buffers for holding spatio-temporal map data.
 *
 * File-mapped byte buffers can be very large (up to 2GB) without taking up any actual heap memory.
 * https://developer.android.com/reference/java/nio/MappedByteBuffer
 *
 * @param sourceDirectory The directory in which the buffer files will be contained
 * @param nBuffers The number of buffer files. There needs to be one per map. i.e. set this to
 * the maximum number of maps likely to be recorded at one time.
 */
class MapBufferProvider(
    private val sourceDirectory: File,
    nBuffers: Int
) {

    /** Buffer file names mapped to their streams or null if the buffer is not available (has
     * not been initiated or is being used already). */
    private val fileStreams: MutableMap<String, RandomAccessFile?> =
        (1..nBuffers).associate { "buffer_$it.dat" to null }.toMutableMap()

    /** The size (bytes) of each buffering file listed in [fileStreams].
     *
     * 100 MB ~= 60 min x 60 sec/min x 30 frame/sec x 1000 bytes/frame.
     * */
    private val MAP_BUFFER_SIZE: Long = 100_000_000L

    /** Initialise the buffering files. */
    fun initialiseBuffers() {
        for((bufferFile, accessStream) in fileStreams) {
            if(accessStream != null) continue
            val file = File(sourceDirectory, bufferFile)
            if(!file.exists()) file.createNewFile()
            val fileSize = file.length()
            if(fileSize < MAP_BUFFER_SIZE) {
                val strm = RandomAccessFile(file, "rw")
                try { strm.setLength(MAP_BUFFER_SIZE) }
                // ... in case there isn't enough memory available.
                catch (_: IOException) {
                    strm.close()
                    return
                }
                strm.close()
            }
        }
    }

    /** The number of buffers available for mapping. */
    fun nFreeBuffers(): Int { return fileStreams.filterValues{it == null}.size }

    /** Get a free buffer or null if no buffers are free. */
    fun getFreeBuffer(): MappedByteBuffer? {
        for((bufferFile, accessStream) in fileStreams) {
            if(accessStream != null) continue
            val file = File(sourceDirectory, bufferFile)
            val strm = RandomAccessFile(file, "rw")
            fileStreams[bufferFile] = strm
            return strm.channel.map(FileChannel.MapMode.READ_WRITE, 0, file.length())
        }
        return null
    }

    /** Free all buffers. */
    fun freeAllBuffers() {
        for((bufferFile, accessStream) in fileStreams) {
            accessStream?.channel?.close()
            accessStream?.close()
            fileStreams[bufferFile] = null
        }
    }
}
