package com.scepticalphysiologist.dmaple.map.field

import android.graphics.Canvas
import android.graphics.Paint
import com.scepticalphysiologist.dmaple.geom.Edge
import com.scepticalphysiologist.dmaple.geom.Frame
import com.scepticalphysiologist.dmaple.etc.randomAlphaString
import com.scepticalphysiologist.dmaple.geom.Rectangle
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
): Rectangle() {

    // Required for unit tests because RectF.hashCode is a stub in the test version of android.graphics
    override fun hashCode(): Int { return uid.map{it.toInt()}.sum() + threshold }

    /** Change the ROI's reference frame. */
    fun changeFrame(newFrame: Frame) {
        this.set(frame.transformRectangle(this, newFrame, resize=true))
        seedingEdge = seedingEdge.rotate(newFrame.orientation - frame.orientation)
        frame = newFrame
    }

    /** Get a copy of the ROI in a new frame. */
    fun inNewFrame(newFrame: Frame): FieldRoi {
        val cpy = copy()
        cpy.changeFrame(newFrame)
        return cpy
    }

    /** Crop the ROI to its frame. */
    fun cropToFrame() { intersect(frame.size.toRectangle())?.let { set(it) } }

    /** Copy the ROI. */
    override fun copy(): FieldRoi {
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
        canvas.drawRect(this.toRectF(), paint)
        val (p0, p1) = edgePoints(seedingEdge)
        val pnt = Paint()
        pnt.set(paint)
        pnt.strokeWidth = paint.strokeWidth * 5
        canvas.drawLine(p0.x, p0.y, p1.x, p1.y, pnt)
    }

}
