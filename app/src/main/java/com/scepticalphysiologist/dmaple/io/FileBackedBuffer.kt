package com.scepticalphysiologist.dmaple.io

import androidx.collection.CircularArray
import com.scepticalphysiologist.dmaple.MainActivity
import java.io.File
import kotlin.random.Random

/** A buffer that backs-up data to a file as the buffer approaches its capacity.
 *
 * This is used for recording maps. Bu occasionally backing up to file, maps can be
 * recorded in access of the heap size of the app.
 */
class FileBackedBuffer<T : Number>(
    /** The capacity of the buffer (samples). */
    private val capacity: Int,
    /** The directory in which to write the backing file. */
    private var directory: File,
    /**A default value to return from the buffer if a get() index is out of range of the buffer.*/
    private val default: T,
    /** The fraction of the capacity at which the buffer is backed-up. */
    backUpThreshold: Float = 0.95f,
    /** The fraction of the capacity to be backed-up at each back-up event. */
    backUpFraction: Float = 0.4f,
) {

    /** The number of samples in the buffer at which back-up is initiated. */
    private val wt: Int = (backUpThreshold * capacity).toInt()
    /** The number of samples written to file at each back-up event. */
    private val wf: Int = (backUpFraction * capacity).toInt()
    /** A circular array that acts as the buffer. */
    private val buffer = CircularArray<T>(minCapacity = capacity)
    /** A file into which [wf] samples are written each time the buffer size reaches [wt]. */
    private var file: NumericFile<T>? = null
    /** The number of samples in the file. */
    private var nf: Int = 0

    init {
        if((!directory.isDirectory) || (!directory.exists())) directory = File("")
    }

    /** Initiate the back-up file. */
    private fun initiateFile(value: T) {
        val path = File(directory, "buffer_${randomAlphaString(20)}.dat")
        if(path.exists()) path.delete()
        file = NumericFile(path, value)
    }

    /** The number of samples in the buffer. */
    fun nBuffer(): Int { return buffer.size() }

    /** The number of samples in the back-up and buffer. */
    fun nSamples(): Long { return (file?.nSamples() ?: 0) + buffer.size() }

    /** Add a sample to the buffer. */
    fun add(value: T) {
        // Initiate backing file if has not be initiated already.
        if(file == null) initiateFile(value)
        // Buffer size has surpassed threshold - back-up.
        if(buffer.size() >= wt) {
            // ... write file.
            file?.let{
                it.write((0 until wf).map { buffer[it] }, append = true)
                nf = it.nSamples().toInt()
            }
            // ... remove from buffer.
            buffer.removeFromStart(wf)
            val mem = MainActivity.freeBytes()
            println("buffer > file: buffer = ${buffer.size()}, file = ${file?.nSamples()}, free = $mem")
        }
        // Add value to buffer.
        buffer.addLast(value)
    }

    // todo - allow sample retreival from file stream - can then view maps as far back as needed.
    /** Get the ith sample of the buffer. If the sample is in the file, return the default value. */
    fun get(i: Int): T {
        try { return if(i >= nf) buffer[i - nf] else default }
        // Null exception is sometimes called at the end (stop) of a long recording.
        catch(_: NullPointerException) {}
        return default
    }

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

    /** Back-up whatever samples are remaining in the buffer to the file. */
    fun writeRemainingSamples() {
        file?.let {
            it.write((0 until buffer.size()).map{buffer[it]}, append = true)
            buffer.clear()
            nf = it.nSamples().toInt()
        }
    }

    /** Release the buffer's resources. */
    fun release() {
        buffer.clear()
        file?.delete()
    }

}

fun randomAlphaString(n: Int): String {
    val alphas = ('a'..'z').map{it} + ('A'..'Z').map{it}
    return (0 until n).map{alphas[Random.nextInt(alphas.size)]}.joinToString("")
}

