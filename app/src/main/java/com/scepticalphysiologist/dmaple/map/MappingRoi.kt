package com.scepticalphysiologist.dmaple.map

import android.graphics.Canvas
import android.graphics.Paint
import android.graphics.RectF
import com.scepticalphysiologist.dmaple.etc.Edge
import com.scepticalphysiologist.dmaple.etc.Frame
import com.scepticalphysiologist.dmaple.etc.Point
import com.scepticalphysiologist.dmaple.etc.randomAlphaString
import com.scepticalphysiologist.dmaple.map.creator.MapType

/** An ROI used for creating a spatio-temporal map.
 *
 * @property frame The reference frame of the ROI.
 * @property threshold The threshold (pixel luminance) that distinguishes gut from background.
 * @property seedingEdge The edge of the ROI along which the map is seeded.
 */
class MappingRoi(
    var frame: Frame,
    var threshold: Int = 0,
    var seedingEdge: Edge = Edge.BOTTOM,
    var maps: List<MapType> = listOf(),
    var uid: String = randomAlphaString(20)
): RectF(0f, 0f, 0f, 0f) {




    /** Change the ROI's reference frame. */
    fun changeFrame(newFrame: Frame) {
        this.set(frame.transformRect(this, newFrame, resize=true))
        seedingEdge = seedingEdge.rotate(newFrame.orientation - frame.orientation)
        frame = newFrame
    }

    /** Get a copy of the ROI in a new frame. */
    fun inNewFrame(newFrame: Frame): MappingRoi {
        val cpy = this.copy()
        cpy.changeFrame(newFrame)
        return cpy
    }

    /** Copy the ROI. */
    fun copy(): MappingRoi {
        val cpy= MappingRoi(
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
