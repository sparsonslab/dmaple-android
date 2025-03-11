package com.scepticalphysiologist.dmaple.map.creator

import org.junit.jupiter.api.Test
import org.junit.jupiter.api.Assertions.*
import kotlin.math.abs

class GutSegmentorTest {

    @Test
    fun `correct bounds are calculated`() {
        // Given: A horizontal gut of constant width.
        val iw = 200
        val extent = Pair(40, 180)
        val ih = 100
        val bounds = Pair(35, 80)
        var image = createImage(iw, ih, 0f)
        for(i in extent.first..extent.second)
            for(j in bounds.first..bounds.second)
                image[j][i] = 1f

        // When: The gut is segmented.
        val segmentor = ArrayGutSegmentor()
        segmentor.setLongSection(extent.first, extent.second)
        segmentor.gutIsHorizontal = true
        segmentor.threshold = 0.5f
        segmentor.gutIsAboveThreshold = true
        GutSegmentor.minWidth = 5
        segmentor.setFieldImage(image)
        segmentor.detectGutAndSeedSpine(Pair(1, ih - 1))

        // Then: The lower and upper bounds are where expected.
        assertEquals(bounds.first, segmentor.lower[20])
        assertEquals(bounds.second, segmentor.upper[20])

        // Given: The same gut, rotated.
        image = rotateImage(image)
        segmentor.gutIsHorizontal = false

        // When: The gut is segmented.
        segmentor.setFieldImage(image)
        segmentor.detectGutAndSeedSpine(Pair(1, ih - 1))

        // Then: The lower and upper bounds are where expected.
        assertEquals(bounds.first, segmentor.lower[20])
        assertEquals(bounds.second, segmentor.upper[20])

        // Given The field values are inverted.
        applyImage(image, {v -> abs(v - 1f)})
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
        var image = createImage(iw, ih, 0f)
        for((i, w) in expectedWidths.withIndex())
            paintSlice(image, i + gutExtent.first, ih / 2, w, 1f, false)

        // When: The gut is segmented.
        val segmentor = ArrayGutSegmentor()
        segmentor.setLongSection(gutExtent.first, gutExtent.second)
        segmentor.gutIsHorizontal = true
        segmentor.threshold = 0.5f
        segmentor.gutIsAboveThreshold = true
        GutSegmentor.minWidth = 5
        segmentor.setFieldImage(image)
        segmentor.detectGutAndSeedSpine(Pair(1, ih - 1))

        // Then: The measured widths match the expected.
        var actualWidths = expectedWidths.indices.map{segmentor.getDiameter(it)}.toList()
        assertEquals(expectedWidths, actualWidths)

        // Given: The same gut, rotated.
        image = rotateImage(image)
        segmentor.gutIsHorizontal = false

        // When: The gut is segmented.
        segmentor.setFieldImage(image)
        segmentor.detectGutAndSeedSpine(Pair(1, ih - 1))

        // Then: The measured widths match the expected.
        actualWidths = expectedWidths.indices.map{segmentor.getDiameter(it)}.toList()
        assertEquals(expectedWidths, actualWidths)

        // Given: The field values are inverted.
        applyImage(image, {v -> abs(v - 1f)})
        segmentor.gutIsAboveThreshold = false

        // When: The gut is segmented.
        segmentor.setFieldImage(image)
        segmentor.detectGutAndSeedSpine(Pair(1, ih - 1))

        // Then: The measured widths match the expected.
        actualWidths = expectedWidths.indices.map{segmentor.getDiameter(it)}.toList()
        assertEquals(expectedWidths, actualWidths)
    }

    @Test
    fun `gaps are handled`() {
        // Given: A horizontal gut of constant width.
        val gutExtent = Pair(20, 200)
        val gutWidth = 40
        val expectedWidths = (gutExtent.first..gutExtent.second).map{gutWidth}.toList()
        val iw = maxOf(gutExtent.first, gutExtent.second) + 20
        val ih = 90
        val image = createImage(iw, ih, 0f)
        for((i, w) in expectedWidths.withIndex())
            paintSlice(image, i + gutExtent.first, ih / 2, w, 1f, false)
        // ... with a series of gaps.
        val gap = 4
        val hg = 1 + gap / 2
        val j = (ih - gutWidth) / 2
        for(i in hg until gutWidth - hg)
            paintSlice(image, i + gutExtent.first + 10, j + i, gap,0f, false)

        // When: The gut is segmented with a maxGap attribute the same as the gap width.
        val segmentor = ArrayGutSegmentor()
        segmentor.setLongSection(gutExtent.first, gutExtent.second)
        segmentor.gutIsHorizontal = true
        segmentor.threshold = 0.5f
        segmentor.gutIsAboveThreshold = true
        GutSegmentor.maxGap = gap
        GutSegmentor.minWidth = 5
        GutSegmentor.smoothWinSize = 10
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
