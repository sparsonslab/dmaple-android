package com.scepticalphysiologist.dmaple.map.buffer

import androidx.compose.runtime.produceState
import com.scepticalphysiologist.dmaple.map.buffer.MapBufferProvider
import org.junit.Assert.assertEquals
import org.junit.Test
import org.robolectric.util.TempDirectory
import java.io.File
import java.io.RandomAccessFile


class MapBufferProviderTest {

    @Test
    fun `buffers initialised then used then freed`() {
        // Given: A provider of two buffers of 100 bytes each.
        val source = TempDirectory().create("test").toFile()
        val provider = MapBufferProvider(sourceDirectory = source, nBuffers = 2, bufferByteSize = 100)

        // When: The buffers are initialised.
        provider.initialiseBuffers()

        // Then: Buffer files of the correct number and size have been created and they are all free,
        assertEquals(2, source.listFiles()?.size)
        assertEquals(listOf(100L, 100L), source.listFiles()?.map{it.length()})
        assertEquals(2, provider.nFreeBuffers())

        // When: One buffer is provided.
        var buffer = provider.getFreeBuffer()

        // Then: The provided buffer has the required capacity and one free buffer is left.
        assertEquals(100, buffer?.capacity())
        assertEquals(1, provider.nFreeBuffers())

        // When: The remaining buffer is used and another buffer is requested.
        provider.getFreeBuffer()
        buffer = provider.getFreeBuffer()

        // Then: The last buffer returned is null and there are no free buffers.
        assertEquals(null, buffer)
        assertEquals(0, provider.nFreeBuffers())

        // When: All buffers are freed.
        provider.freeAllBuffers()

        // Then: There are two free buffers.
        assertEquals(2, provider.nFreeBuffers())
    }

    @Test
    fun `buffers extended`() {
        // Given: An existing buffer file of 100 bytes.
        val source = TempDirectory().create("test").toFile()
        val bufferFile = File(source, "buffer_1.dat")
        bufferFile.createNewFile()
        val strm = RandomAccessFile(bufferFile, "rw")
        strm.setLength(100)
        strm.close()

        // When: The provider is initiated with the same source directory, with a buffer size of 200 bytes.
        val provider = MapBufferProvider(sourceDirectory = source, nBuffers = 1, bufferByteSize = 200)
        provider.initialiseBuffers()

        // Then: The buffer file now has a size of 200 bytes.
        assertEquals(200L, bufferFile.length())

        // When: The buffer is provided.
        val buffer = provider.getFreeBuffer()

        // Then: The buffer has a size of 200 bytes.
        assertEquals(200, buffer?.capacity())
    }

}
