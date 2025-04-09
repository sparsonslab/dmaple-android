package com.scepticalphysiologist.dmaple.map.record

import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.os.Environment
import com.google.gson.Gson
import com.google.gson.GsonBuilder
import com.google.gson.JsonSyntaxException
import com.scepticalphysiologist.dmaple.map.buffer.MapBufferProvider
import com.scepticalphysiologist.dmaple.map.field.FieldRoi
import com.scepticalphysiologist.dmaple.map.creator.MapCreator
import com.scepticalphysiologist.dmaple.map.creator.FieldParams
import mil.nga.tiff.FieldTagType
import mil.nga.tiff.TIFFImage
import mil.nga.tiff.TiffWriter
import java.io.File
import java.io.FileOutputStream
import java.nio.file.Files
import java.nio.file.attribute.BasicFileAttributes
import java.time.Instant

/** Input-output of a mapping recording.
 *
 */
class MappingRecord(
    /** The directory containing all the data from a recording. */
    val location: File,
    /** An image of the mapping field (i.e. a camera frame). */
    val field: Bitmap?,
    /** Map creators. */
    val creators: List<MapCreator>,
    /** Metadata. */
    val metadata: RecordMetadata,
) {

    companion object {

        // Recordings
        // ----------
        /** The default root folder for mapping record folders.*/
        val DEFAULT_ROOT: File = Environment.getExternalStoragePublicDirectory(Environment.DIRECTORY_DOCUMENTS)
        /** The default name for mapping record folders. */
        const val DEFAULT_RECORD_FOLDER: String = "Maps"
        /** All the loaded mapping records. */
        val records = mutableListOf<MappingRecord>()

        // File names
        // ----------
        /** The file name for the field image. */
        const val fieldFile = "field.jpg"
        /** The file name for the mapping parameters. */
        const val paramFile = "params.json"
        /** The file name for the recording metadata. */
        const val metadataFile = "metadata.json"
        /** GSON (de)serialization object for JSON files. */
        val gson: Gson = GsonBuilder().registerTypeAdapter(Instant::class.java, InstantTypeAdapter()).create()

        /** Load all records. */
        fun loadRecords(root: File = DEFAULT_ROOT) {
            if(records.isNotEmpty()) return // Don't load twice during the lifetime of the app.
            root.listFiles()?.filter{it.isDirectory}?.map { folder ->
                read(folder)?.let{ record -> records.add(record) }
            }
            records.sortByDescending { it.metadata.startTime }
        }

        /** Read a record from the [location] folder.*/
        fun read(location: File): MappingRecord? {
            if(!location.exists() || !location.isDirectory) return null

            fun <T> deserialize(file: File, cls: Class<T>): T? {
                if(!file.exists()) return null
                try { return gson.fromJson(file.readText(), cls) }
                catch (_: JsonSyntaxException) { }
                catch(_: java.io.FileNotFoundException) {} // Can happen from access-denied, when file is there.
                return null
            }
            // Field parameters.
            val params = deserialize(File(location, paramFile), FieldParams::class.java) ?: return null

            // Metadata.
            val fileAttrs = Files.readAttributes(location.toPath(), BasicFileAttributes::class.java)
            val fileCreationTime = fileAttrs.creationTime().toInstant()
            val metadata = deserialize(File(location, metadataFile), RecordMetadata::class.java) ?:
                           RecordMetadata(startTime = fileCreationTime, endTime = fileCreationTime)

            // Field of view
            var field: Bitmap?= null
            val fieldFile = File(location, fieldFile)
            if(fieldFile.exists()) field = BitmapFactory.decodeFile(fieldFile.absolutePath)

            // ROI JSON files.
            val roiFiles = location.listFiles()?.filter{
                it.name.endsWith(".json")  &&
                !it.name.endsWith(paramFile) &&
                !it.name.endsWith(metadataFile)
            } ?: listOf()
            if(roiFiles.isEmpty()) return null

            // Instantiate map creators from ROIs.
            val creators = mutableListOf<MapCreator>()
            for(roiFile in roiFiles) {
                val roi = deserialize(roiFile, FieldRoi::class.java) ?: continue
                creators.add(MapCreator(roi, params))
            }

            return MappingRecord(
                location = location,
                field = field,
                creators = creators,
                metadata = metadata
            )
        }

    }

    /** Once a record has been read, load the map TIFFs. */
    fun loadMapTiffs(bufferProvider: MapBufferProvider) {
        for(creator in creators) {
            creator.loadFromTiffs(location, bufferProvider)
            if(bufferProvider.nFreeBuffers() == 0) return
        }
    }

    /** Write the record to its [location] folder. */
    fun write() {
        if(creators.isEmpty()) return

        // Directory to save map
        if(!location.exists()) location.mkdir()

        // For each creator ....
        for(creator in creators) {
            // ... ROI: serialize to JSON
            val roiFile = File(location, "${creator.roi.uid}.json")
            roiFile.writeText(gson.toJson(creator.roi))

            // ... maps: to separate TIFF images.
            // Considered making each map a slice/directory of a single TIFF file and
            // though this works, many third-party readers (e.g. ImageJ) cannot read multiple
            // directories with different pixel types (e.g. a mix of short and RBG).
            for(tiff in creator.toTiff()) {
                val img = TIFFImage().also{it.add(tiff)}
                val des = tiff.getStringEntryValue(FieldTagType.ImageUniqueID)
                TiffWriter.writeTiff(File(location, "${creator.roi.uid}_$des.tiff"), img)
            }
        }

        // Parameters, metadata, field bitmap
        File(location, paramFile).writeText(gson.toJson(creators[0].params))
        File(location, metadataFile).writeText(gson.toJson(metadata))
        field?.compress(Bitmap.CompressFormat.JPEG, 90, FileOutputStream(File(location, fieldFile)))
    }

}
