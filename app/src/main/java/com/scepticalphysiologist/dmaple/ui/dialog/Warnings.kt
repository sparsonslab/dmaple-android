package com.scepticalphysiologist.dmaple.ui.dialog

import android.app.Activity
import androidx.compose.foundation.layout.Column
import androidx.compose.material.AlertDialog
import androidx.compose.material.Text
import androidx.compose.material.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember

/** A collection of warning messages and whether they should stop some process. */
class Warnings(
    val title: String = "Warning"
): ComposeDialog() {

    /** The warning messages. Each message consists of the message and whether it should stop a process. */
    private val messages: MutableList<Pair<String, Boolean>> = mutableListOf()

    /** Add a warning and whether it should cause a stop. */
    fun add(message: String, causesStop: Boolean) { messages.add(Pair(message, causesStop)) }

    /** Do any of the warnings stop the process? */
    fun shouldStop(): Boolean { return messages.any {it.second} }

    /** Only show if there are some messages. */
    override fun show(activity: Activity) { if(messages.isNotEmpty()) super.show(activity) }

    @Composable
    override fun MakeDialog() {
        // State.
        val openDialog = remember { mutableStateOf(true) }

        if(openDialog.value) {
            AlertDialog(
                title = { Text(title) },
                text = {
                    Column { for(message in messages) Text(message.first) }
                },
                onDismissRequest = {},
                confirmButton = {},
                dismissButton = { TextButton(onClick = {openDialog.value = false}) { Text("Okay") }}
            )
        }

    }

}
