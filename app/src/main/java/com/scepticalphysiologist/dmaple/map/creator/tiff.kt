// Copyright (c) 2025-2025. Dr Sean Paul Parsons. All rights reserved.

package com.scepticalphysiologist.dmaple.map.creator

import mil.nga.tiff.FieldTagType
import mil.nga.tiff.FileDirectory
import kotlin.math.ceil


/** Find a tiff directory with a matching identifier. */
fun findTiff(tiffs: List<FileDirectory>, identifier: String): FileDirectory? {
    return tiffs.filter{it.getStringEntryValue(FieldTagType.ImageUniqueID) == identifier}.firstOrNull()
}

/** Set the x and y resolutions of a tiff directory according to the ImageJ format.
 *
 * @param tiff The TIFF directory.
 * @param xr The x resolution (pixels/unit).
 * @param yr The y resolution (pixels/unit).
 * */
fun setResolution(tiff: FileDirectory, xr: Pair<Float, String>, yr: Pair<Float, String>) {
    tiff.setRationalEntryValue(FieldTagType.XResolution, floatToRational(xr.first))
    tiff.setRationalEntryValue(FieldTagType.YResolution, floatToRational(yr.first))
    val imagejDescription = listOf(
        "ImageJ=1.53k",
        "unit=${xr.second}", "yunit=${yr.second}", "zunit=-",
        "vunit=${xr.second}", "cf=0", "c0=0", "c1=${1f / xr.first}"
    ).joinToString("\n")
    tiff.setStringEntryValue(FieldTagType.ImageDescription, imagejDescription)
}

/** Get the x and y resolutions of a tiff directory. */
fun getResolution(tiff: FileDirectory): Pair<Pair<Float, String>, Pair<Float, String>> {
    try {
        val xr = rationalToFloat(tiff.getLongListEntryValue(FieldTagType.XResolution))
        val yr = rationalToFloat(tiff.getLongListEntryValue(FieldTagType.YResolution))
        var xu = ""
        var yu = ""
        val description = tiff.getStringEntryValue(FieldTagType.ImageDescription)
        for(line in description.split("\n")){
            val entry = line.split("=")
            if(entry.size != 2) continue
            when(entry[0]) {
                "unit" -> xu = entry[1]
                "yunit" -> yu = entry[1]
            }
        }
        return Pair(Pair(xr, xu), Pair(yr, yu))
    } catch(_: java.lang.ClassCastException) {}
    return Pair(Pair(1f, ""), Pair(1f, ""))
}

fun rangeSize(range: Int, step: Int): Int {
    return ceil(range.toFloat() / step.toFloat()).toInt()
}

/** Convert a float to a pair of numerator and denominator long integers. */
fun floatToRational(value: Float, denom: Long = 100_000): List<Long> {
    val num = (value * denom).toLong()
    return listOf(num, denom)
}

/** Convert a pair of numerator and denominator long integers to a float. */
fun rationalToFloat(ratio: List<Long>): Float {
    if(ratio.size < 2) return 1f
    return ratio[0].toFloat() / ratio[1].toFloat()
}
