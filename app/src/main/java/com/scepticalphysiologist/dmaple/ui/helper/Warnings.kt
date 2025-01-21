package com.scepticalphysiologist.dmaple.ui.helper

import android.content.Context
import android.content.DialogInterface
import androidx.appcompat.app.AlertDialog
import com.scepticalphysiologist.dmaple.R


/** Collection of warnings and whether they should stop some process.
 *
 * If both the [negative] and [positive] attributes are null, a default 'Okay' button
 * is shown by the dialog which just dismisses the dialog.
 *
 * @property title Title for the warnings if they are shown in a dialog.
 * @property message The message shown by the alert dialog.
 * @property negative A label and an optional callback, for the dialog's negative button.
 * @property positive A label and an optional callback, for the dialog's positive button.
 */
class Warnings(
    var title: String = "Warning",
) : DialogInterface.OnClickListener  {

    var negative: Pair<String, (() -> Unit)?>? = null
    var positive: Pair<String, (() -> Unit)?>? = null

    private val messages: MutableList<Pair<String, Boolean>> = mutableListOf()

    val message: String
        get() = messages.joinToString("\n\n") { it.first }

    val length: Int
        get() = messages.size

    /** Add a warning and whether it should cause a stop. */
    fun add(message: String, causesStop: Boolean) {
        messages.add(Pair(message, causesStop))
    }

    /** Do any of the warnings stop the process? */
    fun shouldStop(): Boolean { return messages.any {it.second} }

    /** Show the warnings in an alert dialog. */
    fun show(context: Context) {
        // Don't show the dialog if there are messages.
        if (messages.isEmpty()) return
        val dialog = AlertDialog.Builder(context, R.style.warning_dialog)
        dialog.setTitle(title)
        dialog.setMessage(message)
        if ((negative == null) && (positive == null)) negative = Pair("Okay", null)
        negative?.let {dialog.setNegativeButton(it.first, this)}
        positive?.let {dialog.setPositiveButton(it.first, this)}
        dialog.show()
    }

    override fun onClick(dialog: DialogInterface?, button: Int) {
        when(button) {
            DialogInterface.BUTTON_NEGATIVE -> negative?.second?.invoke()
            DialogInterface.BUTTON_POSITIVE -> positive?.second?.invoke()
        }
    }

}
