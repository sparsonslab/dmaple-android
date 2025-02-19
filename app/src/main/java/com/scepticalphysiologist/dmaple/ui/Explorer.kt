package com.scepticalphysiologist.dmaple.ui

import android.os.Environment
import androidx.recyclerview.widget.LinearLayoutManager
import com.scepticalphysiologist.dmaple.databinding.ExplorerBinding
import com.scepticalphysiologist.dmaple.map.record.MappingRecord
import com.scepticalphysiologist.dmaple.map.record.MappingRecordAdaptor


class Explorer: DMapLEPage<ExplorerBinding>(ExplorerBinding::inflate) {


    companion object {

        var records = mutableListOf<MappingRecord>()

    }

    override fun createUI() {

        // Update records
        val root = Environment.getExternalStoragePublicDirectory(Environment.DIRECTORY_DOCUMENTS)
        root.listFiles()?.let {
            records = it.map{MappingRecord.read(it)}.filterNotNull().toMutableList()
        }

        // Set recycler view properties, layout and adaptor.
        val recycler = binding.mappingRecords
        recycler.setHasFixedSize(true)
        recycler.setLayoutManager(LinearLayoutManager(binding.root.getContext()))
        val recordAdapter = MappingRecordAdaptor(this)
        recycler.setAdapter(recordAdapter)


    }


}