package com.scepticalphysiologist.dmaple.map.creator

import android.graphics.Bitmap
import com.scepticalphysiologist.dmaple.etc.ntscGrey

abstract class MapFieldAnalyser {

    var threshold: Float = 1f
    var gutIsHorizontal: Boolean = true
    var gutIsAboveThreshold: Boolean = true
    var fieldWidth: Int = 1
    var fieldHeight: Int = 1

    abstract fun setImage(image: Any)

    abstract fun getPixel(i: Int, j: Int): Float

    fun findEdge(iLong: Int, iTrans: Int, goUp: Boolean, ofGut: Boolean=true): Int {

        val d = if(goUp) 1 else -1
        val findAbove = !gutIsAboveThreshold xor ofGut

        if(gutIsHorizontal){
            var y = iTrans
            if(findAbove) while((y >= 0) && (y < fieldHeight) && (getPixel(iLong, y) > threshold)) y += d
            else while((y >= 0) && (y < fieldHeight) && (getPixel(iLong, y) < threshold)) y += d
            return y - d
        }
        else {
            var x = iTrans
            if(findAbove) while((x >= 0) && (x < fieldWidth) && (getPixel(x, iLong)) > threshold) x += d
            else while((x >= 0) && (x < fieldWidth) && (getPixel(x, iLong) < threshold)) x += d
            return x - d
        }

    }


    fun transverseSection(iLong: Int, rTrans: Pair<Int, Int>): BooleanArray {
        if(gutIsHorizontal) {
            return (rTrans.first..rTrans.second).map{
                (getPixel(iLong, it) > threshold) xor !gutIsAboveThreshold
            }.toBooleanArray()
        }
        else {
            return (rTrans.first..rTrans.second).map{
                (getPixel(it, iLong) > threshold) xor !gutIsAboveThreshold
            }.toBooleanArray()
        }
    }


    fun findGut(iLong: Int, rTrans: Pair<Int, Int>, minWidth: Int = 5): Pair<Int, Int>? {
        val section = transverseSection(iLong, rTrans)
        val guts = mutableListOf<Pair<Int, Int>>()
        var w = 0
        for((i, v) in section.withIndex()) {
            if(v) w += 1
            else {
                if(w >= minWidth) guts.add(Pair(rTrans.first + i - w, rTrans.first + i - 1))
                w = 0
            }
        }
        if(guts.isEmpty()) return null
        return guts.maxBy { it.second - it.first }
    }

}


class BitmapFieldAnalyser: MapFieldAnalyser() {

    private lateinit var bitmap: Bitmap

    override fun setImage(image: Any) {
        bitmap = image as Bitmap
        fieldWidth = bitmap.width
        fieldHeight = bitmap.height
    }

    override fun getPixel(i: Int, j: Int): Float { return ntscGrey(bitmap.getPixel(i, j)) }

}


class ArrayFieldAnalyser: MapFieldAnalyser() {

    private lateinit var array: Array<FloatArray>

    override fun setImage(image: Any) {
        array = image as Array<FloatArray>
        fieldHeight = array.size
        fieldWidth = array[0].size
    }

    override fun getPixel(i: Int, j: Int): Float { return array[j][i] }

}





