// Copyright (c) 2025-2025. Dr Sean Paul Parsons. All rights reserved.

package com.scepticalphysiologist.dmaple.map.record

import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.os.Environment
import com.scepticalphysiologist.dmaple.map.FrameTimer
import com.scepticalphysiologist.dmaple.map.buffer.MapBufferProvider
import com.scepticalphysiologist.dmaple.map.creator.MapCreator
import com.scepticalphysiologist.dmaple.map.field.FieldRuler
import mil.nga.tiff.FieldTagType
import mil.nga.tiff.TIFFImage
import mil.nga.tiff.TiffWriter
import java.io.File
import java.io.FileOutputStream
import java.time.Duration
import java.time.Instant

/** Data from a recording that can be written to file and then later read back to recreate the
 * recording conditions - the mapping ROIs.
 *
 */
class MappingRecord(
    /** The directory containing all the data from a recording. */
    val location: File,
    /** An image of the mapping field (i.e. a camera frame). */
    val field: Bitmap?,
    /** The field ruler. */
    val ruler: FieldRuler?,
    /** The frame rate timer. */
    val timer: FrameTimer?,
    /** Map creators. */
    val creators: List<MapCreator>,
    /** Metadata for serialisation. */
    val metadata: RecordMetadata =  RecordMetadata(
        recordingPeriod = timer?.recordingPeriod() ?: listOf(Instant.now(), Instant.now()),
        rois = creators.map{it.roi},
        ruler = ruler,
        params = creators[0].params
    )
) {

    companion object {

        // Recordings
        // ----------
        /** The default root folder for mapping record folders.*/
        val DEFAULT_ROOT: File = Environment.getExternalStoragePublicDirectory(Environment.DIRECTORY_DOCUMENTS)
        /** The actual root folder from which records have been loaded.*/
        var root: File = DEFAULT_ROOT
        /** The default name for mapping record folders. */
        const val DEFAULT_RECORD_FOLDER: String = "Maps"
        /** All the loaded mapping records. */
        val records = mutableListOf<MappingRecord>()

        // File names
        // ----------
        /** The file name for the field image. */
        const val FIELD_FILE = "field.jpg"
        /** The file name for the recording metadata. */
        const val METADATA_FILE = "metadata.json"

        const val TIMING_FILE = "timing.txt"

        /** Load all records. */
        fun loadRecords(root: File = DEFAULT_ROOT) {
            if(records.isNotEmpty()) return // Don't load twice during the lifetime of the app.
            root.listFiles()?.filter{it.isDirectory}?.map { folder ->
                read(folder)?.let{ record -> records.add(record) }
            }
            records.sortByDescending { it.metadata.recordingPeriod[0] }
            this.root = root
        }

        /** All directories in the records root directory. */
        fun allDirectoriesInRoot(): List<File> {
            return root.listFiles()?.filter { it.isDirectory } ?: listOf()
        }

        /** Read a record from the [location] folder.*/
        fun read(location: File): MappingRecord? {
            if(!location.exists() || !location.isDirectory) return null

            // Metadata.
            val metadata = RecordMetadata.deserialize(File(location, METADATA_FILE)) ?: return null

            // Timer.
            val timer = FrameTimer.read(File(location, TIMING_FILE))

            // Recreate creators from metadata.
            val creators = metadata.rois.map { roi -> MapCreator(roi, metadata.params) }

            // Field of view
            val fieldFile = File(location, FIELD_FILE)
            val field = if(fieldFile.exists()) BitmapFactory.decodeFile(fieldFile.absolutePath) else null

            // Record
            return MappingRecord(
                location = location,
                field = field,
                ruler = metadata.ruler,
                creators = creators,
                metadata = metadata,
                timer = timer
            )
        }

    }

    /** The recording duration of the record. */
    fun duration(): Duration {
        if(metadata.recordingPeriod.size > 1)
            return Duration.between(metadata.recordingPeriod[0], metadata.recordingPeriod[1])
        return timer?.recordingDuration() ?: Duration.ZERO
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

        try {
            // Metadata.
            metadata.serialise(File(location, METADATA_FILE))

            // Timer.
            timer?.write(File(location, TIMING_FILE))

            // Field bitmap
            field?.compress(Bitmap.CompressFormat.JPEG, 90, FileOutputStream(File(location, FIELD_FILE)))

            // Maps to separate TIFF images.
            // Considered making each map a slice/directory of a single TIFF file and
            // though this works, many third-party readers (e.g. ImageJ) cannot read multiple
            // directories with different pixel types (e.g. a mix of short and RBG).
            for(creator in creators) {
                for(tiff in creator.toTiff()) {
                    val img = TIFFImage().also{it.add(tiff)}
                    val des = tiff.getStringEntryValue(FieldTagType.ImageUniqueID)
                    TiffWriter.writeTiff(File(location, "${creator.roi.uid}_$des.tiff"), img)
                }
            }
        }
        // No permission to write.
        catch(_: java.io.FileNotFoundException){}
    }

}
