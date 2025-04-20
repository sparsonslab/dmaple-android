// Copyright (c) 2025-2025. Dr Sean Paul Parsons. All rights reserved.

package com.scepticalphysiologist.dmaple.map.buffer

import android.graphics.Color
import com.scepticalphysiologist.dmaple.assertNumbersEqual
import com.scepticalphysiologist.dmaple.map.creator.findTiff
import mil.nga.tiff.TIFFImage
import mil.nga.tiff.TiffReader
import mil.nga.tiff.TiffWriter
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotNull
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config
import org.robolectric.shadows.ShadowColor
import java.io.File
import java.io.RandomAccessFile
import java.nio.ByteBuffer
import java.util.Random


@RunWith(RobolectricTestRunner::class)
@Config(shadows = [ShadowColor::class])
class RGBMapTest {

    @Test
    fun `set and get round trip`() {
        // Given: An RGB-type map.
        val map = RGBMap(buffer = ByteBuffer.allocate(1000), nx = 10)

        // When: A pixel value is set.
        val setPixelValue: Int = Color.RED
        map.setPixel(8, 5, setPixelValue)

        // Then: The pixel returned by get is the same value as set.
        assertEquals(setPixelValue, map.getPixel(8, 5))
    }

    @Test
    fun `to and from tiff round trip`() {
        // Given: A RGB-type map with random colors but constant alpha (non-transparent).
        val map = RGBMap(buffer = ByteBuffer.allocate(10_000), nx = 10)
        val rng = Random()
        val pixels = (0..100).map{
            Color.argb(1f, rng.nextFloat(), rng.nextFloat(), rng.nextFloat())
        }
        for(pixel in pixels) map.addSample(pixel)

        // When: The map is written to a TIFF image
        val mapId = "abcdef"
        val dir = map.toTiffDirectory(mapId)
        val file = File.createTempFile("image", "tiff")
        TiffWriter.writeTiff(file, TIFFImage().also { it.add(dir) })
        // ... and then read back.
        val imageRead  = TiffReader.readTiff(file)
        val dirRead = findTiff(imageRead.fileDirectories, mapId)

        // Then: The directory read back is not null.
        assertNotNull(dirRead)

        // Then: The read-back colors match those written.
        val strm = RandomAccessFile(file, "r")
        map.fromTiffDirectory(dirRead!!, strm)
        strm.channel.close()
        strm.close()
        assertNumbersEqual(
            expected = pixels,
            actual = pixels.indices.map{map.getSample(it)}.toList()
        )
    }

}
