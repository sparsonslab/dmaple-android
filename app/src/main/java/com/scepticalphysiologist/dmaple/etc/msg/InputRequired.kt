package com.scepticalphysiologist.dmaple.etc.msg

import android.content.Context
import android.text.InputType
import android.widget.EditText
import androidx.appcompat.app.AlertDialog

class InputRequired(
    title: String = "Input",
    message: String = "Provide some input.",
    val initialValue: Any = "",
    val inputType: Int = InputType.TYPE_CLASS_TEXT,
): Message(title, message) {

    var input: EditText? = null

    override fun createDialog(context: Context): AlertDialog.Builder {
        val dialog =  super.createDialog(context)
        input = EditText(context).also {
            it.setText(initialValue.toString())
            it.inputType = inputType
            dialog.setView(it)
        }
        return dialog
    }

    override fun clickString(): String { return input?.text.toString() }

}
