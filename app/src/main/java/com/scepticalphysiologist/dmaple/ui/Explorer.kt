package com.scepticalphysiologist.dmaple.ui

import androidx.recyclerview.widget.LinearLayoutManager
import com.scepticalphysiologist.dmaple.databinding.ExplorerBinding
import com.scepticalphysiologist.dmaple.map.record.MappingRecordAdaptor


class Explorer: DMapLEPage<ExplorerBinding>(ExplorerBinding::inflate) {

    override fun createUI() {
        // Set recycler view properties, layout and adaptor.
        val recycler = binding.mappingRecords
        recycler.setHasFixedSize(true)
        recycler.setLayoutManager(LinearLayoutManager(binding.root.getContext()))
        val recordAdapter = MappingRecordAdaptor(this)
        recycler.setAdapter(recordAdapter)
    }

}
