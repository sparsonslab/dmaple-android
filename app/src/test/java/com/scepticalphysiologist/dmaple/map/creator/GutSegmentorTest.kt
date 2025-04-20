// Copyright (c) 2025-2025. Dr Sean Paul Parsons. All rights reserved.

package com.scepticalphysiologist.dmaple.map.creator

import android.graphics.Color
import com.scepticalphysiologist.dmaple.assertNumbersEqual
import com.scepticalphysiologist.dmaple.geom.Edge
import com.scepticalphysiologist.dmaple.geom.Frame
import com.scepticalphysiologist.dmaple.geom.Point
import com.scepticalphysiologist.dmaple.map.field.FieldImage
import com.scepticalphysiologist.dmaple.map.field.FieldRoi
import org.junit.Test
import org.junit.jupiter.api.BeforeEach
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config
import org.robolectric.shadows.ShadowBitmap
import kotlin.random.Random

@RunWith(RobolectricTestRunner::class)
@Config(shadows = [ShadowBitmap::class])
class GutSegmentorTest {


    lateinit var image: FieldImage

    lateinit var bounds: MutableList<Pair<Int, Int>>

    lateinit var roi: FieldRoi

    lateinit var params: FieldParams

    lateinit var expectedLowerBounds: List<Int>

    lateinit var expectedUpperBounds: List<Int>

    lateinit var expectedDiameters: List<Int>

    init { setUp() }

    @BeforeEach
    fun setUp() {
        // Field with a horizontal gut of increasing diameter.
        val fieldWidth = 200
        val extent = Pair(30, 180)
        val diameter = Pair(10, 50)
        val bitmap = createBitmap(fieldWidth, 50 + maxOf(diameter.first, diameter.second), Color.BLACK)
        val m = (diameter.second - diameter.first).toFloat() / (extent.second - extent.first).toFloat()
        val c = diameter.first - m * extent.first
        bounds = mutableListOf<Pair<Int, Int>>()
        for(i in extent.first..extent.second) {
            val l = 20
            val u = l + (i * m + c).toInt()
            bounds.add(Pair(l, u))
            for(j in l..u) bitmap.setPixel(i, j, Color.WHITE)
        }

        // An ROI within that field
        val frame = Frame(Point(bitmap.width.toFloat(), bitmap.height.toFloat()), 0)
        image = FieldImage(frame = frame, bitmap = bitmap)
        roi = FieldRoi(
            frame = frame,
            c0 = Point(extent.first.toFloat(), bounds[0].first.toFloat() - 4),
            c1 = Point(extent.second.toFloat(), bounds[0].second.toFloat() + 4),
            seedingEdge = Edge.LEFT,
            threshold = 100
        )

        // Mapping parameters
        params = FieldParams(
            gutsAreAboveThreshold = true,
            minWidth = 5,
            maxGap = 1,
            spineSkipPixels = 0,
            spineSmoothPixels = 1
        )

        // Expected bounds anf diameters.
        expectedLowerBounds = bounds.map { it.first }
        expectedUpperBounds = bounds.map { it.second }
        expectedDiameters = bounds.map{it.second - it.first + 1}
    }

    @Test
    fun `correct bounds and diameters calculated`() {
        // Given: The gut.

        // When: The gut is segmented.
        val segmentor = GutSegmentor(roi, params)
        segmentor.setFieldImage(LumaReader().also{it.readBitmap(image.bitmap)})
        segmentor.detectGutAndSeedSpine()

        // Then: The bounds and diameters are as expected.
        assertNumbersEqual(expectedLowerBounds, segmentor.lower.toList())
        assertNumbersEqual(expectedUpperBounds, segmentor.upper.toList())
        assertNumbersEqual(expectedDiameters, segmentor.longIdx.indices.map{segmentor.getDiameter(it)})
    }

    @Test
    fun `correct bounds and diameters are calculated after rotation`() {
        // Given: The same gut, rotated.
        val newFrame = Frame(roi.frame.size.swap(), roi.frame.orientation + 90)
        image.inNewFrame(newFrame)
        roi.inNewFrame(newFrame)

        // When: The gut is segmented.
        val segmentor = GutSegmentor(roi, params)
        segmentor.setFieldImage(LumaReader().also{it.readBitmap(image.bitmap)})
        segmentor.detectGutAndSeedSpine()

        // Then: The bounds and diameters are as expected.
        assertNumbersEqual(expectedLowerBounds, segmentor.lower.toList())
        assertNumbersEqual(expectedUpperBounds, segmentor.upper.toList())
        assertNumbersEqual(expectedDiameters, segmentor.longIdx.indices.map{segmentor.getDiameter(it)})
    }

    @Test
    fun `correct bounds and diameters are calculated after inversion`() {
        // Given The field values are inverted.
        invertBitmap(image.bitmap)
        params.gutsAreAboveThreshold = false

        // When: The gut is segmented.
        val segmentor = GutSegmentor(roi, params)
        segmentor.setFieldImage(LumaReader().also{it.readBitmap(image.bitmap)})
        segmentor.detectGutAndSeedSpine()

        // Then: The bounds and diameters are as expected.
        assertNumbersEqual(expectedLowerBounds, segmentor.lower.toList())
        assertNumbersEqual(expectedUpperBounds, segmentor.upper.toList())
        assertNumbersEqual(expectedDiameters, segmentor.longIdx.indices.map{segmentor.getDiameter(it)})
    }

    @Test
    fun `skip spine pixels`() {
        // Given: Pixels are skipped
        params.spineSkipPixels = 1

        // When: The gut is segmented.
        val segmentor = GutSegmentor(roi, params)
        segmentor.setFieldImage(LumaReader().also{it.readBitmap(image.bitmap)})
        segmentor.detectGutAndSeedSpine()

        // Then: The lower and upper bounds are where expected.
        assertNumbersEqual(expectedLowerBounds.filterIndexed{i, _ -> i % 2 == 0}, segmentor.lower.toList())
        assertNumbersEqual(expectedUpperBounds.filterIndexed{i, _ -> i % 2 == 0}, segmentor.upper.toList())
        assertNumbersEqual(expectedDiameters.filterIndexed{i, _ -> i % 2 == 0}, segmentor.longIdx.indices.map{segmentor.getDiameter(it)})
    }

    @Test
    fun `behaviour at gut termination`() {
        // Given: The ROI extends beyond the right side of the gut.
        roi.right += 5f

        // When: The gut is segmented.
        val segmentor = GutSegmentor(roi, params)
        segmentor.setFieldImage(LumaReader().also{it.readBitmap(image.bitmap)})
        segmentor.detectGutAndSeedSpine()

        // Then: The gut has a diameter of 1 at the right.
        assert(1 == segmentor.getDiameter(segmentor.longIdx.size - 1))
        assert(1 != segmentor.getDiameter(0))
    }

    @Test
    fun `gaps are handled`() {
        // Given: A segmentor.
        params.maxGap = 5
        val segmentor = GutSegmentor(roi, params)
        // ... and some gaps in the gut.
        val gapSize = params.maxGap
        for((k, i) in segmentor.longIdx.withIndex()) {
            if(expectedDiameters[k] < gapSize + 5) continue
            val gap = (Random.nextFloat() * gapSize).toInt()
            val g0= 2 + segmentor.lower[k]
            for(j in 0 until gap) image.bitmap.setPixel(i, g0 + j, Color.BLACK)
        }

        // When: The gut is segmented.
        segmentor.setFieldImage(LumaReader().also{it.readBitmap(image.bitmap)})
        segmentor.detectGutAndSeedSpine()

        // Then: The lower and upper bounds are where expected.
        assertNumbersEqual(expectedLowerBounds, segmentor.lower.toList())
        assertNumbersEqual(expectedUpperBounds, segmentor.upper.toList())
        assertNumbersEqual(expectedDiameters, segmentor.longIdx.indices.map{segmentor.getDiameter(it)})
    }

}
