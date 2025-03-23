package com.scepticalphysiologist.dmaple.ui

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.material.Text
import androidx.compose.ui.platform.ComposeView
import androidx.compose.ui.unit.dp
import androidx.fragment.app.Fragment
import com.scepticalphysiologist.dmaple.map.record.MappingRecord


class Explorer: Fragment() {

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        val view = ComposeView(requireActivity())
        view.setContent {
            LazyVerticalGrid(columns = GridCells.Adaptive(minSize = 200.dp)) {
                items(200) { i ->
                    Text(text = "This is item $i")
                }
            }
        }
        return view
    }

}
