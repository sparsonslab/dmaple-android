package com.scepticalphysiologist.dmaple.map

import com.google.gson.Gson
import com.google.gson.JsonSyntaxException
import com.scepticalphysiologist.dmaple.map.creator.MapCreator
import mil.nga.tiff.TIFFImage
import mil.nga.tiff.TiffWriter
import java.io.File

class MappingRecord(
    val name: String,
    val struct: Map<MappingRoi, List<MapCreator>>
) {

    companion object {

        fun read(folder: File): MappingRecord? {

            if(!folder.exists() || !folder.isDirectory) return null

            // ROI files.
            val roiFiles = folder.listFiles()?.filter{it.name.endsWith(".json") } ?: listOf()
            if(roiFiles.isEmpty()) return null

            val struct = mutableMapOf<MappingRoi, List<MapCreator>>()
            for(roiFile in roiFiles) {
                // ROI: deserialize JSON
                val roi = try { Gson().fromJson(roiFile.readText(), MappingRoi::class.java) }
                catch (_: JsonSyntaxException) { null }
                if(roi == null) continue

                // Maps: creators
                val mapsFile = File(folder, "${roi.uid}.tiff")
                if(!mapsFile.exists()) continue
                struct[roi] = roi.maps.map{it.makeCreator(roi)}
            }

            return MappingRecord(name=folder.name, struct = struct)
        }

    }

    fun write(root: File) {
        // Directory to save map
        val dir = File(root, name)
        if(!dir.exists()) dir.mkdir()

        // ROIs and their maps.
        for((roi, roiCreators) in struct) {
            // ROI: serialize to JSON
            val roiFile = File(dir, "${roi.uid}.json")
            roiFile.writeText(Gson().toJson(roi))

            // Maps: to TIFF (one slice/directory per map)
            val img = TIFFImage()
            for(tiff in roiCreators.map{it.toTiff()}.filterNotNull().flatten()) img.add(tiff)
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
