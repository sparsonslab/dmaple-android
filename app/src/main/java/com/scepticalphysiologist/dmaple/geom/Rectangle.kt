package com.scepticalphysiologist.dmaple.geom

import android.graphics.Rect
import android.graphics.RectF
import kotlin.math.abs

/** A rectangle.
 *
 * Unlike [android.graphics.RectF] this can be unit tested!
 *
 * @property c0
 * @property c1
 */
open class Rectangle (
    /** The corner of the rectangle opposite to [c1]. */
    var c0: Point = Point(),
    /** The corner of the rectangle opposite to [c0]. */
    var c1: Point = Point()
){

    // ---------------------------------------------------------------------------------------------
    // android.graphics.RectF equivalent attributes
    // ---------------------------------------------------------------------------------------------

    var left: Float
        get() = minOf(c0.x, c1.x)
        set(value) { if(c0.x < c1.x) c0.x = value else c1.x = value }
    /** The maximum x coordinate. */
    var right: Float
        get() = maxOf(c0.x, c1.x)
        set(value) { if(c0.x > c1.x) c0.x = value else c1.x = value }
    /** The minimum y coordinate. */
    var top: Float
        get() = minOf(c0.y, c1.y)
        set(value) { if(c0.y < c1.y) c0.y = value else c1.y = value }
    /** The maximum y coordinate. */
    var bottom: Float
        get() = maxOf(c0.y, c1.y)
        set(value) { if(c0.y > c1.y) c0.y = value else c1.y = value }
    /** The rectangle width. */
    val width: Float
        get() = abs(c1.x - c0.x)
    /** The rectangle height. */
    val height: Float
        get() = abs(c1.y - c0.y)

    // ---------------------------------------------------------------------------------------------
    // Conversion and copy.
    // ---------------------------------------------------------------------------------------------

    /** Convert to an [android.graphics.RectF]. */
    fun toRectF(): RectF { return RectF(left, top, right, bottom) }

    fun toRect(): Rect { return Rect(left.toInt(), top.toInt(), right.toInt(), bottom.toInt()) }

    /** Copy. */
    open fun copy(): Rectangle { return Rectangle(c0.copy(), c1.copy()) }

    /** Set from another rectangle. */
    fun set(other: Rectangle) { c0 = other.c0.copy(); c1 = other.c1.copy() }

    // ---------------------------------------------------------------------------------------------
    // Geometry
    // ---------------------------------------------------------------------------------------------

    /** Intersect with another rectangle.
     * @return The intersection or null if there is no intersection/overlap.
     * */
    fun intersect(other: Rectangle): Rectangle? {
        val xr = Pair(maxOf(left, other.left), minOf(right, other.right))
        if(xr.first > xr.second) return null
        val yr = Pair(maxOf(top, other.top), minOf(bottom, other.bottom))
        if(yr.first > yr.second) return null
        return Rectangle(c0 = Point(xr.first, yr.first), c1 = Point(xr.second, yr.second))
    }

    /** The corner points at one edge of the rectangle. */
    fun edgePoints(edge: Edge): Pair<Point, Point> {
        return when(edge){
            Edge.LEFT ->   Pair(Point(left, top),    Point(left, bottom))
            Edge.RIGHT ->  Pair(Point(right, top),   Point(right, bottom))
            Edge.TOP ->    Pair(Point(left, top),    Point(right, top))
            Edge.BOTTOM -> Pair(Point(left, bottom), Point(right, bottom))
        }
    }

    /** The distance of a point from the centre of the rectangle, relative to the rectangle's size.
     *
     * @param p The point.
     * @return (x, y) distance of the point from the centre of the rectangle as a fraction of the
     * rectangle's width/height. 0 = centre, -1/1 = edge of rectangle (left/right or top/bottom).
     */
    fun relativeDistance(p: Point): Point {
        return Point(
            (2f * p.x - right - left) / (right - left),
            (2f * p.y - bottom - top) / (bottom - top)
        )
    }

    /** Translate the rectangle. */
    fun translate(dxy: Point) { c0 += dxy; c1 += dxy }

}
