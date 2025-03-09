package com.scepticalphysiologist.dmaple.map.creator

import org.junit.jupiter.api.Test
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotEquals
import kotlin.math.abs

class GutSegmentorTest {

    @Test
    fun `correct diameter calculated`() {
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

        // When: The field values are inverted.
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

fun createImage(w: Int, h: Int, background: Float): Array<FloatArray> {
    return Array(h) { FloatArray(w){ background } }
}

fun imageSize(image: Array<FloatArray>): Pair<Int, Int> {
    return Pair(image[0].size, image.size)
}

fun rotateImage(image: Array<FloatArray>): Array<FloatArray>{
    val (w, h) = imageSize(image)
    val rImage = createImage(h, w, 0f)
    for(i in 0 until w) for(j in 0 until h) rImage[i][j] = image[j][i]
    return rImage
}

fun applyImage(image: Array<FloatArray>, foo: (Float) -> Float){
    val (w, h) = imageSize(image)
    for(i in 0 until w) for(j in 0 until h) image[j][i] = foo(image[j][i])
}

fun paintSlice(image: Array<FloatArray>, i: Int, cent: Int, width: Int, value: Float, horizontal: Boolean) {
    val p = cent - width / 2
    val q = p + width - 1
    if(horizontal) for(j in p..q) image[i][j] = value
    else for(j in p..q) image[j][i] = value
}
