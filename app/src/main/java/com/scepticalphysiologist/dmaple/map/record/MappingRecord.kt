package com.scepticalphysiologist.dmaple.map.record

import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.os.Environment
import com.google.gson.Gson
import com.google.gson.JsonSyntaxException
import com.scepticalphysiologist.dmaple.map.field.FieldRoi
import com.scepticalphysiologist.dmaple.map.creator.MapCreator
import mil.nga.tiff.FieldTagType
import mil.nga.tiff.FileDirectory
import mil.nga.tiff.TIFFImage
import mil.nga.tiff.TiffReader
import mil.nga.tiff.TiffWriter
import java.io.File
import java.io.FileOutputStream
import java.nio.ByteBuffer

/** Input-output of a mapping recording.
 *
 */
class MappingRecord(
    /** The directory containing all the data from a recording. */
    val location: File,
    /** An image of the mapping field (i.e. a camera frame). */
    val field: Bitmap?,
    /** Map creators. */
    val creators: List<MapCreator>
) {

    /** The name (folder name) of the record. */
    val name: String = location.name

    companion object {

        val records = mutableListOf<MappingRecord>()

        fun loadRecords() {
            val root = Environment.getExternalStoragePublicDirectory(Environment.DIRECTORY_DOCUMENTS)
            root.listFiles()?.let { files -> records.addAll(files.map{read(it)}.filterNotNull()) }
        }

        /** Read a record from the [location] folder.*/
        fun read(location: File): MappingRecord? {

            if(!location.exists() || !location.isDirectory) return null

            // ROI JSON files.
            val roiFiles = location.listFiles()?.filter{it.name.endsWith(".json") } ?: listOf()
            if(roiFiles.isEmpty()) return null

            val creators = mutableListOf<MapCreator>()
            for(roiFile in roiFiles) {
                // ROI: deserialize JSON
                val roi = try { Gson().fromJson(roiFile.readText(), FieldRoi::class.java) }
                catch (_: JsonSyntaxException) { null }
                if(roi == null) continue
                creators.add(MapCreator(roi))
            }

            // Field of view
            var field: Bitmap?= null
            val fieldFile = File(location, "field.jpg")
            if(fieldFile.exists()) field = BitmapFactory.decodeFile(fieldFile.absolutePath)

            return MappingRecord(location, field, creators)
        }

    }

    /** Once a record has been read, load the map TIFFs. */
    fun loadMapTiffs(bufferProvider: (() -> ByteBuffer?)) {

        val tiffFiles = location.listFiles()?.filter { it.name.endsWith(".tiff") } ?: return

        for(creator in creators) {
            val dirs = mutableListOf<FileDirectory>()
            for(file in tiffFiles) {
                if(!file.name.startsWith(creator.roi.uid)) continue
                dirs.addAll(TiffReader.readTiff(file).fileDirectories)
            }
            val buffers = (0 until creator.nMaps).map{bufferProvider.invoke()}.filterNotNull()
            if(buffers.size < creator.nMaps) return
            creator.provideBuffers(buffers)
            creator.fromTiff(dirs)
        }
    }

    /** Write the record to its [location] folder. */
    fun write() {
        // Directory to save map
        if(!location.exists()) location.mkdir()

        // ROIs and their maps.
        for(creator in creators) {
            // ROI: serialize to JSON
            val roiFile = File(location, "${creator.roi.uid}.json")
            roiFile.writeText(Gson().toJson(creator.roi))
            // Maps: to TIFF (one slice/directory per map)
            for(tiff in creator.toTiff()) {
                val img = TIFFImage().also{it.add(tiff)}
                val des = tiff.getStringEntryValue(FieldTagType.ImageDescription)
                TiffWriter.writeTiff(File(location, "${creator.roi.uid}_$des.tiff"), img)
            }
        }

        // Mapping field.
        field?.compress(
            Bitmap.CompressFormat.JPEG, 90,
            FileOutputStream(File(location, "field.jpg"))
        )
    }

}

