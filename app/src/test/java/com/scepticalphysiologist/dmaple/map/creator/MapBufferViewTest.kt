package com.scepticalphysiologist.dmaple.map.creator

import android.graphics.Color
import com.scepticalphysiologist.dmaple.assertNumbersEqual
import org.junit.Assert.assertEquals
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config
import org.robolectric.shadows.ShadowColor
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

        // Then: The maximum pixel value is shown as a color with maximum values (1f) for all channels.
        val color = Color.valueOf(map.getColorInt(5, 0))
        assertEquals(listOf(1f, 1f, 1f, 1f), color.components.toList())

        // Then: The color-representations of each pixel consists of an alpha channel of 1 and rgb
        // channels of the ratio of the pixel value to the maximum pixel value.
        val maxDistance = pixels.max()
        assertNumbersEqual(
            expected = pixels.map{ x -> (0..2).map{x.toFloat() / maxDistance} + listOf(1f) }.flatten(),
            actual = pixels.indices.map{ Color.valueOf(map.getColorInt(it, 0)).components.toList() }.flatten(),
            tol = 1f / 255f
        )
    }

}
