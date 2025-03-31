package com.scepticalphysiologist.dmaple.ui.msg

import android.content.Context
import android.text.InputType
import android.widget.ArrayAdapter
import android.widget.EditText
import android.widget.LinearLayout
import android.widget.Spinner
import androidx.appcompat.app.AlertDialog
import androidx.constraintlayout.widget.ConstraintLayout
import com.scepticalphysiologist.dmaple.etc.sidePaddedLayout

/** A dialog for setting a quantity (magnitude and unit). */
class QuantityDialog(
    title: String = "Provide some quantity",
    message: String = "",
    val initial: Pair<Float, String>,
    val unitSelection: List<String>
): Message<Pair<Float, String>>(title, message) {

    private var magnitude: EditText? = null
    private var unit: Spinner? = null

    override fun createDialog(context: Context): AlertDialog.Builder {
        val dialog = super.createDialog(context)
        magnitude = EditText(context).also {
            it.setText(initial.first.toString())
            it.inputType = InputType.TYPE_CLASS_NUMBER or InputType.TYPE_NUMBER_FLAG_DECIMAL
        }
        unit = Spinner(context).also {
            val style = android.R.layout.simple_spinner_dropdown_item
            it.adapter = ArrayAdapter(context, style, unitSelection)
            val idx = unitSelection.indexOf(initial.second)
            if(idx >= 0) it.setSelection(idx)
        }

        val layout = LinearLayout(context)
        layout.orientation = LinearLayout.HORIZONTAL
        layout.addView(magnitude)
        layout.addView(unit)
        layout.layoutParams = sidePaddedLayout(40)

        dialog.setView(ConstraintLayout(context).also{it.addView(layout)})
        return dialog
    }

    override fun clickReturn(): Pair<Float, String> {
        return Pair(
            magnitude?.text.toString().toFloatOrNull() ?: 1f,
            unit?.selectedItem.toString()
        )
    }

}
