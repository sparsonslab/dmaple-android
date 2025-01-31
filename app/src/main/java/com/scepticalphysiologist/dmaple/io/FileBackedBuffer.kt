package com.scepticalphysiologist.dmaple.io

import androidx.collection.CircularArray
import java.io.File

/** A buffer that backs-up data to a file as the buffer approaches its capacity.
 *
 * @param T The numeric type of the samples in the buffer.
 * @property capacity The capacity of the buffer (samples).
 * @property directory The directory in which to write the backing file.
 * @param backUpThreshold The fraction of the capacity at which the buffer is backed-up.
 * @param backUpFraction The fraction of the capacity to be backed-up at each back-up event.
 */
class FileBackedBuffer<T : Number>(
    private val capacity: Int,
    private var directory: File,
    backUpThreshold: Float = 0.95f,
    backUpFraction: Float = 0.4f,
){

    private val wf: Int = (backUpFraction * capacity).toInt()
    private val wt: Int = (backUpThreshold * capacity).toInt()

    private val buffer = CircularArray<T>(minCapacity = capacity)
    private var file: NumericFile<T>? = null

    init {
        if((!directory.isDirectory) || (!directory.exists()))
            directory = File("")
    }

    private fun initiateFile(value: T) {
        val path = File(directory, "buffer.dat")
        if(path.exists()) path.delete()
        file = NumericFile(path, value)
    }

    /** The number of samples in the buffer. */
    fun nBuffer(): Int { return buffer.size() }

    /** The number of samples in the back-up and buffer. */
    fun nSamples(): Long { return (file?.size() ?: 0) + buffer.size() }

    /** Add a sample to the buffer. */
    fun add(value: T) {
        // Initiate backing file if has not be initiated already.
        if(file == null) initiateFile(value)
        // Buffer size has surpassed threshold - back-up.
        if(buffer.size() >= wt) {
            file?.write((0 until wf).map { buffer[it] })
            buffer.removeFromStart(wf)
        }
        // Add value to buffer.
        buffer.addLast(value)
    }

    /** Get the ith sample of the buffer. */
    fun get(i: Int): T { return buffer[i] }

    /** Read a selection of samples contiguous across the back-up and buffer. */
    fun read(offset: Long = 0, length: Int? = null): List<T> {

        file?.let {
            val (off, len) = constrainedOffsetAndLength(nSamples(), offset, length)
            val arr = it.read(off, len)
            val n = len - arr.size
            for(i in 0 until n) arr.add(buffer[i])
            return arr
        }
        return listOf()
    }

}
