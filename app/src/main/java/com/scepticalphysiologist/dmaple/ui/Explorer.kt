// Copyright (c) 2025-2025. Dr Sean Paul Parsons. All rights reserved.

package com.scepticalphysiologist.dmaple.ui

import android.annotation.SuppressLint
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.CircularProgressIndicator
import androidx.compose.material.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.AbsoluteAlignment
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.graphics.toArgb
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.platform.ComposeView
import androidx.compose.ui.res.dimensionResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.fragment.app.Fragment
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.ViewModelProvider
import androidx.navigation.fragment.findNavController
import com.scepticalphysiologist.dmaple.R
import com.scepticalphysiologist.dmaple.etc.strftime
import com.scepticalphysiologist.dmaple.map.record.MappingRecord
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch

/** Explore saved records and load them.
 *
 */
class Explorer: Fragment() {

    /** Is a record being loaded? Used to prevent more than one record from being loaded. */
    private var isLoading = false

    /** When a record has been loaded. */
    private val recordHasBeenLoaded = MutableLiveData<Boolean?>(null)

    /** Corner rounding for the record items shown. */
    private val itemShape = RoundedCornerShape(
        topStart = 10.dp, topEnd = 10.dp, bottomStart = 10.dp, bottomEnd = 10.dp
    )

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {

        // Once a record has been loaded, navigate to the recording fragment to show the recording.
        // This has to be done in response to a live data object, rather than within the composable's
        // on-click call-back because doing the latter blocks the progress indicator.
        recordHasBeenLoaded.observe(viewLifecycleOwner) { loaded ->
            if(loaded == true) findNavController().navigate(R.id.recorder)
        }

        // A lazy grid of recordings.
        val view = ComposeView(requireActivity())
        view.setBackgroundColor(Color.DarkGray.toArgb())
        view.setContent {
            LazyColumn (
                verticalArrangement = Arrangement.spacedBy(10.dp),
                modifier = Modifier.padding(10.dp)
            ) { items(MappingRecord.records.size) { i -> RecordItem(i) } }
        }
        return view
    }

    @SuppressLint("DefaultLocale")
    private fun recordDescription(record: MappingRecord): String {
        val d = record.duration()
        var description = "duration: ${String.format(
            "%02d:%02d:%02d", d.toHours(), d.toMinutesPart(), d.toSecondsPart()
        )}\n"
        for(creator in record.creators) description +=
            "${creator.roi.uid}: ${creator.roi.maps.joinToString(", ").lowercase()}\n"
        return description
    }

    /** A composable that shows a single recording. */
    @Composable
    fun RecordItem(recordIndex: Int) {
        // Scope and state for loading the record.
        val scope = rememberCoroutineScope()
        var loading by remember { mutableStateOf(false) }

        // The record being shown.
        val record = MappingRecord.records[recordIndex]
        val description = recordDescription(record)

        // UI
        Box (
            modifier = Modifier
                .pointerInput(Unit) {
                    // Load a record by double-tapping.
                    detectTapGestures(
                        onDoubleTap = {
                            if(!loading) { // prevent loading more than one record at a time.
                                loading = true
                                scope.launch(Dispatchers.IO) {
                                    loadRecord(recordIndex)
                                    loading = false
                                }
                            }
                        }
                    )
                }
                .background(color= Color.LightGray, shape=itemShape)
                .padding(5.dp)
                .clip(itemShape)
        ) {
            Row {
                Column(modifier = Modifier.weight(0.8f).padding(end=10.dp)) {
                    Row(modifier = Modifier.padding(bottom = 5.dp).fillMaxSize()) {
                        val fSize = dimensionResource(R.dimen.small_text_size).value.sp
                        Text(
                            text = strftime(record.metadata.recordingPeriod.first(), "dd/MM/YYYY, HH:mm"),
                            fontSize = fSize,
                            fontWeight = FontWeight.Light,
                            modifier = Modifier.padding(end=10.dp)
                        )
                        Text(
                            text = record.location.name.replace('_', ' '),
                            fontSize = fSize,
                            fontWeight = FontWeight.Bold
                        )
                    }
                    Row {
                        Text(
                            text = description.replace('_', ' '),
                            fontSize = dimensionResource(R.dimen.extra_small_text_size).value.sp,
                            modifier = Modifier.padding(bottom = 5.dp)
                        )
                    }
                }
                Column(
                    modifier = Modifier.weight(0.2f),
                    horizontalAlignment = AbsoluteAlignment.Right
                ) {
                    record.field?.let {
                        Image(
                            bitmap = it.asImageBitmap(),
                            contentDescription = null,
                        )
                    }
                }
            }

            // Progress indicator for when the record is being loaded.
            if(!loading) return
            CircularProgressIndicator(
                modifier = Modifier.width(60.dp).align(Alignment.Center),
                color = Color.Blue,
                backgroundColor = Color.DarkGray,
            )
        }
    }

    /** Load the ith record.
     * @return If the record was loaded.
     * */
    private fun loadRecord(i: Int) {
        if (isLoading) return // We are already trying to load another recording.
        isLoading = true
        val model = ViewModelProvider(requireActivity()).get(RecorderModel::class.java)
        recordHasBeenLoaded.postValue(model.loadRecording(MappingRecord.records[i]))
        isLoading = false
    }

}
