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
        fun TransformCases() = Stream.of(
            Arguments.of(
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
            ),
            Arguments.of(
                FieldRoi(
                    frame=Frame(size = frameSize, orientation = 0),
                    c0 = Point(10f, 10f), c1 = Point(60f, 60f),
                    seedingEdge = Edge.BOTTOM
                ),
                FieldRoi(
                    frame=Frame(size = frameSize, orientation = -180),
                    c0 = Point(40f, 40f), c1 = Point(90f, 90f),
                    seedingEdge = Edge.TOP
                ),
            ),
        )

    }

    @ParameterizedTest(name = "{index} ==> transform {0} to {1}")
    @MethodSource("TransformCases")
    fun `correct transform`(actualRoi: FieldRoi, expectedRoi: FieldRoi) {
        // Given: An ROI
        // [actualRoi]

        // When: It is transformed to another frame.
        actualRoi.changeFrame(expectedRoi.frame)

        // Then: The transformed ROI is as expected.
        assertEquals(expectedRoi.left, actualRoi.left, 1e-4f)
        assertEquals(expectedRoi.right, actualRoi.right, 1e-4f)
        assertEquals(expectedRoi.top, actualRoi.top, 1e-4f)
        assertEquals(expectedRoi.bottom, actualRoi.bottom, 1e-4f)
        assertEquals(expectedRoi.seedingEdge, actualRoi.seedingEdge)
    }

}
