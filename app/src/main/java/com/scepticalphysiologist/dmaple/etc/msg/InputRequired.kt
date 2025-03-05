package com.scepticalphysiologist.dmaple.etc.msg

import android.content.Context
import android.text.InputType
import android.widget.EditText
import androidx.appcompat.app.AlertDialog
import androidx.constraintlayout.widget.ConstraintLayout
import com.scepticalphysiologist.dmaple.etc.sidePaddedLayout

/** A message to provide an input. */
class InputRequired(
    title: String = "Input",
    message: String = "Provide some input.",
    /** The initial value to be shown in the input box. */
    val initialValue: Any = "",
    /** The valid input types. */
    val inputType: Int = InputType.TYPE_CLASS_TEXT,
): Message<String>(title, message) {

    /** Input widget. */
    private var input: EditText? = null

    override fun createDialog(context: Context): AlertDialog.Builder {
        val dialog =  super.createDialog(context)
        input = EditText(context).also {
            it.setText(initialValue.toString())
            it.inputType = inputType
            it.layoutParams = sidePaddedLayout(40)
        }
        val layout = ConstraintLayout(context)
        layout.addView(input)
        dialog.setView(layout)
        return dialog
    }

    override fun clickReturn(): String { return input?.text.toString() }

}
