package com.scepticalphysiologist.dmaple.ui.msg


import android.app.Activity
import android.view.ViewGroup
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.material.AlertDialog
import androidx.compose.material.Checkbox
import androidx.compose.material.Text
import androidx.compose.material.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.ComposeView
import com.scepticalphysiologist.dmaple.map.creator.MapType
import com.scepticalphysiologist.dmaple.map.field.FieldRoi

class RoiInfo(
    val roi: FieldRoi,
    val onSetRoi: (Map<MapType, Boolean>) -> Unit
) {

    private val selections = MapType.entries.map{it to (it in roi.maps)}.toMap().toMutableMap()

    private var description: String = "some description"

    fun show(activity: Activity) {
        activity.addContentView(
            ComposeView(activity).apply {
                setContent {
                    var showDialog by remember { mutableStateOf(true) }
                    if (showDialog) RoiInfoDialog()
                }
            },
            ViewGroup.LayoutParams(
                ViewGroup.LayoutParams.MATCH_PARENT,
                ViewGroup.LayoutParams.MATCH_PARENT
            )
        )
    }

    @Composable
    fun RoiInfoDialog() {

        val openDialog = remember { mutableStateOf(true) }

        if(openDialog.value) {
            AlertDialog(
                title = { Text("Roi Info") },
                text = {
                    Column {
                        Text("some info about the ROI.")
                        for((map, selected) in selections) MapTypeRow(map, selected)
                        Text(description)
                    }
                },
                onDismissRequest = { },
                confirmButton = {
                    TextButton(
                        onClick = {
                            onSetRoi(selections)
                            openDialog.value = false
                        }
                    ) { Text("Set") }
                },
                dismissButton = {
                    TextButton(
                        onClick = { openDialog.value = false }
                    )  { Text("Cancel")}
                }
            )
        }
    }

    @Composable
    fun MapTypeRow(map: MapType, initialSelection: Boolean) {
        var selected by remember { mutableStateOf(initialSelection) }
        Row {
            Checkbox(
                checked=selected,
                onCheckedChange = {
                    selected = it
                    selections[map] = it
                }
            )
            Text(map.title, modifier = Modifier.align(Alignment.CenterVertically))
        }
    }

}
