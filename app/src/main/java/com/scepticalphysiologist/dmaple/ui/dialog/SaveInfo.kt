package com.scepticalphysiologist.dmaple.ui.dialog

import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.AlertDialog
import androidx.compose.material.Text
import androidx.compose.material.TextButton
import androidx.compose.material.TextField
import androidx.compose.runtime.Composable
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.ui.text.input.KeyboardType

/** Options for saving maps at the end of a recording.
 *
 * @param onDoSave A function to call when the user decides to save the maps. The function takes
 * as an argument the suffix the user has chosen for the directory containing the saved maps.
 * @param onDoNotSave A function to call when the user decides not to save the maps.
 */
class SaveInfo(
    val onDoSave: (String) -> Unit,
    val onDoNotSave: () -> Unit
): ComposeDialog() {

    @Composable
    override fun MakeDialog() {
        val openDialog = remember { mutableStateOf(true) }

        val directorySuffix = remember { mutableStateOf("") }

        if(openDialog.value) {
            AlertDialog(
                title = {Text("Save", fontSize = titleFontSize, fontWeight = titleFontWeight)},
                text = {
                    Column {
                        Text(
                            text =  "Do you wish to save the maps?\n" +
                                    "If so you may set a suffix for the directory containing the maps.",
                            fontSize = mainFontSize
                        )
                        TextField(
                            value = directorySuffix.value,
                            readOnly = false,
                            keyboardOptions = KeyboardOptions.Default.copy(keyboardType = KeyboardType.Ascii),
                            maxLines = 1,
                            onValueChange = { directorySuffix.value = it }
                        )
                    }
                },
                onDismissRequest = {},
                confirmButton = {
                    TextButton(
                        onClick = {
                            onDoSave(directorySuffix.value)
                            openDialog.value = false
                        }
                    ) { Text("Save") }
                },
                dismissButton = {
                    TextButton(
                        onClick = {
                            onDoNotSave()
                            openDialog.value = false
                        }
                    ) { Text("Do Not") }
                }
            )
        }

    }

}
