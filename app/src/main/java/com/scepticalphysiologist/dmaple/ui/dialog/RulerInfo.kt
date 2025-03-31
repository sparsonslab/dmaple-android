package com.scepticalphysiologist.dmaple.ui.dialog

import androidx.compose.foundation.layout.Column
import androidx.compose.material.AlertDialog
import androidx.compose.material.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.DropdownMenuItem
import androidx.compose.material.ExperimentalMaterialApi
import androidx.compose.material.ExposedDropdownMenuBox
import androidx.compose.material.TextButton
import androidx.compose.material.TextField
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.input.KeyboardType
import com.scepticalphysiologist.dmaple.map.field.FieldRuler

/** A dialog to set the length and units of a ruler.
 *
 * @param ruler The ruler being set.
 * @param onSetRuler A function to be called when the user decides to set the ruler. Takes as an
 * argument a pair of the ruler length and ruler unit.
 * */
class RulerInfo(
    val ruler: FieldRuler,
    val onSetRuler: (Pair<Float, String>) -> Unit
): ComposeDialog() {

    @OptIn(ExperimentalMaterialApi::class)
    @Composable
    override fun MakeDialog() {
        // State.
        val openDialog = remember { mutableStateOf(true) }
        val expanded = remember { mutableStateOf(false) }
        val selectedUnit = remember { mutableStateOf(ruler.unit) }
        val measuredLength = remember { mutableStateOf(ruler.length.toString()) }

        if(openDialog.value) {
            AlertDialog(
                title = { Text("Ruler", fontSize = titleFontSize, fontWeight = titleFontWeight)},
                text = {
                    Column {
                        Text("Set the ruler length and unit.", fontSize = mainFontSize)
                        Row{
                            TextField(
                                value = measuredLength.value,
                                readOnly = false,
                                keyboardOptions = KeyboardOptions.Default.copy(keyboardType = KeyboardType.Number),
                                onValueChange = { measuredLength.value = it },
                                modifier = Modifier.weight(0.75f)
                            )
                            ExposedDropdownMenuBox(
                                expanded = expanded.value,
                                onExpandedChange = { expanded.value = !expanded.value },
                                modifier = Modifier.weight(0.25f)
                            ) {
                                TextField(
                                    value = selectedUnit.value,
                                    readOnly = true,
                                    onValueChange = { selectedUnit.value = it }
                                )
                                ExposedDropdownMenu(
                                    expanded = expanded.value,
                                    onDismissRequest = { expanded.value = false }
                                ) {
                                    for(unit in FieldRuler.allowedUnits) DropdownMenuItem(
                                        onClick = {
                                            selectedUnit.value = unit
                                            expanded.value = false
                                        }
                                    ) { Text(unit) }
                                }
                            }
                        }
                    }
                },
                onDismissRequest = {},
                confirmButton = {
                    TextButton(
                        onClick = {
                            onSetRuler(Pair(measuredLength.value.toFloatOrNull() ?: 1f, selectedUnit.value))
                            openDialog.value = false
                        }
                    ) { Text("Set") }
                },
                dismissButton = { TextButton(onClick = {openDialog.value = false}) { Text("Cancel") }}
            )
        }
    }

}
