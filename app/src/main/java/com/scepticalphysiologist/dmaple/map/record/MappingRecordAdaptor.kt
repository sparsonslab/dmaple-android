package com.scepticalphysiologist.dmaple.map.record

import android.view.LayoutInflater
import android.view.ViewGroup
import androidx.core.os.bundleOf
import androidx.fragment.app.Fragment
import androidx.navigation.fragment.findNavController
import androidx.recyclerview.widget.RecyclerView
import com.scepticalphysiologist.dmaple.R
import com.scepticalphysiologist.dmaple.databinding.MappingRecordHolderBinding
import com.scepticalphysiologist.dmaple.etc.rotateBitmap

class MappingRecordAdaptor (
    private val fragment: Fragment,
): RecyclerView.Adapter<MappingRecordAdaptor.MappingRecordHolder>() {


    // ---------------------------------------------------------------------------------------------
    // Holder view.
    // ---------------------------------------------------------------------------------------------

    class MappingRecordHolder (
        val binding: MappingRecordHolderBinding
    ) : RecyclerView.ViewHolder (binding.root) {}

    // ---------------------------------------------------------------------------------------------
    // View creation.
    // ---------------------------------------------------------------------------------------------

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): MappingRecordHolder {
        val binding = MappingRecordHolderBinding.inflate(
            LayoutInflater.from(parent.context), parent, false
        )
        return MappingRecordHolder(binding)
    }

    override fun getItemCount(): Int { return MappingRecord.records.size }

    override fun onBindViewHolder(holder: MappingRecordHolder, position: Int) {
        // The test record to be inserted.
        val idx = MappingRecord.records.size - position - 1
        val record = MappingRecord.records[idx]

        // Record name and description.
        holder.binding.recordName.text = record.name
        val roiDescription = record.struct.keys.map{
            it.maps.toString()
        }.joinToString("\n")
        holder.binding.recordDescription.text = roiDescription

        // Field image
        record.field?.let {
            val or = record.struct.keys.first().frame.orientation
            println("or = $or")
            holder.binding.recordImage.setImageBitmap(it)
        }

        // Touch to open.
        holder.binding.root.setOnClickListener { openRecord(idx) }
    }

    private fun openRecord(idx: Int) {
        fragment.findNavController().navigate(R.id.recorder, bundleOf("recordIdx" to idx))
    }

}
