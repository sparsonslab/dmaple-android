// Copyright (c) 2025-2025. Dr Sean Paul Parsons. All rights reserved.

package com.scepticalphysiologist.dmaple.map.buffer

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
 * @param bufferByteSize The size of each buffer in bytes.
 * the maximum number of maps likely to be recorded at one time.
 */
class MapBufferProvider(
    private val sourceDirectory: File,
    nBuffers: Int,
    private val bufferByteSize: Long
) {

    /** Buffer file names mapped to their streams or null if the buffer is not available (has
     * not been initiated or is being used already). */
    private val fileStreams: MutableMap<String, RandomAccessFile?> =
        (1..nBuffers).associate { "buffer_$it.dat" to null }.toMutableMap()

    /** Initialise the buffering files. */
    fun initialiseBuffers() {
        for((bufferFile, accessStream) in fileStreams) {
            if(accessStream != null) continue
            val file = File(sourceDirectory, bufferFile)
            if(!file.exists()) file.createNewFile()
            // If the file isn't the required size, extend it.
            if(file.length() < bufferByteSize) {
                val strm = RandomAccessFile(file, "rw")
                try { strm.setLength(bufferByteSize) }
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

    /** Get the allocated buffer size (bytes). */
    fun bufferSize(): Long { return bufferByteSize }

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
