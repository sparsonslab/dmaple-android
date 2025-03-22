package com.scepticalphysiologist.dmaple.geom

import android.graphics.RectF
import kotlin.math.abs

open class Rectangle (
    var c0: Point = Point(),
    var c1: Point = Point()
){

    var left: Float
        get() = minOf(c0.x, c1.x)
        set(value) { if(c0.x < c1.x) c0.x = value else c1.x = value }

    var right: Float
        get() = maxOf(c0.x, c1.x)
        set(value) { if(c0.x > c1.x) c0.x = value else c1.x = value }

    var top: Float
        get() = minOf(c0.y, c1.y)
        set(value) { if(c0.y < c1.y) c0.y = value else c1.y = value }

    var bottom: Float
        get() = maxOf(c0.y, c1.y)
        set(value) { if(c0.y > c1.y) c0.y = value else c1.y = value }

    val width: Float
        get() = abs(c1.x - c0.x)

    val height: Float
        get() = abs(c1.y - c0.y)



    fun toRectF(): RectF { return RectF(left, top, right, bottom) }

    open fun copy(): Rectangle { return Rectangle(c0.copy(), c1.copy()) }

    fun set(other: Rectangle) {
        this.c0 = other.c0.copy()
        this.c1 = other.c1.copy()
    }

    fun intersect(other: Rectangle): Rectangle? {
        val xr = Pair(maxOf(left, other.left), minOf(right, other.right))
        if(xr.first > xr.second) return null
        val yr = Pair(maxOf(top, other.top), minOf(bottom, other.bottom))
        if(yr.first > yr.second) return null
        return Rectangle(c0 = Point(xr.first, yr.first), c1 = Point(xr.second, yr.second))
    }

    fun edgePoints(edge: Edge): Pair<Point, Point> {
        return when(edge){
            Edge.LEFT -> Pair(Point(left, top), Point(left, bottom))
            Edge.RIGHT -> Pair(Point(right, top), Point(right, bottom))
            Edge.TOP -> Pair(Point(left, top), Point(right, top))
            Edge.BOTTOM -> Pair(Point(left, bottom), Point(right, bottom))
        }
    }

    fun relativeDistance(p: Point): Point {
        return Point(
            (2f * p.x - right - left) / (right - left),
            (2f * p.y - bottom - top) / (bottom - top)
        )
    }

    fun translate(dxy: Point) {
        c0 += dxy
        c1 += dxy
    }


}