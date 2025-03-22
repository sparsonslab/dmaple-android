package com.scepticalphysiologist.dmaple.map.field

import android.graphics.Canvas
import android.graphics.Paint
import android.graphics.RectF
import com.scepticalphysiologist.dmaple.geom.Edge
import com.scepticalphysiologist.dmaple.geom.Frame
import com.scepticalphysiologist.dmaple.geom.Point
import com.scepticalphysiologist.dmaple.etc.randomAlphaString
import com.scepticalphysiologist.dmaple.map.creator.MapType

/** An ROI used for creating a spatio-temporal map.
 *
 * @property frame The reference frame of the ROI.
 * @property threshold The threshold (pixel luminance) that distinguishes gut from background.
 * @property seedingEdge The edge of the ROI along which the map is seeded.
 */
class FieldRoi(
    var frame: Frame,
    var threshold: Int = 0,
    var seedingEdge: Edge = Edge.BOTTOM,
    var maps: List<MapType> = listOf(),
    var uid: String = randomAlphaString(20)
): RectF(0f, 0f, 0f, 0f) {

    // Required for unit tests because RectF.hashCode is a stub in the test version of android.graphics
    override fun hashCode(): Int { return uid.map{it.toInt()}.sum() + threshold }

    /** Change the ROI's reference frame. */
    fun changeFrame(newFrame: Frame) {
        this.set(frame.transformRect(this, newFrame, resize=true))
        seedingEdge = seedingEdge.rotate(newFrame.orientation - frame.orientation)
        frame = newFrame
    }

    /** Get a copy of the ROI in a new frame. */
    fun inNewFrame(newFrame: Frame): FieldRoi {
        val cpy = this.copy()
        cpy.changeFrame(newFrame)
        return cpy
    }

    /** Make the ROI a "valid" RectF object. i.e. left < right && top < bottom. */
    fun makeValid() {
        if(left > right) {
            val p = right
            right = left
            left = p
            if(seedingEdge == Edge.LEFT) seedingEdge = Edge.RIGHT
            else if(seedingEdge == Edge.RIGHT) seedingEdge = Edge.RIGHT
        }
        if(top > bottom) {
            val p = bottom
            bottom = top
            top = p
            if(seedingEdge == Edge.BOTTOM) seedingEdge = Edge.TOP
            else if(seedingEdge == Edge.TOP) seedingEdge = Edge.BOTTOM
        }
    }

    /** Crop the ROI to its frame. */
    fun cropToFrame() {
        makeValid()
        this.intersect(frame.size.toRect())
    }

    /** Copy the ROI. */
    fun copy(): FieldRoi {
        val cpy = FieldRoi(
            frame = this.frame,
            threshold = this.threshold,
            seedingEdge = this.seedingEdge,
            maps = this.maps,
            uid = this.uid
        )
        cpy.set(this)
        return cpy
    }

    /** Draw the ROI. */
    fun draw(canvas: Canvas, paint: Paint) {
        canvas.drawRect(this, paint)
        val (p0, p1) = Point.ofRectEdge(this, this.seedingEdge)
        val pnt = Paint()
        pnt.set(paint)
        pnt.strokeWidth = paint.strokeWidth * 5
        canvas.drawLine(p0.x, p0.y, p1.x, p1.y, pnt)
    }

}
