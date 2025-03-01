package com.scepticalphysiologist.dmaple.etc

import android.graphics.Matrix
import android.graphics.Rect
import android.graphics.RectF
import android.view.Display
import android.view.MotionEvent
import android.view.Surface
import android.view.View
import androidx.camera.core.AspectRatio
import androidx.camera.core.ImageProxy
import kotlin.math.cos
import kotlin.math.sin
import kotlin.math.abs
import kotlin.math.atan
import kotlin.math.min
import kotlin.math.pow


// -------------------------------------------------------------------------------------------------
// Conversion of geometric enum to real values
// -------------------------------------------------------------------------------------------------

/** Get the degree value of a Surface rotation enum. */
fun surfaceRotationDegrees(rotation: Int): Int {
    return when(rotation) {
        Surface.ROTATION_0 -> 0
        Surface.ROTATION_90 -> 90
        Surface.ROTATION_180 -> 180
        Surface.ROTATION_270 -> 270
        else -> 0
    }
}

/** Get the ratio value of an AspectRatio ratio enum. */
fun aspectRatioRatio(aspect: Int): Float {
    return when(aspect) {
        AspectRatio.RATIO_16_9 -> 16f/9f
        AspectRatio.RATIO_4_3 -> 4f/3f
        AspectRatio.RATIO_DEFAULT -> 1f
        else -> 1f
    }
}

// -------------------------------------------------------------------------------------------------
// Rectangle handling
// Rect(left, top, right, bottom)
// left < right, top < bottom
// -------------------------------------------------------------------------------------------------

fun validRect(rect: RectF): Rect {
    return Rect(
        minOf(rect.left, rect.right).toInt(),
        minOf(rect.bottom, rect.top).toInt(),
        maxOf(rect.left, rect.right).toInt(),
        maxOf(rect.top, rect.bottom).toInt()
    )
}

enum class Edge {
    LEFT,
    BOTTOM,
    RIGHT,
    TOP;

    /** Rotate an edge. */
    fun rotate(degrees: Int): Edge {
        var k = (this.ordinal + degrees / 90) % 4
        if (k < 0) k += 4
        return Edge.entries[k]
    }

    fun isVertical(): Boolean {
        return (this == LEFT) || (this == RIGHT)
    }
}

fun printRect(r: Rect, name: String = "") {
    println("$name = ${r.left} - ${r.right}, ${r.top} - ${r.bottom}")
}

fun printRectF(r: RectF, name: String = "") {
    println("$name = ${r.left} - ${r.right}, ${r.top} - ${r.bottom}")
}



// -------------------------------------------------------------------------------------------------
// Geometric classes
// -------------------------------------------------------------------------------------------------

/**
 * An point on the Cartesian plain.
 *
 * Nice operator overloading, unlike android.graphics.PointF.
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

    fun abs(): Point { return Point(abs(x), abs(y)) }

    fun swap(): Point { return Point(y, x) }

    fun min(): Float { return min(x, y) }

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

    /** The distance of the point from the centre of a rectangle, relative to its size.
     *
     * @param r The rectangle.
     * @return Distance of (x, y) from the centre of the rectangle as a fraction of its width/height.
     * 0 = centre, -1/1 = edge of rectangle (left/right or top/bottom).
     */
    fun relativeDistance(r: RectF): Point {
        return Point(
            (2f * x - r.right - r.left) / (r.right - r.left),
            (2f * y - r.bottom - r.top) / (r.bottom - r.top)
        )
    }

    fun toRect(): RectF { return RectF(0f, 0f, x, y) }

    /** Functions that operate on collections of points. */
    companion object {

        fun minOf(p0: Point, p1: Point): Point { return Point(minOf(p0.x, p1.x), minOf(p0.y, p1.y)) }

        fun maxOf(p0: Point, p1: Point): Point { return Point(maxOf(p0.x, p1.x), maxOf(p0.y, p1.y)) }

        /** The point with unit length from the origin for a given angle. */
        fun unitLength(theta: Float): Point { return Point(cos(theta), sin(theta)) }

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


        /** The points at the edge of a rectangle. */
        fun ofRectEdge(r: RectF, edge: Edge): Pair<Point, Point> {
            return when(edge){
                Edge.LEFT -> Pair(Point(r.left, r.top), Point(r.left, r.bottom))
                Edge.RIGHT -> Pair(Point(r.right, r.top), Point(r.right, r.bottom))
                Edge.TOP -> Pair(Point(r.left, r.top), Point(r.right, r.top))
                Edge.BOTTOM -> Pair(Point(r.left, r.bottom), Point(r.right, r.bottom))
            }
        }

        fun ofViewExtent(view: View): Point {
            return Point(view.width.toFloat(), view.height.toFloat())
        }

        fun ofMotionEvent(event: MotionEvent): Point { return Point(event.x, event.y) }

    }

}

/** A geometric "frame" within which there may be some points.
 *
 * @property orientation The frame orientation (degrees).
 * @property size The width and height of the frame.
 */
class Frame(val size: Point, val orientation: Int = 0) {

    //val size: Point = Point(width, height)

    companion object {

        /**  Get a view's frame. */
        fun fromView(view: View, display: Display): Frame {
            return Frame(
                Point(view.width.toFloat(), view.height.toFloat()),
                orientation = surfaceRotationDegrees(display.rotation)
            )
        }

        /** Get an image's frame. */
        fun fromImage(image: ImageProxy): Frame {
            return Frame(
                Point(image.width.toFloat(), image.height.toFloat()),
                orientation = image.imageInfo.rotationDegrees
            )
        }

    }

    override fun toString(): String {
        return "x = ${size.x}, y = ${size.y}, o =  $orientation"
    }


    /** Translate points from this frame to another.
     *
     * Proceeds by the following transforms:
     * 1) Offset the point relative to the centre of its frame (i.e. make the centre the origin).
     * 2) Rotate the point.
     * 3) [if resize == true] Expand the point by the size ratio of its rotated frame to this frame.
     * 4) Offset the point relative to the corner the new frame.
     *
     * @param ps Some points.
     * @param newFrame The transformed frame.
     * @param resize The frame transform includes re-sizing.
     * @return The transformed points.
     */
    fun transform(ps:List<Point>, newFrame: Frame, resize: Boolean = true): List<Point> {
        // Rotation angle.
        val rotation = newFrame.orientation - this.orientation
        val theta = (Math.PI * rotation / 180.0).toFloat()

        // Offset from origin of original frame.
        val offset = this.size * 0.5f

        // Expansion and offset for new frame size.
        val rotatedFrameSize = this.size.rotate(theta).abs()
        val newSize = if(resize) newFrame.size else rotatedFrameSize
        val expansion = newSize / rotatedFrameSize
        val offsetRotated = newSize * 0.5f

        // Transform.
        return ps.map {(it - offset).rotate(theta) * expansion + offsetRotated}
    }


    /** Translate a rectangle from this frame to another.
     *
     * @param r The rectangle to be transformed.
     * @param newFrame The transformed rectangle's frame.
     * @param resize The frame transform includes re-sizing.
     * @return The transformed rectangle.
     */
    fun transformRect(r: RectF, newFrame: Frame, resize: Boolean = true): RectF {
        return Point.toRect(transform(Point.fromRect(r), newFrame, resize))!!
    }


    fun transformMatrix(newFrame: Frame, resize: Boolean = true): Matrix {

        // Rotation angle.
        val rotation = (newFrame.orientation - this.orientation).toFloat()
        val theta = (Math.PI * rotation / 180.0).toFloat()

        // Offset from origin of original frame.
        val offset = this.size * 0.5f

        // Expansion and offset for new frame size.
        val rotatedFrameSize = this.size.rotate(theta).abs()
        val newSize = if(resize) newFrame.size else rotatedFrameSize
        val expansion = newSize / rotatedFrameSize
        val offsetRotated = newSize * 0.5f

        val matrix = Matrix()
        matrix.preConcat(Matrix().also{ it.setTranslate(offsetRotated.x, offsetRotated.y)})
        matrix.preConcat(Matrix().also{ it.setScale(expansion.x, expansion.y) })
        matrix.preConcat(Matrix().also{ it.setRotate(-rotation) })
        matrix.preConcat(Matrix().also{ it.setTranslate(-offset.x, -offset.y) })
        return matrix
    }

}

