package com.scepticalphysiologist.dmaple.map.creator

import android.graphics.Bitmap
import android.graphics.Rect
import android.util.Size
import com.scepticalphysiologist.dmaple.etc.Point
import com.scepticalphysiologist.dmaple.map.MappingRoi
import mil.nga.tiff.FileDirectory
import java.nio.ByteBuffer
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
    abstract fun tiffDirectories(): List<FileDirectory>?

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
