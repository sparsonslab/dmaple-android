// Copyright (c) 2025-2025. Dr Sean Paul Parsons. All rights reserved.

package com.scepticalphysiologist.dmaple.map.field

import android.graphics.Canvas
import android.graphics.Paint
import com.scepticalphysiologist.dmaple.geom.Edge
import com.scepticalphysiologist.dmaple.geom.Frame
import com.scepticalphysiologist.dmaple.etc.randomAlphaString
import com.scepticalphysiologist.dmaple.geom.Point
import com.scepticalphysiologist.dmaple.geom.Rectangle
import com.scepticalphysiologist.dmaple.map.creator.MapType

/** An ROI used for creating a spatio-temporal map.
 *
 * @property frame The reference frame of the ROI.
 * @property c0 The corner of the ROI opposite to [c1].
 * @property c1 The corner of the ROI opposite to [c0].
 * @property threshold The threshold (pixel luminance) that distinguishes gut from background.
 * @property seedingEdge The edge of the ROI along which the map is seeded.
 * @property maps The map types to be created for the ROI.
 * @property uid A unique identifier for the ROI.
 */
class FieldRoi(
    var frame: Frame,
    c0: Point = Point(0f, 0f),
    c1: Point = Point(0f, 0f),
    var threshold: Int = 0,
    var seedingEdge: Edge = Edge.BOTTOM,
    var maps: List<MapType> = listOf(),
    var uid: String = "ROI_${randomAlphaString(20)}",
): Rectangle(c0, c1) {

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
            frame = this.frame.copy(),
            c0 = this.c0.copy(),
            c1 = this.c1.copy(),
            threshold = this.threshold,
            seedingEdge = this.seedingEdge,
            maps = this.maps,
            uid = this.uid
        )
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

    /** Get the bounds of the longitudinal axis of the ROI. */
    fun longitudinalAxis(): Pair<Float, Float> {
        return when(seedingEdge) {
            Edge.LEFT -> Pair(left, right)
            Edge.RIGHT -> Pair(right, left)
            Edge.TOP -> Pair(top, bottom)
            Edge.BOTTOM -> Pair(bottom, top)
        }
    }

    /** Get the bounds of the transverse axis of the ROI. */
    fun transverseAxis(): Pair<Float, Float> {
        return if(seedingEdge.isVertical()) Pair(top, bottom) else Pair(left, right)
    }
}
