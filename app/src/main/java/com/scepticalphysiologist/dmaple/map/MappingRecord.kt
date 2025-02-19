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
                try {
                    val roi = Gson().fromJson(roiFile.readText(), MappingRoi::class.java)
                    val roiName = roiFile.name.slice(0 until roiFile.name.length - 5)
                    println("${roi.uid}")

                    val mapsFile = File(folder, "${roiName}.tiff")
                    if(!mapsFile.exists()) {
                        struct[roi] = listOf()
                        continue
                    }


                    struct[roi] = listOf()

                } catch(_: JsonSyntaxException) {
                    continue
                }
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
            // JSON serialized ROI
            val roiFile = File(dir, "${roi.uid}.json")
            roiFile.writeText(Gson().toJson(roi))

            // TIFF of maps (one slice/directory per map)
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