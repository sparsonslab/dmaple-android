package com.scepticalphysiologist.dmaple.io

import java.io.File
import java.io.FileInputStream
import java.io.FileOutputStream
import java.nio.ByteBuffer

class NumericFile<T : Number>(
    val name: File,
    val example: T
) {

    val byteWidth = byteSize(example)

    init {
        if(!name.exists()) name.createNewFile()
    }

    fun clear() {
        name.delete()
        name.createNewFile()
    }

    fun size(): Long { return name.length() / byteWidth }

    fun write(values: List<T>, append: Boolean = true) {
        val buffer = ByteBuffer.allocate(values.size * byteWidth)
        when(example) {
            is Byte -> for(value in values) buffer.put(value as Byte)
            is Short -> for(value in values) buffer.putShort(value as Short)
            is Int -> for(value in values) buffer.putInt(value as Int)
            is Long -> for(value in values) buffer.putLong(value as Long)
            is Float -> for(value in values) buffer.putFloat(value as Float)
            is Double -> for(value in values) buffer.putDouble(value as Double)
        }
        val stream = FileOutputStream(name, append)
        stream.write(buffer.array())
        stream.close()
    }

    fun read(offset: Long = 0, length: Int? = null): MutableList<T> {

        val (off, len) = constrainedOffsetAndLength(size(), offset, length)
        if(offset != off) return mutableListOf()

        val stream = FileInputStream(name)
        val bytes = ByteArray(len * byteWidth)
        stream.skip(off * byteWidth)
        stream.read(bytes)
        stream.close()
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

fun constrainedOffsetAndLength(available: Long, offset: Long, length: Int?): Pair<Long, Int> {
   // println("available = $available, offset = $offset, length = $length")
    val off = if(offset in 0 until available) offset else 0
    val len =  length?.let {
        if((it < 1) || (off + it > available))  (available - off).toInt()
        else it
    } ?: (available - off).toInt()
    //println(" .... off = $off, len = $len")
    return Pair(off, len)
}

