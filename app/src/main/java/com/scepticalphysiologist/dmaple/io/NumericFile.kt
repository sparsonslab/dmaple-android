package com.scepticalphysiologist.dmaple.io

import java.io.File
import java.io.FileInputStream
import java.io.FileOutputStream
import java.nio.ByteBuffer

/** A binary file of numbers. */
class NumericFile<T : Number>(
    /** The full path of the file. */
    private val name: File,
    /** An example of the number of type. */
    private val example: T
) {

    /** The number of bytes required to represent one number. */
    private val byteWidth = byteSize(example)

    init { if(!name.exists()) name.createNewFile() }

    /** Clear the file. */
    fun clear() {
        name.delete()
        name.createNewFile()
    }

    /** Delete the file. */
    fun delete() { name.delete() }

    /** The sample sie of the file. */
    fun nSamples(): Long { return name.length() / byteWidth }

    /** The full path of the file. */
    fun path(): String { return name.absolutePath }

    /** Write sample to the file. */
    fun write(values: List<T>, append: Boolean = true) {
        println("writing: ${path()}")
        // Write to a byte buffer.
        val buffer = ByteBuffer.allocate(values.size * byteWidth)
        when(example) {
            is Byte -> for(value in values) buffer.put(value as Byte)
            is Short -> for(value in values) buffer.putShort(value as Short)
            is Int -> for(value in values) buffer.putInt(value as Int)
            is Long -> for(value in values) buffer.putLong(value as Long)
            is Float -> for(value in values) buffer.putFloat(value as Float)
            is Double -> for(value in values) buffer.putDouble(value as Double)
        }
        // Write the buffer to the file.
        val stream = FileOutputStream(name, append)
        stream.write(buffer.array())
        stream.close()
    }

    /** Read samples from the file.
     *
     * @param offset The offset into the file.
     * If greater than the file length, no samples are returned.
     * @param length The number of samples to obtain.
     * If [offset] + [length] are greater than the file length, samples are returned to the end of the file.
     * @return The samples.
     * */
    fun read(offset: Long = 0, length: Int? = null): MutableList<T> {
        // Check the offset and length.
        val (off, len) = constrainedOffsetAndLength(nSamples(), offset, length)
        if(offset != off) return mutableListOf()
        // Read the file into a byte array.
        val stream = FileInputStream(name)
        val bytes = ByteArray(len * byteWidth)
        stream.skip(off * byteWidth)
        stream.read(bytes)
        stream.close()
        // Read the samples from the bytes.
        val buffer = ByteBuffer.wrap(bytes)
        val values = mutableListOf<T>()
        when(example) {
            is Byte -> repeat(len){values.add(buffer.get() as T)}
            is Short -> repeat(len){values.add(buffer.getShort() as T)}
            is Int -> repeat(len){values.add(buffer.getInt() as T)}
            is Long -> repeat(len){values.add(buffer.getLong() as T)}
            is Float -> repeat(len){values.add(buffer.getFloat() as T)}
            is Double -> repeat(len){values.add(buffer.getDouble() as T)}
        }
        return values
    }

    /** The number of bytes to represent the value. */
    private fun byteSize(value: T): Int {
        return when(value) {
            is Byte -> 1
            is Short -> 2
            is Int -> 4
            is Long -> 8
            is Float -> 4
            is Double -> 8
            else -> 0
        }
    }

}

/** For sub-sampling an array, constrain the sample-sample offset and length.
 *
 * @param arrayLength The length of the array.
 * @param offset The offset into the array.
 * @param subSampleLength The length the sub-sample.
 * @return A pair of the constrained offset and length.
 */
fun constrainedOffsetAndLength(arrayLength: Long, offset: Long, subSampleLength: Int?): Pair<Long, Int> {
    val off = if(offset in 0 until arrayLength) offset else 0
    val len =  subSampleLength?.let {
        if((it < 1) || (off + it > arrayLength))  (arrayLength - off).toInt()
        else it
    } ?: (arrayLength - off).toInt()
    return Pair(off, len)
}
