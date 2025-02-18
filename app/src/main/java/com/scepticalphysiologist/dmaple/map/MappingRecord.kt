package com.scepticalphysiologist.dmaple.map

import com.scepticalphysiologist.dmaple.etc.strftime
import com.scepticalphysiologist.dmaple.etc.writeJSON
import com.scepticalphysiologist.dmaple.map.creator.MapCreator
import com.scepticalphysiologist.dmaple.map.creator.MapType
import mil.nga.tiff.FieldTagType
import mil.nga.tiff.TIFFImage
import mil.nga.tiff.TiffWriter
import java.io.File
import java.time.Instant

class MappingRecord(
    val name: String,
    val time: Instant,
    creators: List<MapCreator>
) {

    val struct: Map<MappingRoi, List<MapCreator>> = roiCreatorsMap(creators)

    companion object {

        fun read(folder: File) {



        }

    }

    fun write(root: File) {

        // Directory to save map
        val dir = File(root, name)
        if(!dir.exists()) dir.mkdir()

        // Metadata
        val metadata: MutableMap<String, Any> = mutableMapOf(
            "date-time" to strftime(time,"YY-MM-dd HH:mm:ss"),
        )

        // Maps
        // One TIFF image per ROI. One TIFF "directory"/"slice" per map.
        val roiMetadata = mutableMapOf<String, Any>()
        for((roi, roiCreators) in struct) {

            // Creators - TIFF slices and metadata.
            val img = TIFFImage()
            val mapMetadata = mutableListOf<Map<String, Any>>()
            for(creator in roiCreators) {
                val creatorName = MapType.getMapType(creator).title
                for((mapName, tiffDir) in creator.tiffDirectory()) {
                    tiffDir.setStringEntryValue(FieldTagType.ImageDescription, mapName)
                    img.add(tiffDir)
                    mapMetadata.add(mapOf(
                        "type" to creatorName,
                        "map#" to mapName
                    ))
                }
            }

            // TIFF image
            TiffWriter.writeTiff(File(dir, "${roi.uid}.tiff"), img)
            roiMetadata[roi.uid] = mapOf("maps" to mapMetadata)
        }

        // Write metadata.
        metadata["rois"] = roiMetadata
        writeJSON(File(dir, "mapping_metadata.json"), metadata)
    }


}


/** Map ROIs (in the camera's frame) to the creators associated with them. */
private fun roiCreatorsMap(creators: List<MapCreator>): Map<MappingRoi, List<MapCreator>> {
    val uids = creators.map{it.roi.uid}.toSet()
    val mp = mutableMapOf<MappingRoi, List<MapCreator>>()
    for(uid in uids) {
        val roi = creators.first { it.roi.uid == uid }.roi
        mp[roi] = creators.filter { it.roi.uid == uid }
    }
    return mp
}