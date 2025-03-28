package com.scepticalphysiologist.dmaple.geom

import com.scepticalphysiologist.dmaple.assertPointsEqual
import org.junit.Assert.assertEquals
import org.junit.jupiter.params.ParameterizedTest
import org.junit.jupiter.params.provider.Arguments
import org.junit.jupiter.params.provider.MethodSource
import java.util.stream.Stream
import kotlin.math.PI

class PointTest {

    companion object {
        @JvmStatic
        fun pointPointOperations() = Stream.of(
            Arguments.of(Point(3f, 5f), "*", Point(7f, -6f), Point(21f, -30f)),
            Arguments.of(Point(3f, 5f), "+", Point(3.6f, -5.6f), Point(6.6f, -0.6f)),
            Arguments.of(Point(3f, 5f), "-", Point(7f, -6f), Point(-4f, 11f)),
            Arguments.of(Point(3f, 8.8f), "/", Point(-2f, 4f), Point(-1.5f, 2.2f)),
            Arguments.of(Point(3f, 5f), "+", Point(-2.5f, 0.00003f), Point(0.5f, 5.00003f)),
            Arguments.of(Point(3f, 5f), "*", Point(0.5f, -0.01f), Point(1.5f, -0.05f)),
            Arguments.of(Point(3f, 5f), "-", Point(-0.0022f, -6.089f), Point(3.0022f, 11.089f)),
            Arguments.of(Point(3f, 5f), "/", Point(6f, -0.1f), Point(0.5f, -50f)),
        )

        @JvmStatic
        fun pointFloatOperations() = Stream.of(
            Arguments.of(Point(3f, 5f), "*", 5f, Point(15f, 25f)),
            Arguments.of(Point(3f, 16.8f), "/", -4f, Point(-0.75f, -4.2f)),
            Arguments.of(Point(3f, 5f), "+", 0.00098f, Point(3.00098f, 5.00098f)),
            Arguments.of(Point(3f, -5f), "-", 0.002f, Point(2.998f, -5.002f)),
            Arguments.of(Point(3f, 5f), "*",-0.1f, Point(-0.3f, -0.5f)),
            Arguments.of(Point(3f, -5f), "/", -0.01f, Point(-300f, 500f)),
            Arguments.of(Point(3f, 5f), "+", -7.8f, Point(-4.8f, -2.8f)),
            Arguments.of(Point(3f, 5f), "-", 1.2f, Point(1.8f, 3.8f)),
        )

        @JvmStatic
        fun pointToPointOperations() = Stream.of(
            Arguments.of(Point(3f, -5f), "abs", Point(3f, 5f)),
            Arguments.of(Point(3.087f, -5.874f), "ceil", Point(4f, -5f)),
            Arguments.of(Point(-2.67f, 0.07f), "ceil", Point(-2f, 1f)),
            Arguments.of(Point(3.787f, -5.98f), "swap", Point(-5.98f, 3.787f)),
            Arguments.of(Point(3f, -5f), "copy", Point(3f, -5f)),
        )

        @JvmStatic
        fun pointToFloatOperations() = Stream.of(
            Arguments.of(Point(0f, 5f), "theta", 0.5f * PI.toFloat()),
            Arguments.of(Point(0f, -5f), "theta", -0.5f * PI.toFloat()),
            Arguments.of(Point(-5f, 0f), "theta", -0f),
            Arguments.of(Point(5f, 0f), "theta", 0f),
            Arguments.of(Point(5f, 5f), "theta", 0.25f * PI.toFloat()),
            Arguments.of(Point(-5f, -5f), "theta", 0.25f * PI.toFloat()),
            Arguments.of(Point(-5f, 5f), "theta", -0.25f * PI.toFloat()),
            Arguments.of(Point(5f, -5f), "theta", -0.25f * PI.toFloat()),
            Arguments.of(Point(0f, 5f), "l2", 5f),
            Arguments.of(Point(-5f, -0f), "l2", 5f),
            Arguments.of(Point(2f, -2f), "l2", 2.828427f),
            Arguments.of(Point(-2f, 2f), "l2", 2.828427f),
            Arguments.of(Point(0.6f, 5f), "min", 0.6f),
            Arguments.of(Point(-8.9f, 5f), "min", -8.9f),
            Arguments.of(Point(-0.00005f, -0.00003f), "min", -0.00005f),
        )
    }

    @ParameterizedTest(name = "case #{index} ==> {0} {1} {2} = {3}")
    @MethodSource("pointPointOperations")
    fun `point point operators work`(a: Point, operator: String, b: Point, x: Point) {
        // Given: Two points, a and b.
        // When: They are combined with a mathematical operator.
        // Then: The output point of the operation is as expected.
        when(operator) {
            "*" -> assertPointsEqual(x, a * b)
            "/" -> assertPointsEqual(x, a / b)
            "+" -> assertPointsEqual(x, a + b)
            "-" -> assertPointsEqual(x, a - b)
        }
    }

    @ParameterizedTest(name = "case #{index} ==> {0} {1} {2} = {3}")
    @MethodSource("pointFloatOperations")
    fun `point float operators work`(a: Point, operator: String, b: Float, x: Point) {
        // Given: A point (a) and a float (b).
        // When: They are combined with a mathematical operator.
        // Then: The output point of the operation is as expected.
        when(operator) {
            "*" -> assertPointsEqual(x, a * b)
            "/" -> assertPointsEqual(x, a / b)
            "+" -> assertPointsEqual(x, a + b)
            "-" -> assertPointsEqual(x, a - b)
        }
    }

    @ParameterizedTest(name = "case #{index} ==> {1} of {0} = {2}")
    @MethodSource("pointToPointOperations")
    fun `point to point operators work`(a: Point, operator: String, x: Point) {
        // Given: A point (a).
        // When: The operator is applied.
        // Then: The output point of the operation is as expected.
        when(operator) {
            "abs" -> assertPointsEqual(x, a.abs())
            "ceil" -> assertPointsEqual(x, a.ceil())
            "swap" -> assertPointsEqual(x, a.swap())
            "copy" -> assertPointsEqual(x, a.copy())
        }
    }

    @ParameterizedTest(name = "case #{index} ==> {1} of {0} = {2}")
    @MethodSource("pointToFloatOperations")
    fun `point to float operators work`(a: Point, operator: String, x: Float) {
        // Given: A point (a).
        // When: The operator is applied.
        // Then: The output point of the operation is as expected.
        when(operator) {
            "min" -> assertEquals(x, a.min(), 1e-5f)
            "theta" -> assertEquals(x, a.theta(), 1e-5f)
            "l2" -> assertEquals(x, a.l2(), 1e-5f)
        }
    }

}
