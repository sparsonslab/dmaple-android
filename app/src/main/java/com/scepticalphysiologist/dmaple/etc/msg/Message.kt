package com.scepticalphysiologist.dmaple.etc.msg

import android.content.Context
import android.content.DialogInterface
import com.scepticalphysiologist.dmaple.R
import androidx.appcompat.app.AlertDialog

/** A message dialog with button callbacks.
 *
 * If both the [negative] and [positive] attributes are null, a default 'Okay' button
 * is shown by the dialog which just dismisses the dialog.
 *
 * @property title The title of the dialog.
 * @property message The message shown by the dialog.
 * @property negative A label and an optional callback, for the dialog's negative button.
 * @property positive A label and an optional callback, for the dialog's positive button.
 */
abstract class Message(
    protected val title: String = "Message",
    protected var message: String = "Some explanation"
): DialogInterface.OnClickListener {

    var negative: Pair<String, ((String) -> Unit)?>? = null
    var positive: Pair<String, ((String) -> Unit)?>? = null

    /** Create the dialog. */
    protected open fun createDialog(context: Context): AlertDialog.Builder {
        val dialog = AlertDialog.Builder(context, R.style.warning_dialog)
        dialog.setTitle(title)
        dialog.setMessage(message)
        if ((negative == null) && (positive == null)) negative = Pair("Okay", null)
        positive?.let {dialog.setPositiveButton(it.first, this)}
        negative?.let {dialog.setNegativeButton(it.first, this)}
        return dialog
    }

    /** Show the dialog. */
    open fun show(context: Context) { createDialog(context).show() }

    /** Respond to button clicks by invoking the callbacks. */
    override fun onClick(dialog: DialogInterface?, button: Int) {
        when(button) {
            DialogInterface.BUTTON_NEGATIVE -> negative?.second?.invoke(clickString())
            DialogInterface.BUTTON_POSITIVE -> positive?.second?.invoke(clickString())
        }
    }

    /** A string argument passed to callbacks. */
    protected open fun clickString(): String { return "" }

}
