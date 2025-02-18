package com.scepticalphysiologist.dmaple.map.creator

import android.graphics.Bitmap
import android.graphics.Rect
import android.os.Environment
import android.util.Size
import com.scepticalphysiologist.dmaple.etc.Point
import com.scepticalphysiologist.dmaple.etc.strftime
import com.scepticalphysiologist.dmaple.etc.writeJSON
import com.scepticalphysiologist.dmaple.map.MappingRoi
import mil.nga.tiff.FieldTagType
import mil.nga.tiff.FileDirectory
import mil.nga.tiff.TIFFImage
import mil.nga.tiff.TiffWriter
import java.io.File
import java.nio.ByteBuffer
import java.time.Instant
import kotlin.math.ceil


/** A class that handles the creation of a spatio-temporal map. */
abstract class MapCreator(val roi: MappingRoi, bufferProvider: (() -> ByteBuffer?)) {

    /** The current sample width (space) and height (time) of the map. */
    abstract fun spaceTimeSampleSize(): Size

    /** Update the map from a new camera bitmap. */
    abstract fun updateWithCameraBitmap(bitmap: Bitmap)

    /** Get the number of maps produced by the creator. */
    abstract fun nMaps(): Int

    /** Get a portion of the map as a bitmap.
     *
     * @param idx The index of the map (if the creator makes more than one map).
     * @param crop The space-time area (samples) of the map to return.
     * @param stepX The number of space samples to step when sampling the map.
     * i.e. the spatial down-sampling of the map.
     * @param stepY The number of time samples to step when sampling the map.
     * @param backing A backing array for the bitmap, into the map samples will be put.
     * */
    abstract fun getMapBitmap(
        idx: Int,
        crop: Rect?,
        stepX: Int = 1, stepY: Int = 1,
        backing: IntArray,
    ): Bitmap?

    /** Destroy the creator (freeing any resources) and if a file is provided, save the map. */
    abstract fun tiffDirectory(): Map<String, FileDirectory>

}

// -------------------------------------------------------------------------------------------------
// Useful functions
// -------------------------------------------------------------------------------------------------

fun rangeSize(range: Int, step: Int): Int {
    return ceil(range.toFloat() / step.toFloat()).toInt()
}

fun orderedX(pp: Pair<Point, Point>): Pair<Int, Int> {
    val p0 = pp.first.x.toInt()
    val p1 = pp.second.x.toInt()
    return Pair(minOf(p0, p1), maxOf(p0, p1))
}

fun orderedY(pp: Pair<Point, Point>): Pair<Int, Int> {
    val p0 = pp.first.y.toInt()
    val p1 = pp.second.y.toInt()
    return Pair(minOf(p0, p1), maxOf(p0, p1))
}


// -------------------------------------------------------------------------------------------------
// I/O
// -------------------------------------------------------------------------------------------------

fun saveCreatorsAndMaps(
    creators: List<MapCreator>,
    folderName: String = "",
    time: Instant = Instant.now()
) {

    // The root directory,
    val dtPrefix = strftime(time, "YYMMdd_HHmmss")
    val root = Environment.getExternalStoragePublicDirectory(Environment.DIRECTORY_DOCUMENTS)
    val dir = File(root, if(folderName.isEmpty()) dtPrefix else "${dtPrefix}_$folderName")
    if(!dir.exists()) dir.mkdir()

    // Metadata
    val metadata: MutableMap<String, Any> = mutableMapOf(
        "date-time" to strftime(time,"YY-MM-dd HH:mm:ss"),
    )

    // Maps
    // One TIFF image per ROI. One TIFF "directory"/"slice" per map.
    val roiMetadata = mutableMapOf<String, Any>()
    for((roi, roiCreators) in roiCreatorsMap(creators)) {

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
        roiMetadata[roi.uid] = mapOf(
            "maps" to mapMetadata,

        )
    }

    // Write metadata.
    metadata["rois"] = roiMetadata
    writeJSON(File(dir, "mapping_metadata.json"), metadata)

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

