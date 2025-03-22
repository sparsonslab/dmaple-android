package com.scepticalphysiologist.dmaple.geom

import android.graphics.RectF
import android.view.MotionEvent
import android.view.View
import kotlin.math.atan
import kotlin.math.cos
import kotlin.math.pow
import kotlin.math.sin

/**
 * An point on the Cartesian plain.
 *
 * Nice operator overloading, unlike [android.graphics.PointF].
 *
 * @property x The x coordinate.
 * @property y The y coordinate.
 */
class Point(var x: Float = 0f, var y: Float = 0f) {

    override fun toString(): String { return "x = $x, y = $y" }

    operator fun plus(other: Point): Point { return Point(x + other.x, y + other.y) }

    operator fun minus(other: Point): Point { return Point(x - other.x, y - other.y) }

    operator fun times(other: Point): Point { return Point(x * other.x, y * other.y) }

    operator fun div(other: Point): Point { return Point(x / other.x, y / other.y) }

    operator fun plus(other: Float): Point { return Point(x + other, y + other) }

    operator fun minus(other: Float): Point { return Point(x - other, y - other) }

    operator fun times(other: Float): Point { return Point(x * other, y * other) }

    operator fun div(other: Float): Point { return Point(x / other, y / other) }

    fun abs(): Point { return Point(kotlin.math.abs(x), kotlin.math.abs(y)) }

    fun swap(): Point { return Point(y, x) }

    fun min(): Float { return kotlin.math.min(x, y) }

    /** The distance from the origin.*/
    fun l2(): Float { return (x.pow(2) + y.pow(2)).pow(0.5f) }

    /** The angle to the origin. */
    fun theta(): Float { return atan(y / x) }

    /** Rotate the point about the origin.
     *
     * @param theta Rotation angle (radians).
     * @return The rotated point.
     */
    fun rotate(theta: Float): Point {
        return Point(x * cos(theta) + y * sin(theta), -x * sin(theta) + y * cos(theta))
    }

    fun toRectangle(): Rectangle { return Rectangle(c0 = Point(0f, 0f), c1 = this.copy()) }

    fun copy(): Point { return Point(x, y) }

    /** Functions that operate on collections of points. */
    companion object {

        fun minOf(p0: Point, p1: Point): Point { return Point(minOf(p0.x, p1.x), minOf(p0.y, p1.y)) }

        fun maxOf(p0: Point, p1: Point): Point { return Point(maxOf(p0.x, p1.x), maxOf(p0.y, p1.y)) }

        /** The point with unit length from the origin for a given angle. */
        fun unitLength(theta: Float): Point { return Point(cos(theta), sin(theta)) }

        /** Flatten a list of Points to a float array of (x, y, x, y, ...) pairs.
         *
         * Passed to methods of [android.graphics.Canvas] for drawing points, lines, etc.
         * */
        fun toFloatArray(ps: List<Point>): FloatArray {
            return ps.map{ listOf(it.x, it.y) }.flatten().toFloatArray()
        }

        /** The two opposing corners of a rectangle.*/
        fun fromRect(r: RectF): List<Point> {
            return listOf(Point(r.left, r.top), Point(r.right, r.bottom))
        }

        fun toRect(ps: List<Point>, i: Int = 0): RectF? {
            if(ps.size - i < 2) return null
            return RectF(
                minOf(ps[i].x, ps[i + 1].x),
                minOf(ps[i].y, ps[i + 1].y),
                maxOf(ps[i].x, ps[i + 1].x),
                maxOf(ps[i].y, ps[i + 1].y)
            )
        }

        fun ofViewExtent(view: View): Point {
            return Point(view.width.toFloat(), view.height.toFloat())
        }

        fun ofMotionEvent(event: MotionEvent): Point { return Point(event.x, event.y) }

    }

}
