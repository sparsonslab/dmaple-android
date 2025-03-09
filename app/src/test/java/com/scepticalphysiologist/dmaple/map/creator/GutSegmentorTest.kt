package com.scepticalphysiologist.dmaple.map.creator

import org.junit.jupiter.api.Test
import org.junit.Assert.assertEquals

class GutSegmentorTest {

    @Test
    fun `gaps detected`() {
        // Given: An image with a horizontal gut consisting of a band with
        // a 3 pixel gap in the middle.
        val image = createImage(100, 100, 0f)
        addBand(image, 30..80, 1f, true)
        addBand(image, 50..52, 0f, true)

        // Given: A gut segmentor.
        val segmentor = ArrayGutSegmentor()
        segmentor.setFieldImage(image)
        segmentor.gutIsHorizontal = true
        segmentor.threshold = 0.5f
        segmentor.gutIsAboveThreshold = true
        GutSegmentor.minWidth = 5
        GutSegmentor.smoothWinSize = 2

        // When: the max gap is less than the gap.
        GutSegmentor.maxGap = 0
        var guts = segmentor.findGuts(90, Pair(10, 90))

        // Then: Two guts are found
        assertEquals(listOf(Pair(30, 49), Pair(53, 80)), guts)

        // When: the max gap is more than the gap.
        GutSegmentor.maxGap = 4
        guts = segmentor.findGuts(90, Pair(10, 90))

        // The: One gut is found.
        println(guts)
        assertEquals(listOf(Pair(30, 80)), guts)
    }


    @Test
    fun `spine smoothed`() {

        // Given: An image with a horizontal gut consisting of a band with
        // a 3 pixel gap in the middle.
        val image = createImage(100, 100, 0f)
        addBand(image, 30..80, 1f, true)
        addBand(image, 50..52, 0f, true)

        // Given: A gut segmentor.
        val segmentor = ArrayGutSegmentor()
        segmentor.setLongSection(10, 90)
        segmentor.gutIsHorizontal = true
        segmentor.threshold = 0.5f
        segmentor.gutIsAboveThreshold = true
        GutSegmentor.minWidth = 5
        GutSegmentor.smoothWinSize = 10

        segmentor.setFieldImage(image)
        segmentor.detectGutAndSeedSpine(Pair(10, 90))
        for(i in segmentor.longIdx.indices) {
            println("${segmentor.spine[i]}\t${segmentor.spineSmoothed[i]}")
        }


    }

}

fun createImage(w: Int, h: Int, background: Float): Array<FloatArray> {
    return Array(h) { FloatArray(w){ background } }
}

fun imageSize(image: Array<FloatArray>): Pair<Int, Int> {
    return Pair(image[0].size, image.size)
}

fun addBand(image: Array<FloatArray>, range: IntRange, value: Float, horizontal: Boolean) {
    val (w, h) = imageSize(image)
    if(horizontal) for(i in 0 until w) for(j in range) image[j][i] = value
    else for(i in range) for(j in 0 until h) image[j][i] = value
}
