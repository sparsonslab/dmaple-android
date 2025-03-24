package com.scepticalphysiologist.dmaple.ui

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.material.CircularProgressIndicator
import androidx.compose.material.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.platform.ComposeView
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.core.os.bundleOf
import androidx.fragment.app.Fragment
import androidx.lifecycle.MutableLiveData
import androidx.navigation.fragment.findNavController
import com.scepticalphysiologist.dmaple.MainActivity
import com.scepticalphysiologist.dmaple.R
import com.scepticalphysiologist.dmaple.map.record.MappingRecord
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch

/** Explore saved records and load them.
 *
 */
class Explorer: Fragment() {

    /** When a record has been loaded. */
    private val recordHasBeenLoaded = MutableLiveData<Boolean?>(null)

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {

        // Once a record has been loaded, navigate to the recording fragment to show the recording.
        // This has to be done in response to a live data object, rather than within the composable's
        // on-click call-back because doing the latter blocks the progress indicator.
        recordHasBeenLoaded.observe(viewLifecycleOwner) { loaded ->
            loaded?.let{findNavController().navigate(R.id.recorder, bundleOf("LOADED" to loaded))}
        }

        // A lazy grid of recordings.
        val view = ComposeView(requireActivity())
        view.setContent {
            LazyVerticalGrid(
                columns = GridCells.Adaptive(minSize = 200.dp),
                verticalArrangement = Arrangement.spacedBy(20.dp),
                horizontalArrangement = Arrangement.spacedBy(20.dp),
                modifier = Modifier.padding(10.dp)
            ) { items(MappingRecord.records.size) { i -> RecordItem(i) } }
        }
        return view
    }

    /** A composable that shows a single recording. */
    @Composable
    fun RecordItem(recordIndex: Int) {
        // Scope and state for loading the record.
        val scope = rememberCoroutineScope()
        var loading by remember { mutableStateOf(false) }

        // The record being shown.
        val record = MappingRecord.records[recordIndex]
        val roiDescription = record.creators.joinToString("\n") { it.roi.maps.toString() }

        // UI
        // todo - block clicking on all items one one is clicked.
        Box (
            modifier = Modifier
                .clickable(
                    enabled = !loading,
                    onClick = {
                        loading = true
                        scope.launch(Dispatchers.Default) {
                            recordHasBeenLoaded.postValue(loadRecord(recordIndex))
                            loading = false
                        }
                    },
                )
                .background(color= Color.LightGray)
                .padding(5.dp)
        ) {
            // Label and description of the record and an image of the field.
            Column {
                Text(text = record.name, fontSize = 18.sp)
                Text(text = roiDescription, fontSize = 12.sp)
                record.field?.let { Image(bitmap = it.asImageBitmap(), contentDescription = null) }
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
    private suspend fun loadRecord(i: Int): Boolean{
        return MainActivity.mapService?.loadRecord(MappingRecord.records[i]) ?: false
    }

}
