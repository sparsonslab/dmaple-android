package com.scepticalphysiologist.dmaple.ui.camera

import android.graphics.Canvas
import android.graphics.Paint
import kotlin.math.PI

class Ruler(
    var frame: Frame,
    var p0: Point = Point(),
    var p1: Point = Point(),
    var length: Float = 1f,
    var end: Float = 0.1f
) {


    fun draw(canvas: Canvas, paint: Paint) {
        canvas.drawLine(p0.x, p0.y, p1.x, p1.y, paint)
        // Flat ends.
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
        val points = frame.transform(listOf(p0, p1), newFrame, resize=true)
        p0 = points[0]
        p1 = points[1]
        length = (p1 - p0).l2() * lengthRatio
        frame = newFrame
    }

}
