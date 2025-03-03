package com.scepticalphysiologist.dmaple.map.creator

import android.graphics.Bitmap
import com.scepticalphysiologist.dmaple.etc.ntscGrey

abstract class MapFieldAnalyser {

    var threshold: Float = 1f
    var gutIsHorizontal: Boolean = true
    var gutIsAboveThreshold: Boolean = true
    var fieldWidth: Int = 1
    var fieldHeight: Int = 1

    var minWidth: Int = 1
    var maxGap: Int = 0

    var longIdx: IntArray = IntArray(0)
    var spine: IntArray = IntArray(0)
    var upper: IntArray = IntArray(0)
    var lower: IntArray = IntArray(0)

    abstract fun setImage(image: Any)

    abstract fun getPixel(i: Int, j: Int): Float

    // ---------------------------------------------------------------------------------------------
    // Initiation
    // ---------------------------------------------------------------------------------------------

    fun setLongSection(l0: Int, l1: Int) {
        longIdx = if(l0 < l1) (l0..l1).toList().toIntArray() else (l1..l0).toList().toIntArray()
        val nl = longIdx.size
        spine = IntArray(nl)
        upper = IntArray(nl)
        lower = IntArray(nl)
    }

    fun seedSpine(t0: Int, t1: Int): Boolean {
        val gut = findWidestGut(longIdx.first(), Pair(t0, t1)) ?: return false
        spine[0] = gut.first + (gut.second - gut.first) / 2
        updateSpine()
        return true
    }

    fun findWidestGut(iLong: Int, rTrans: Pair<Int, Int>): Pair<Int, Int>? {
        val guts = findGuts(iLong, rTrans)
        if(guts.isEmpty()) return null
        return guts.maxBy { it.second - it.first }
    }

    fun findGuts(iLong: Int, rTrans: Pair<Int, Int>): List<Pair<Int, Int>> {
        val section = transverseSection(iLong, rTrans)
        val guts = mutableListOf<Pair<Int, Int>>()
        var w = 0
        var g = 0
        for((i, v) in section.withIndex()) {
            if(v || ((w > 0) && (g < maxGap))) {
                w += 1
                if(!v) g += 1 else g = 0
            }
            else {
                if(w >= minWidth) guts.add(Pair(rTrans.first + i - w, rTrans.first + i - g - 1))
                g = 0
                w = 0
            }
        }
        return guts
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

    // ---------------------------------------------------------------------------------------------
    // Update
    // ---------------------------------------------------------------------------------------------

    fun updateSpine(){
        var transIdx = spine[0]
        for(i in longIdx.indices) {
            upper[i] = findEdge(longIdx[i], transIdx, goUp = true, ofGut = true)
            lower[i] = findEdge(longIdx[i], transIdx, goUp = false, ofGut = true)
            transIdx = lower[i] + (upper[i] - lower[i]) / 2
            spine[i] = transIdx
        }
    }

    private fun findEdge(iLong: Int, iTrans: Int, goUp: Boolean, ofGut: Boolean=true): Int {
        val d = if(goUp) 1 else -1
        val findAbove = !gutIsAboveThreshold xor ofGut
        var i = iTrans
        var g = 0
        if(gutIsHorizontal){
            if(findAbove) while((i >= 0) && (i < fieldHeight) && (g < maxGap)) {
                if(getPixel(iLong, i) > threshold) g = 0 else g += 1
                i += d
            }
            else while((i >= 0) && (i < fieldHeight) && (g < maxGap)){
                if(getPixel(iLong, i) < threshold) g = 0 else g += 1
                i += d
            }
        }
        else {
            if(findAbove) while((i >= 0) && (i < fieldHeight) && (g < maxGap)) {
                if(getPixel(i, iLong) > threshold) g = 0 else g += 1
                i += d
            }
            else while((i >= 0) && (i < fieldHeight) && (g < maxGap)){
                if(getPixel(i, iLong) < threshold) g = 0 else g += 1
                i += d
            }
        }
        return i - d * g
    }

    // ---------------------------------------------------------------------------------------------
    // Values
    // ---------------------------------------------------------------------------------------------

    fun getDiameter(i: Int): Int { return upper[i] - lower[i] }

    fun getUpperRadius(i: Int): Int { return upper[i] - spine[i] }

    fun getLowerRadius(i: Int): Int { return spine[i] - lower[i] }

    fun getSpine(i: Int): Int { return spine[i] }

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
