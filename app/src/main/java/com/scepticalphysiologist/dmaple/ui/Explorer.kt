package com.scepticalphysiologist.dmaple.ui

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.material.Text
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.platform.ComposeView
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.core.os.bundleOf
import androidx.fragment.app.Fragment
import androidx.navigation.fragment.findNavController
import com.scepticalphysiologist.dmaple.R
import com.scepticalphysiologist.dmaple.map.record.MappingRecord


class Explorer: Fragment() {

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {

        val n = MappingRecord.records.size

        val view = ComposeView(requireActivity())
        view.setContent {
            LazyVerticalGrid(
                columns = GridCells.Adaptive(minSize = 200.dp),
                verticalArrangement = Arrangement.spacedBy(20.dp),
                horizontalArrangement = Arrangement.spacedBy(20.dp)
            ) {
                items(n) { i ->
                    val record = MappingRecord.records[i]
                    val roiDescription = record.creators.joinToString("\n") {
                        it.roi.maps.toString()
                    }

                    Column (
                        modifier = Modifier
                            .clickable(onClick = { openRecord(i) })
                            .background(color= Color.LightGray)
                    ) {
                        Text(text = record.name, fontSize = 18.sp)
                        Text(text = roiDescription, fontSize = 12.sp)
                        record.field?.let {
                            Image(bitmap = it.asImageBitmap(), contentDescription = null)
                        }
                    }
                }
            }
        }
        return view
    }


    private fun openRecord(i: Int) {
        println("click!!!")
        findNavController().navigate(R.id.recorder, bundleOf("recordIdx" to i))
        println("done!!!!")
    }


}
