// Copyright (c) 2025-2025. Dr Sean Paul Parsons. All rights reserved.

package com.scepticalphysiologist.dmaple.map.field

import android.graphics.Canvas
import android.graphics.Paint
import com.scepticalphysiologist.dmaple.geom.Frame
import com.scepticalphysiologist.dmaple.geom.Point
import kotlin.math.PI

/** A ruler shown on a view used for calibration between pixel units and some measurement unit. */
class FieldRuler(
    /** The ruler's frame. */
    var frame: Frame,
    /** The start point of the ruler in pixel coordinates.. */
    var p0: Point = Point(),
    /** The end point of the ruler in pixel coordinates. */
    var p1: Point = Point(1f, 1f),
    /** The length of the end-caps of the ruler in pixels. */
    var end: Float = 0.1f,
    /** The length of the ruler in the measurement units. */
    var length: Float = 1f,
    /** The unit of the ruler. */
    var unit: String = FieldRuler.allowedUnits.first()
) {

    companion object {
        /** Allowed units for the ruler. */
        val allowedUnits = listOf("mm", "cm", "inch")
    }

    /** Get the resolution (pixels/unit) and unit. */
    fun getResolution(): Pair<Float, String> { return Pair((p1 - p0).l2() / length, unit) }

    fun draw(canvas: Canvas, paint: Paint) {
        canvas.drawLine(p0.x, p0.y, p1.x, p1.y, paint)
        // End caps.
        val theta = (p1 - p0).theta() + 0.5f * PI.toFloat()  // 90 degree to ruler angle.
        val u = Point.unitLength(theta) * end * 0.5f
        for(p in listOf(p0, p1)) {
            val u0 = p - u
            val u1 = p + u
            canvas.drawLine(u0.x, u0.y, u1.x, u1.y, paint)
        }
    }

    fun changeFrame(newFrame: Frame) {
        val lengthRatio = length / (p1 - p0).l2()
        val points = frame.transformPoints(listOf(p0, p1), newFrame, resize=true)
        p0 = points[0]
        p1 = points[1]
        length = (p1 - p0).l2() * lengthRatio
        frame = newFrame
    }

    fun newLength(input: Pair<Float, String>) {
        length = input.first
        unit = input.second
    }

    fun copy(): FieldRuler {
        return FieldRuler(
            frame = this.frame.copy(),
            p0 = this.p0.copy(),
            p1 = this.p1.copy(),
            end = this.end,
            length = this.length,
            unit = this.unit
        )
    }

}

