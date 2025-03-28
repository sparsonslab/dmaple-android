package com.scepticalphysiologist.dmaple.geom

import org.junit.Assert.assertEquals
import org.junit.Test
import org.junit.jupiter.params.ParameterizedTest
import org.junit.jupiter.params.provider.Arguments
import org.junit.jupiter.params.provider.MethodSource
import java.util.stream.Stream

class EdgeTest {

    companion object {
        @JvmStatic
        fun SetsOfRotations() = Stream.of(
            Arguments.of(Edge.LEFT, 90, Edge.BOTTOM),
            Arguments.of(Edge.RIGHT, 90, Edge.TOP),
            Arguments.of(Edge.LEFT, 270, Edge.TOP),
            Arguments.of(Edge.BOTTOM, -90, Edge.LEFT),
            Arguments.of(Edge.LEFT, -180, Edge.RIGHT),
            Arguments.of(Edge.TOP, 270, Edge.RIGHT),
            Arguments.of(Edge.LEFT, -90, Edge.TOP),
            Arguments.of(Edge.BOTTOM, 180, Edge.TOP),
            Arguments.of(Edge.LEFT, 0, Edge.LEFT),
            Arguments.of(Edge.TOP, 0, Edge.TOP),
            Arguments.of(Edge.TOP, -270, Edge.LEFT)
        )

        @JvmStatic
        fun Verticality() = Stream.of(
            Arguments.of(Edge.LEFT, true),
            Arguments.of(Edge.RIGHT, true),
            Arguments.of(Edge.TOP, false),
            Arguments.of(Edge.BOTTOM, false),
        )
    }

    @ParameterizedTest(name = "case #{index} ==> {0} rotated by {1} gives {2}")
    @MethodSource("SetsOfRotations")
    fun `rotates correctly`(edge: Edge, rotation: Int, expectedEdge: Edge) {
        // Given: An "old edge".
        // When: That edge is rotated.
        // Then: It is the expected edge.
        assertEquals(expectedEdge, edge.rotate(rotation))
    }

    @ParameterizedTest(name = "case #{index} ==> {0} is vertical =  {1}}")
    @MethodSource("Verticality")
    fun `correct verticality`(edge:Edge, expectedVertical: Boolean) {
        // Given: An edge.
        // Then: The edge's verticality is as expected.
        assertEquals(expectedVertical, edge.isVertical())
    }
}
