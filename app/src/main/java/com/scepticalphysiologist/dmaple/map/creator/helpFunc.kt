package com.scepticalphysiologist.dmaple.map.creator

import android.graphics.Bitmap
import com.scepticalphysiologist.dmaple.etc.ntscGrey
import com.scepticalphysiologist.dmaple.map.field.FieldRoi





fun pointsAlong(range: Pair<Float, Float>): IntArray {
    if (range.first < range.second)
        return (range.first.toInt()..range.second.toInt()).toList().toIntArray()
    return (range.first.toInt() downTo range.second.toInt()).toList().toIntArray()
}



fun findEdge(
    bitmap: Bitmap,
    threshold: Int,
    findAbove: Boolean,
    i: Int, j: Int,
    scanVertical: Boolean,
    increment: Boolean
): Int {

    val d = if(increment) 1 else -1

    if(findAbove && scanVertical) {
        var y = j
        while((y > 0) && (y < bitmap.height - 1) && (ntscGrey(bitmap.getPixel(i, y)) < threshold)) y += d
        return y
    }
    else if(!findAbove && scanVertical) {
        var y = j
        while((y > 0) && (y < bitmap.height - 1) && (ntscGrey(bitmap.getPixel(i, y)) > threshold)) y += d
        return y
    }
    else if(findAbove) {
        var x = i
        while((x > 0) && (x < bitmap.width - 1) && (ntscGrey(bitmap.getPixel(x, j)) < threshold)) x += d
        return x
    }
    else  {
        var x = i
        while((x > 0) && (x < bitmap.width - 1) && (ntscGrey(bitmap.getPixel(x, j)) > threshold)) x += d
        return x
    }

}


fun scanAcross(
    bitmap: Bitmap,
    roi: FieldRoi,

) {

}



