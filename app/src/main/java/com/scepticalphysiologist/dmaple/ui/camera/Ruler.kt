package com.scepticalphysiologist.dmaple.ui.camera

import android.content.Context
import android.content.DialogInterface
import android.graphics.Canvas
import android.graphics.Paint
import android.text.InputType
import android.widget.EditText
import android.widget.FrameLayout.LayoutParams
import android.widget.LinearLayout
import android.widget.RelativeLayout
import android.widget.TextView
import androidx.appcompat.app.AlertDialog
import androidx.core.view.marginLeft
import androidx.core.view.marginRight
import androidx.core.view.updateLayoutParams
import com.scepticalphysiologist.dmaple.R
import com.scepticalphysiologist.dmaple.ui.helper.marginedLayoutParams
import com.scepticalphysiologist.dmaple.ui.helper.paddedFrameLayout
import com.scepticalphysiologist.dmaple.ui.helper.sidePaddedLayout
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
): DialogInterface.OnClickListener {

    var lengthInput: EditText? = null

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
        val dialog = AlertDialog.Builder(context, R.style.warning_dialog)
        dialog.setTitle("Ruler Length")
        dialog.setCancelable(true)
        lengthInput = EditText(context).also {
            it.setText(length.toString())
            it.inputType = InputType.TYPE_CLASS_NUMBER or InputType.TYPE_NUMBER_FLAG_DECIMAL
            // todo - try and put some margins either side of edittext. Tried various layouts but having no effect.
            dialog.setView(it)
        }
        dialog.setPositiveButton("Set", this)
        dialog.setNegativeButton("Cancel", this)
        dialog.show()
    }

    override fun onClick(p0: DialogInterface?, button: Int) {
        when(button) {
            DialogInterface.BUTTON_NEGATIVE -> {}
            DialogInterface.BUTTON_POSITIVE -> lengthInput?.let {length = it.text.toString().toFloat()}
        }
    }


}
