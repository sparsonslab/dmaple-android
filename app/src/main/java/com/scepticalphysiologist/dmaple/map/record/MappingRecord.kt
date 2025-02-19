package com.scepticalphysiologist.dmaple.map.record

import com.google.gson.Gson
import com.google.gson.JsonSyntaxException
import com.scepticalphysiologist.dmaple.map.MappingRoi
import com.scepticalphysiologist.dmaple.map.creator.MapCreator
import mil.nga.tiff.TIFFImage
import mil.nga.tiff.TiffReader
import mil.nga.tiff.TiffWriter
import java.io.File
import java.nio.ByteBuffer

class MappingRecord(
    val location: File,
    val struct: Map<MappingRoi, List<MapCreator>>
) {

    val name = location.name

    companion object {

        fun read(location: File): MappingRecord? {

            if(!location.exists() || !location.isDirectory) return null

            // ROI files.
            val roiFiles = location.listFiles()?.filter{it.name.endsWith(".json") } ?: listOf()
            if(roiFiles.isEmpty()) return null

            val struct = mutableMapOf<MappingRoi, List<MapCreator>>()
            for(roiFile in roiFiles) {
                // ROI: deserialize JSON
                val roi = try { Gson().fromJson(roiFile.readText(), MappingRoi::class.java) }
                catch (_: JsonSyntaxException) { null }
                if(roi == null) continue

                // Maps: creators
                val mapsFile = File(location, "${roi.uid}.tiff")
                if(!mapsFile.exists()) continue
                struct[roi] = roi.maps.map{it.makeCreator(roi)}
            }

            return MappingRecord(location, struct)
        }

    }

    fun readMapTiffs(bufferProvider: (() -> ByteBuffer?)) {
        for((roi, creators) in struct) {
            val mapsFile = File(location, "${roi.uid}.tiff")
            if(!mapsFile.exists()) continue
            val dirs = TiffReader.readTiff(mapsFile).fileDirectories
            for(creator in creators) {
                val buffers = (0 until creator.nMaps).map{bufferProvider.invoke()}.filterNotNull()
                if(buffers.size < creator.nMaps) return
                creator.provideBuffers(buffers)
                creator.fromTiff(dirs)
            }
        }
    }

    fun write() {
        // Directory to save map
        if(!location.exists()) location.mkdir()

        // ROIs and their maps.
        for((roi, roiCreators) in struct) {
            // ROI: serialize to JSON
            val roiFile = File(location, "${roi.uid}.json")
            roiFile.writeText(Gson().toJson(roi))

            // Maps: to TIFF (one slice/directory per map)
            val img = TIFFImage()
            for(tiff in roiCreators.map{it.toTiff()}.filterNotNull().flatten()) img.add(tiff)
            TiffWriter.writeTiff(File(location, "${roi.uid}.tiff"), img)
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
