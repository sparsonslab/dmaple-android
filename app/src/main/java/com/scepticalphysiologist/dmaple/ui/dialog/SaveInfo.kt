package com.scepticalphysiologist.dmaple.ui.dialog

import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.AlertDialog
import androidx.compose.material.Text
import androidx.compose.material.TextButton
import androidx.compose.material.TextField
import androidx.compose.runtime.Composable
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.AnnotatedString
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.input.OffsetMapping
import androidx.compose.ui.text.input.TransformedText
import androidx.compose.ui.text.input.VisualTransformation
import com.scepticalphysiologist.dmaple.map.record.MappingRecord

/** Options for saving maps at the end of a recording.
 *
 * @param onDoSave A function to call when the user decides to save the maps. The function takes
 * as an argument the name of the folder containing the saved maps.
 * @param onDoNotSave A function to call when the user decides not to save the maps.
 */
class SaveInfo(
    val onDoSave: (String) -> Unit,
    val onDoNotSave: () -> Unit
): ComposeDialog() {

    @Composable
    override fun MakeDialog() {
        val openDialog = remember { mutableStateOf(true) }

        val recordFolder = remember { mutableStateOf(MappingRecord.DEFAULT_RECORD_FOLDER) }

        if(openDialog.value) {
            AlertDialog(
                title = {Text("Save", fontSize = titleFontSize, fontWeight = titleFontWeight)},
                text = {
                    Column {
                        Text(
                            text =  "Do you wish to save the maps?\n" +
                                    "Set a name for the folder containing the maps:",
                            fontSize = mainFontSize
                        )
                        TextField(
                            value = recordFolder.value,
                            readOnly = false,
                            keyboardOptions = KeyboardOptions.Default.copy(
                                keyboardType = KeyboardType.Ascii,
                                autoCorrectEnabled = false,
                            ),
                            maxLines = 1,
                            onValueChange = { recordFolder.value = it },
                            visualTransformation = ValidDirectory(),
                            modifier = Modifier.fillMaxWidth()
                        )
                    }
                },
                onDismissRequest = {},
                confirmButton = {
                    TextButton(
                        onClick = {
                            onDoSave(recordFolder.value)
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

/** As the user inputs a record folder, replace all non-alphanumeric characters with an underscore. */
class ValidDirectory: VisualTransformation {
    override fun filter(text: AnnotatedString): TransformedText {
        val trans = text.text.map{char ->  if(char.isLetterOrDigit()) char else '_'}.joinToString("")
        return TransformedText(
            text = AnnotatedString(trans),
            offsetMapping = OffsetMapping.Identity
        )
    }
}
