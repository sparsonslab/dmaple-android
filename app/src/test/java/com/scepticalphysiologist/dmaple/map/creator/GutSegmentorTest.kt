package com.scepticalphysiologist.dmaple.map.creator

import android.graphics.Color
import com.scepticalphysiologist.dmaple.etc.rotateBitmap
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotEquals
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config
import org.robolectric.shadows.ShadowBitmap



@RunWith(RobolectricTestRunner::class)
@Config(shadows = [ShadowBitmap::class])
class GutSegmentorTest {

    @Test
    fun `correct bounds are calculated`() {
        // Given: A horizontal gut of constant width.
        val iw = 200
        val extent = Pair(40, 180)
        val ih = 100
        val bounds = Pair(35, 80)
        var image = createBitmap(iw, ih, Color.BLACK)
        for(i in extent.first..extent.second)
            for(j in bounds.first..bounds.second)
                image.setPixel(i, j, Color.WHITE)

        // When: The gut is segmented.
        val segmentor = GutSegmentor()
        segmentor.setLongSection(extent.first, extent.second)
        segmentor.gutIsHorizontal = true
        segmentor.threshold = 100f
        segmentor.gutIsAboveThreshold = true
        GutSegmentor.minWidth = 5
        segmentor.setFieldImage(image)
        segmentor.detectGutAndSeedSpine(Pair(1, ih - 1))

        // Then: The lower and upper bounds are where expected.
        assertEquals(bounds.first, segmentor.lower[20])
        assertEquals(bounds.second, segmentor.upper[20])

        // Given: The same gut, rotated.
        image = rotateBitmap(image, 90)
        segmentor.gutIsHorizontal = false

        // When: The gut is segmented.
        segmentor.setFieldImage(image)
        segmentor.detectGutAndSeedSpine(Pair(1, ih - 1))

        // Then: The lower and upper bounds are where expected.
        assertEquals(bounds.first, segmentor.lower[20])
        assertEquals(bounds.second, segmentor.upper[20])

        // Given The field values are inverted.
        invertBitmap(image)
        segmentor.gutIsAboveThreshold = false

        // When: The gut is segmented.
        segmentor.setFieldImage(image)
        segmentor.detectGutAndSeedSpine(Pair(1, ih - 1))

        // Then: The lower and upper bounds are where expected.
        assertEquals(bounds.first, segmentor.lower[20])
        assertEquals(bounds.second, segmentor.upper[20])
    }

    @Test
    fun `correct diameters are calculated`() {
        // Given: A horizontal gut of incrementing widths.
        val gutExtent = Pair(20, 200)
        val gutWidth = Pair(5, 60)
        val wS = (gutWidth.second - gutWidth.first).toFloat() / (gutExtent.second - gutExtent.first).toFloat()
        val w0 = gutWidth.first - wS * gutExtent.first
        val expectedWidths = (gutExtent.first..gutExtent.second).map{(it * wS + w0).toInt()}.toList()
        val iw = maxOf(gutExtent.first, gutExtent.second) + 20
        val ih = expectedWidths.max() + 50
        var image = createBitmap(iw, ih, Color.BLACK)
        for((i, w) in expectedWidths.withIndex())
            paintSlice(image, i + gutExtent.first, ih / 2, w, Color.WHITE, false)

        // When: The gut is segmented.
        val segmentor = GutSegmentor()
        segmentor.setLongSection(gutExtent.first, gutExtent.second)
        segmentor.gutIsHorizontal = true
        segmentor.threshold = 100f
        segmentor.gutIsAboveThreshold = true
        GutSegmentor.minWidth = 5
        segmentor.setFieldImage(image)
        segmentor.detectGutAndSeedSpine(Pair(1, ih - 1))

        // Then: The measured widths match the expected.
        var actualWidths = expectedWidths.indices.map{segmentor.getDiameter(it)}.toList()
        assertEquals(expectedWidths, actualWidths)

        // Given: The same gut, rotated.
        image = rotateBitmap(image, 90)
        segmentor.gutIsHorizontal = false

        // When: The gut is segmented.
        segmentor.setFieldImage(image)
        segmentor.detectGutAndSeedSpine(Pair(1, ih - 1))

        // Then: The measured widths match the expected.
        actualWidths = expectedWidths.indices.map{segmentor.getDiameter(it)}.toList()
        assertEquals(expectedWidths, actualWidths)

        // Given: The field values are inverted.
        invertBitmap(image)
        segmentor.gutIsAboveThreshold = false

        // When: The gut is segmented.
        segmentor.setFieldImage(image)
        segmentor.detectGutAndSeedSpine(Pair(1, ih - 1))

        // Then: The measured widths match the expected.
        actualWidths = expectedWidths.indices.map{segmentor.getDiameter(it)}.toList()
        assertEquals(expectedWidths, actualWidths)
    }

    @Test
    fun `behaviour at gut termination`() {
        // Given: A horizontal gut of constant width.
        val gutExtent = Pair(20, 100)
        val gutWidth = 40
        val expectedWidths = (gutExtent.first..gutExtent.second).map{gutWidth}.toList()
        val iw = maxOf(gutExtent.first, gutExtent.second) + 100
        val ih = 90
        val image = createBitmap(iw, ih, Color.BLACK)
        for((i, w) in expectedWidths.withIndex())
            paintSlice(image, i + gutExtent.first, ih / 2, w, Color.WHITE, false)

        // When: The gut is segmented 20 pixels past the gut's end.
        val segmentor = GutSegmentor()
        segmentor.setLongSection(gutExtent.first, gutExtent.second + 20)
        segmentor.gutIsHorizontal = true
        segmentor.threshold = 100f
        segmentor.gutIsAboveThreshold = true
        GutSegmentor.maxGap = 5
        GutSegmentor.minWidth = 5
        GutSegmentor.spineSmoothWin = 10
        segmentor.setFieldImage(image)
        segmentor.detectGutAndSeedSpine(Pair(1, ih - 1))

        // Then: The measured diameter past the end of the gut is 1.
        val diam = segmentor.getDiameter(segmentor.longIdx.size - 1)
        assertEquals(1, diam)
    }

    @Test
    fun `gaps are handled`() {
        // Given: A horizontal gut of constant width.
        val gutExtent = Pair(20, 200)
        val gutWidth = 40
        val expectedWidths = (gutExtent.first..gutExtent.second).map{gutWidth}.toList()
        val iw = maxOf(gutExtent.first, gutExtent.second) + 20
        val ih = 90
        val image = createBitmap(iw, ih, Color.BLACK)
        for((i, w) in expectedWidths.withIndex())
            paintSlice(image, i + gutExtent.first, ih / 2, w, Color.WHITE, false)
        // ... with a series of gaps.
        val gap = 4
        val hg = 1 + gap / 2
        val j = (ih - gutWidth) / 2
        for(i in hg until gutWidth - hg)
            paintSlice(image, i + gutExtent.first + 10, j + i, gap, Color.BLACK, false)

        // When: The gut is segmented with a maxGap attribute the same as the gap width.
        val segmentor = GutSegmentor()
        segmentor.setLongSection(gutExtent.first, gutExtent.second)
        segmentor.gutIsHorizontal = true
        segmentor.threshold = 100f
        segmentor.gutIsAboveThreshold = true
        GutSegmentor.maxGap = gap
        GutSegmentor.minWidth = 5
        GutSegmentor.spineSmoothWin = 10
        segmentor.setFieldImage(image)
        segmentor.detectGutAndSeedSpine(Pair(1, ih - 1))

        // Then: The measured widths match the expected.
        var actualWidths = expectedWidths.indices.map{segmentor.getDiameter(it)}.toList()
        assertEquals(expectedWidths, actualWidths)

        // When: The gut is segmented with a maxGap attribute less than the gap width
        GutSegmentor.maxGap = gap - 1
        segmentor.updateBoundaries()

        // Then: The measured widths do not match the expected.
        actualWidths = expectedWidths.indices.map{segmentor.getDiameter(it)}.toList()
        assertNotEquals(expectedWidths, actualWidths)
    }

}
