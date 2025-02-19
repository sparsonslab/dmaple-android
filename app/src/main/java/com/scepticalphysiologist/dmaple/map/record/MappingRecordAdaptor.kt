package com.scepticalphysiologist.dmaple.map.record

import android.view.LayoutInflater
import android.view.ViewGroup
import androidx.fragment.app.Fragment
import androidx.recyclerview.widget.RecyclerView
import com.scepticalphysiologist.dmaple.databinding.MappingRecordHolderBinding
import com.scepticalphysiologist.dmaple.ui.Explorer

class MappingRecordAdaptor (
    private val fragment: Fragment,
): RecyclerView.Adapter<MappingRecordAdaptor.MappingRecordHolder>() {


    // ---------------------------------------------------------------------------------------------
    // Holder view.
    // ---------------------------------------------------------------------------------------------

    class MappingRecordHolder (
        val binding: MappingRecordHolderBinding
    ) : RecyclerView.ViewHolder (binding.root) {

    }

    // ---------------------------------------------------------------------------------------------
    // View creation.
    // ---------------------------------------------------------------------------------------------

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): MappingRecordHolder {
        val binding = MappingRecordHolderBinding.inflate(
            LayoutInflater.from(parent.context), parent, false
        )
        return MappingRecordHolder(binding)
    }

    override fun getItemCount(): Int { return Explorer.records.size }

    override fun onBindViewHolder(holder: MappingRecordHolder, position: Int) {

        // The test record to be inserted.
        val record = Explorer.records[position]

        // View header - record name and time.
        holder.binding.recordName.text = record.name
        val roiDescription = record.struct.keys.map{
            it.maps.toString()
        }.joinToString("\n")
        holder.binding.recordDescription.text = roiDescription


        holder.binding.root.setOnClickListener {
            openRecord(record)
        }

    }

    private fun openRecord(record: MappingRecord) {
        println("OPEN ${record.name}")
    }

}