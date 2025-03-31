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
import androidx.compose.ui.res.dimensionResource
import androidx.compose.ui.unit.sp
import com.scepticalphysiologist.dmaple.MainActivity
import com.scepticalphysiologist.dmaple.R
import com.scepticalphysiologist.dmaple.map.creator.GutSegmentor
import com.scepticalphysiologist.dmaple.map.creator.MapType
import com.scepticalphysiologist.dmaple.map.field.FieldRoi
import kotlin.math.abs

/** A dialog for showing information about a mapping ROI and selecting map types.
 *
 * @param roi The ROI to show information about.
 * @param onSetRoi A function to call when the dialog "Set" button is pressed, that takes as
 * argument a list of selected map types.
 * */
class RoiInfo(
    val roi: FieldRoi,
    val onSetRoi: (List<MapType>) -> Unit
) {

    /** The current map selection. */
    private val selections = MapType.entries.map{it to (it in roi.maps)}.toMap().toMutableMap()

    /** Make a description of the ROI based upon the current map selection. */
    private fun makeDescription(): String {
        // Samples and bytes.
        val longAxes = roi.longitudinalAxis()
        val samplePerFrame = abs((longAxes.first - longAxes.second).toInt()) / GutSegmentor.spineSkipPixels
        val bytesPerSample = selections.filter { it.value }.map{it.key.bytesPerSample}.maxOrNull() ?: 0
        val items: MutableList<Pair<String, Any>> = mutableListOf(
            Pair("bytes / sample", bytesPerSample),
            Pair("samples / frame", samplePerFrame),
        )

        // Recording time estimation.
        MainActivity.mapService?.let { service ->
            val framesPerSec = service.getFps()
            val bytesPerBuffer = service.getBufferSize()
            val bytesPerSecond = bytesPerSample * samplePerFrame * framesPerSec
            val minutesPerBuffer = bytesPerBuffer.toFloat() / (bytesPerSecond * 60f)
            items.add(Pair("frames / second", framesPerSec))
            items.add(Pair("bytes / second", bytesPerSecond))
            items.add(Pair("buffer capacity [MB]", bytesPerBuffer / 1_000_000))
            items.add(Pair("recording capacity [min]", minutesPerBuffer.toInt()))
        }
        return items.map{"${it.first}\t${it.second}"}.joinToString("\n")
    }

    /** Show the dialog. */
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

    /** The composable dialog. */
    @Composable
    fun RoiInfoDialog() {
        // State.
        val openDialog = remember { mutableStateOf(true) }
        val description = remember { mutableStateOf( makeDescription() ) }
        fun setDescription() { description.value = makeDescription() }

        if(openDialog.value) {
            AlertDialog(
                title = { Text("Roi Info") },
                text = {
                    Column {
                        Text(
                            text = "Set the maps to be created and get information about the recording.",
                            fontSize = dimensionResource(R.dimen.small_text_size).value.sp
                        )
                        for((map, selected) in selections) MapTypeRow(map, selected, ::setDescription)
                        val items = description.value.split("\n").map{it.split("\t")}
                        val fs = dimensionResource(R.dimen.extra_small_text_size).value.sp
                        for(item in items) Row() {
                            Text(text = item[1], modifier = Modifier.weight(0.2f), fontSize = fs)
                            Text(text = item[0], modifier = Modifier.weight(0.8f), fontSize = fs)
                        }
                    }
                },
                onDismissRequest = { },
                confirmButton = {
                    TextButton(
                        onClick = {
                            onSetRoi(selections.filter{it.value}.keys.toList())
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

    /** A single, checkable map selection. */
    @Composable
    fun MapTypeRow(map: MapType, initialSelection: Boolean, onSelected: () -> Unit) {
        var selected by remember { mutableStateOf(initialSelection) }
        Row {
            Checkbox(
                checked=selected,
                onCheckedChange = {
                    selected = it
                    selections[map] = it
                    onSelected()
                }
            )
            Text(map.title, modifier = Modifier.align(Alignment.CenterVertically))
        }
    }

}
