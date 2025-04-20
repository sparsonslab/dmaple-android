// Copyright (c) 2025-2025. Dr Sean Paul Parsons. All rights reserved.

package com.scepticalphysiologist.dmaple.map.field

import com.scepticalphysiologist.dmaple.assertSerializedObjectsEqual
import com.scepticalphysiologist.dmaple.geom.Edge
import com.scepticalphysiologist.dmaple.geom.Frame
import com.scepticalphysiologist.dmaple.geom.Point
import com.scepticalphysiologist.dmaple.map.creator.MapType
import org.junit.Assert.assertEquals
import org.junit.Test
import org.junit.jupiter.params.ParameterizedTest
import org.junit.jupiter.params.provider.Arguments
import org.junit.jupiter.params.provider.MethodSource
import java.util.stream.Stream


class FieldRoiTest {

    private companion object {

        val frameSize = Point(100f, 100f)

        @JvmStatic
        fun SetsOfEquivalentRoisInDifferentFrames() = Stream.of(
            Arguments.of(listOf(
                FieldRoi(
                    frame=Frame(size = frameSize, orientation = 0),
                    c0 = Point(10f, 10f), c1 = Point(60f, 60f),
                    seedingEdge = Edge.RIGHT
                ),
                FieldRoi(
                    frame=Frame(size = frameSize, orientation = 90),
                    c0 = Point(10f, 40f), c1 = Point(60f, 90f),
                    seedingEdge = Edge.TOP
                ),
                FieldRoi(
                    frame=Frame(size = frameSize, orientation = -90),
                    c0 = Point(40f, 10f), c1 = Point(90f, 60f),
                    seedingEdge = Edge.BOTTOM
                ),
                FieldRoi(
                    frame=Frame(size = frameSize, orientation = -180),
                    c0 = Point(40f, 40f), c1 = Point(90f, 90f),
                    seedingEdge = Edge.LEFT
                ),
            )),
            Arguments.of(listOf(
                FieldRoi(
                    frame=Frame(size = frameSize, orientation = 0),
                    c0 = Point(30f, 30f), c1 = Point(90f, 40f),
                    seedingEdge = Edge.BOTTOM
                ),
                FieldRoi(
                    frame=Frame(size = frameSize, orientation = -180),
                    c0 = Point(10f, 60f), c1 = Point(70f, 70f),
                    seedingEdge = Edge.TOP
                ),
                FieldRoi(
                    frame=Frame(size = frameSize, orientation = -270),
                    c0 = Point(30f, 10f), c1 = Point(40f, 70f),
                    seedingEdge = Edge.RIGHT
                ),
            )),
        )
    }

    @ParameterizedTest(name = "{index} ==> ")
    @MethodSource("SetsOfEquivalentRoisInDifferentFrames")
    fun `transforms are correct`(rois: List<FieldRoi>) {
        // Given: A list of ROIs that are equivalent but in different frames.
        for(i in 1 until rois.size) {

            // When: Each ROI is transformed to the frame of the first ROI.
            rois[i].changeFrame(rois[0].frame)

            // Then: The transformed ROI is equal to the first ROI.
            assertEquals(rois[0].left,        rois[i].left, 1e-4f)
            assertEquals(rois[0].right,       rois[i].right, 1e-4f)
            assertEquals(rois[0].top,         rois[i].top, 1e-4f)
            assertEquals(rois[0].bottom,      rois[i].bottom, 1e-4f)
            assertEquals(rois[0].seedingEdge, rois[i].seedingEdge)
        }
    }

    @Test
    fun `crops to frame`() {
        // Given: An ROI which extend beyond its frame.
        val roi = FieldRoi(
            frame=Frame(size = Point(100f, 100f), orientation = 0),
            c0 = Point(50f, 50f), c1 = Point(150f, 150f),
            seedingEdge = Edge.TOP
        )

        // When: The ROI is cropped to its frame.
        roi.cropToFrame()

        // Then: The ROI extends only to its frame.
        assertEquals(roi.c0.x, 50f)
        assertEquals(roi.c0.y, 50f)
        assertEquals(roi.c1.x, 100f)
        assertEquals(roi.c1.y, 100f)
    }

    @Test
    fun `copies`() {
        // Given: An ROI.
        val roi = FieldRoi(
            frame=Frame(size = Point(100f, 100f), orientation = 0),
            c0 = Point(50f, 50f), c1 = Point(67f, 32f),
            seedingEdge = Edge.TOP,
            threshold = 47,
            maps = listOf(MapType.DIAMETER, MapType.RADIUS),
            uid = "someinterestingmap"
        )

        // When: It is copied.
        val copied = roi.copy()

        // Then: The original and copied objects are equivalent (use serialisation to assert this).
        assertSerializedObjectsEqual(roi, copied)
    }

}
