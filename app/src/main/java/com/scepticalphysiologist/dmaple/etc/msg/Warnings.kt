package com.scepticalphysiologist.dmaple.etc.msg

import android.content.Context
import androidx.appcompat.app.AlertDialog

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
class Warnings(title: String = "Warning") : Message(title, "") {

    private val messages: MutableList<Pair<String, Boolean>> = mutableListOf()

    /** Add a warning and whether it should cause a stop. */
    fun add(message: String, causesStop: Boolean) { messages.add(Pair(message, causesStop)) }

    /** Do any of the warnings stop the process? */
    fun shouldStop(): Boolean { return messages.any {it.second} }

    override fun createDialog(context: Context): AlertDialog.Builder {
        message = messages.joinToString("\n\n") { it.first }
        return super.createDialog(context)
    }

    override fun show(context: Context) {
        if (messages.isNotEmpty()) super.show(context)
    }

}
