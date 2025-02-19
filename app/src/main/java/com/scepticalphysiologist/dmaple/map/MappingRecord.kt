package com.scepticalphysiologist.dmaple.map

import com.scepticalphysiologist.dmaple.etc.readJSON
import com.scepticalphysiologist.dmaple.etc.strftime
import com.scepticalphysiologist.dmaple.etc.strptime
import com.scepticalphysiologist.dmaple.etc.writeJSON
import com.scepticalphysiologist.dmaple.map.creator.MapCreator
import mil.nga.tiff.TIFFImage
import mil.nga.tiff.TiffWriter
import java.io.File
import java.time.Instant

class MappingRecord(
    val name: String,
    val time: Instant,
    val struct: Map<MappingRoi, List<MapCreator>>
) {

    companion object {

        private const val metadataFileName = "mapping_metadata.json"

        private const val dtFormat = "YY-MM-dd HH:mm:ss"


        fun read(folder: File): MappingRecord? {

            // Metadata
            val metaDataFile = File(folder, metadataFileName)
            if(!metaDataFile.exists()) return null
            val metadata = readJSON(metaDataFile)
            val time = (metadata["date-time"] as? String)?.let{ strptime(it, dtFormat) } ?: return null


            // ROIs
            val rois = (metadata["rois"] as? Map<*, *>) ?: return null
            for((k, v) in rois) {
                val roiName = k as? String ?: continue

            }


            val name = folder.name


            return null
        }

    }

    fun write(root: File) {

        // Directory to save map
        val dir = File(root, name)
        if(!dir.exists()) dir.mkdir()

        // Maps
        // One TIFF image per ROI. One TIFF "directory"/"slice" per map.
        for((roi, roiCreators) in struct) {

            // ROI json
            // ??????

            // Map TIFFs
            val img = TIFFImage()
            for(tiff in roiCreators.map{it.tiffDirectories()}.filterNotNull().flatten()) img.add(tiff)
            TiffWriter.writeTiff(File(dir, "${roi.uid}.tiff"), img)
        }

    }


}


/** Map ROIs (in the camera's frame) to the creators associated with them. */
fun roiCreatorsMap(creators: List<MapCreator>): Map<MappingRoi, List<MapCreator>> {
    val uids = creators.map{it.roi.uid}.toSet()
    val mp = mutableMapOf<MappingRoi, List<MapCreator>>()
    for(uid in uids) {
        val roi = creators.first { it.roi.uid == uid }.roi
        mp[roi] = creators.filter { it.roi.uid == uid }
    }
    return mp
}