package com.scepticalphysiologist.dmaple.map.creator

import android.graphics.Color
import com.scepticalphysiologist.dmaple.assertNumbersEqual
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
import java.nio.ByteBuffer

@RunWith(RobolectricTestRunner::class)
@Config(shadows = [ShadowColor::class])
class ShortMapTest {

    @Test
    fun `set and get round trip`() {
        // Given: A short-type map.
        val map = ShortMap(buffer = ByteBuffer.allocate(1000), nx = 10)

        // When: A pixel value is set.
        val setPixelValue: Short = 145
        map.set(8, 5, setPixelValue)

        // Then: The pixel returned by get is the same value as set.
        assertEquals(setPixelValue, map.get(8, 5))
    }

    @Test
    fun `correct distance to color mapping`() {
        // Given: A short-type map.
        val map = ShortMap(buffer = ByteBuffer.allocate(1000), nx = 10)

        // When: A series of distances (diameters or radii) added.
        val pixels = listOf<Int>(23, 67, 0, 101, 67, 456)
        for(pixel in pixels) map.addDistance(pixel)

        // Then: The maximum distance is shown as a color with maximum values (1f) for all channels.
        val color = Color.valueOf(map.getColorInt(5, 0))
        assertEquals(listOf(1f, 1f, 1f, 1f), color.components.toList())

        // Then: The color-representations of each distance consists of an alpha channel of 1 and rgb
        // channels of the ratio of the distance to the maximum distance,
        val maxDistance = pixels.max()
        assertNumbersEqual(
            expected = pixels.map{ x -> (0..2).map{x.toFloat() / maxDistance} + listOf(1f) }.flatten(),
            actual = pixels.indices.map{ Color.valueOf(map.getColorInt(it, 0)).components.toList() }.flatten(),
            tol = 1f / 255f
        )
    }


    @Test
    fun `to and from tiff round trip`() {
        // Given: A short-type map with incrementing distances.
        val map = ShortMap(buffer = ByteBuffer.allocate(10_000), nx = 10)
        val pixels = (0..200).toList()
        for(pixel in pixels) map.addDistance(pixel)

        // When: The map is converted to a TIFF directory.
        val mapId = "abcdef"
        val dir = map.toTiffDirectory(mapId)

        // Then: The TIFF pixel values match the distances.
        assertNumbersEqual(
            expected = (0 until 10).toList(),
            actual = (0 until 10).map{ dir.writeRasters.getPixelSample(0, it, 0) }
        )

        // When: The TIFF is written ...
        val file = File.createTempFile("image", "tiff")
        TiffWriter.writeTiff(file, TIFFImage().also { it.add(dir) })
        // ... and then read back.
        val imageRead  = TiffReader.readTiff(file)
        val dirRead = findTiff(imageRead.fileDirectories, mapId)

        // Then: The directory read back is not null.
        assertNotNull(dirRead)

        // Then: The read-back distances match those written.
        map.fromTiffDirectory(dirRead!!)
        assertNumbersEqual(
            expected = pixels,
            actual = pixels.indices.map{map.get(it, 0).toInt() - Short.MIN_VALUE.toInt()}.toList()
        )
    }

}
