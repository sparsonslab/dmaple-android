package com.scepticalphysiologist.dmaple

import com.google.gson.Gson
import com.scepticalphysiologist.dmaple.geom.Point
import kotlin.math.absoluteValue

/** Assert that two lists of numbers are equal to within tolerance.
 *
 * @param expected The expected numbers.
 * @param actual The actual numbers.
 * @param tol The tolerance.
 */
fun assertNumbersEqual(expected: List<Number>, actual: List<Number>, tol: Number = 0) {
    if (expected.size != actual.size) throw AssertionError(
        "expected size:<${expected.size}> but was:<${actual.size}>"
    )
    for (i in expected.indices)
        if ((expected[i].toDouble() - actual[i].toDouble()).absoluteValue > tol.toDouble())
            throw AssertionError(
                "expected:<$expected> but was:<$actual>"
            )
}


/** Assert that a list of numbers are equal to an expected scalar to within tolerance.
 *
 * @param expected
 * @param actual
 * @param tol
 */
fun assertAllNumbersEqual(expected: Number, actual: List<Number>, tol: Number = 0) {
    for (i in actual.indices)
        if ((expected.toDouble() - actual[i].toDouble()).absoluteValue > tol.toDouble())
            throw AssertionError(
                "expected:<$expected> but was:<$actual>"
            )
}

/** Assert that a string contains other substrings.
 *
 * @param actual The string.
 * @param expected The substrings expected to be in [actual].
 */
fun assertStringContains(actual: String, expected: List<String>) {
    for(substring in expected) if(substring !in actual) throw AssertionError(
        "<$substring> is not in <$actual>"
    )
}


/** Assert that two points are equivalent to within a distance of [delta]. */
fun assertPointsEqual(expected: Point, actual: Point, delta: Float = 1e-5f) {
    if(((expected.x - actual.x).absoluteValue > delta) || ((expected.y - actual.y).absoluteValue > delta))
        throw AssertionError("$expected is not equal to $actual.")
}

fun assertSerializedObjectsEqual(obj1: Any?, obj2: Any?, serializer: Gson = Gson()) {
    val ser1 = serializer.toJson(obj1)
    val ser2 = serializer.toJson(obj2)
    if(ser1 != ser2) throw AssertionError("$ser1 is not equal to $ser2")
}
