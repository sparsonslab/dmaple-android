package com.scepticalphysiologist.dmaple.geom

import android.graphics.Matrix
import android.view.Display
import android.view.View
import androidx.camera.core.ImageProxy


/** A geometric "frame" within which there may be some points.
 *
 * @property orientation The frame orientation (degrees).
 * @property size The width and height of the frame.
 */
class Frame(val size: Point, val orientation: Int = 0) {

    /** The centre of the frame */
    val centre: Point get() = this.size * 0.5f

    override fun toString(): String {
        return "x = ${size.x}, y = ${size.y}, o =  $orientation"
    }

    /** The difference in orientation with another frame in degrees and radians. */
    private fun deltaOrientation(other: Frame): Pair<Float, Float> {
        val degrees = (other.orientation - this.orientation).toFloat()
        val radians = (Math.PI * degrees / 180.0).toFloat()
        return Pair(degrees, radians)
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
    fun transformPoints(ps:List<Point>, newFrame: Frame, resize: Boolean = true): List<Point> {
        val (rotation, theta) = deltaOrientation(newFrame)
        val rotatedFrameSize = this.size.rotate(theta).abs()
        val newSize = if(resize) newFrame.size else rotatedFrameSize
        val expansion = newSize / rotatedFrameSize
        val offsetRotated = newSize * 0.5f

        // Transform.
        return ps.map {(it - this.centre).rotate(theta) * expansion + offsetRotated}
    }

    /** Create a graphics matrix for transforming from this to a new frame. */
    fun transformMatrix(newFrame: Frame, resize: Boolean = true): Matrix {
        val (rotation, theta) = deltaOrientation(newFrame)
        val rotatedFrameSize = this.size.rotate(theta).abs()
        val newSize = if(resize) newFrame.size else rotatedFrameSize
        val expansion = newSize / rotatedFrameSize
        val offsetRotated = newSize * 0.5f

        // Transform matrix.
        val matrix = Matrix()
        matrix.preConcat(Matrix().also{ it.setTranslate(offsetRotated.x, offsetRotated.y)})
        matrix.preConcat(Matrix().also{ it.setScale(expansion.x, expansion.y) })
        matrix.preConcat(Matrix().also{ it.setRotate(-rotation) })
        matrix.preConcat(Matrix().also{ it.setTranslate(-this.centre.x, -this.centre.y) })
        return matrix
    }

    fun transformRectangle(r: Rectangle, newFrame: Frame, resize: Boolean = true): Rectangle {
        val points = transformPoints(listOf(r.c0, r.c1), newFrame, resize)
        return Rectangle(c0 = points[0], c1 = points[1])
    }

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

}
