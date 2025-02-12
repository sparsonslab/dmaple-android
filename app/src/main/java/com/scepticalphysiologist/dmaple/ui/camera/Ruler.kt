package com.scepticalphysiologist.dmaple.ui.camera

import android.content.Context
import android.graphics.Canvas
import android.graphics.Paint
import android.text.InputType
import com.scepticalphysiologist.dmaple.etc.Frame
import com.scepticalphysiologist.dmaple.etc.Point
import com.scepticalphysiologist.dmaple.etc.msg.InputRequired
import kotlin.math.PI

/** A ruler shown on a view used for calibration between pixel units and some measurement unit. */
class Ruler(
    /** The ruler's frame. */
    var frame: Frame,
    /** The start point of the ruler in pixel coordinates.. */
    var p0: Point = Point(),
    /** The end point of the ruler in pixel coordinates. */
    var p1: Point = Point(1f, 1f),
    /** The length of the ruler in the measurement units. */
    var length: Float = 1f,
    /** The length of the end-caps of the ruler in pixels. */
    var end: Float = 0.1f
) {

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
        val points = frame.transform(listOf(p0, p1), newFrame, resize=true)
        p0 = points[0]
        p1 = points[1]
        length = (p1 - p0).l2() * lengthRatio
        frame = newFrame
    }

    fun editLength(context: Context) {
        val dialog = InputRequired(
            title = "Ruler Length",
            message = "Set the length of the ruler in real units (cm, mm, etc.).",
            initialValue = length.toString(),
            inputType = InputType.TYPE_CLASS_NUMBER or InputType.TYPE_NUMBER_FLAG_DECIMAL
        )
        dialog.positive = Pair("Set", this::newLength)
        dialog.negative = Pair("Cancel", null)
        dialog.show(context)
    }

    fun newLength(input: String) {
        length = input.toFloat()
    }

}
