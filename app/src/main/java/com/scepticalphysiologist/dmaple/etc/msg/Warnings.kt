package com.scepticalphysiologist.dmaple.etc.msg

import android.content.Context
import androidx.appcompat.app.AlertDialog

/** A collection of warning messages and whether they should stop some process. */
class Warnings(title: String = "Warning"): Message<List<Pair<String, Boolean>>>(title, "") {

    /** The warning messages. Each message consists of the message and whether it should stop a process. */
    private val messages: MutableList<Pair<String, Boolean>> = mutableListOf()

    /** Add a warning and whether it should cause a stop. */
    fun add(message: String, causesStop: Boolean) { messages.add(Pair(message, causesStop)) }

    /** Do any of the warnings stop the process? */
    fun shouldStop(): Boolean { return messages.any {it.second} }

    /** Create the dialog's message from the concatenation of individual warnings. */
    override fun createDialog(context: Context): AlertDialog.Builder {
        message = messages.joinToString("\n\n") { it.first }
        return super.createDialog(context)
    }

    /** Only show if there are some warning messages.*/
    override fun show(context: Context) { if(messages.isNotEmpty()) super.show(context) }

    override fun clickReturn(): List<Pair<String, Boolean>> { return messages }

}
