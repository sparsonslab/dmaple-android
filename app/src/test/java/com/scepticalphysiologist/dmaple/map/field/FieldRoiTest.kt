package com.scepticalphysiologist.dmaple.map.field

import com.scepticalphysiologist.dmaple.geom.Edge
import com.scepticalphysiologist.dmaple.geom.Frame
import com.scepticalphysiologist.dmaple.geom.Point
import org.junit.Assert.assertEquals
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
}
