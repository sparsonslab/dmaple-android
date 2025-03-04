package com.scepticalphysiologist.dmaple.map.creator

import android.graphics.Bitmap
import com.scepticalphysiologist.dmaple.etc.ntscGrey

/** Segments a gut in an camera field.
 *
 * Uses sim[ple threshold detection to identify the lower and upper bounds of the gut which can
 * then be used to calculate diameter, radius, etc.
 *
 * This abstract class contains all the processing algorithms and functions. Two concrete subclasses
 * are used for running on Android ([BitmapGutSegmentor]) and unit testing ([ArrayGutSegmentor]) as
 * the former uses android.graphics.Bitmap images which cannot be mocked easily in unit tests.
 */
abstract class GutSegmentor {

    // The gut and its field
    // ----------------------
    /** The gut is horizontal within the field of view. */
    var gutIsHorizontal: Boolean = true
    /** The pixel width of the field of view. */
    var fieldWidth: Int = 1
    /** The pixel height of the field of view. */
    var fieldHeight: Int = 1

    // Variables
    // ---------
    /** The grey-scale threshold at the gut boundary. */
    var threshold: Float = 1f
    /** The gut is above threshold (light against a dark background). */
    var gutIsAboveThreshold: Boolean = true
    /** The minimum pixel width of the gut at its seeding edge. */
    var minWidth: Int = 1
    /** The maximum pixel gap (below threshold region) for thresholding. */
    var maxGap: Int = 0

    // Boundaries
    // ----------
    /** The pixel indices along the longitudinal axis of the gut. */
    var longIdx: IntArray = IntArray(0)
    /** The transverse-axis indices of the spine along the centre of the gut. */
    var spine: IntArray = IntArray(0)
    /** The transverse-axis indices of the upper boundary of the gut. */
    var upper: IntArray = IntArray(0)
    /** The transverse-axis indices of the lower boundary of the gut. */
    var lower: IntArray = IntArray(0)

    // ---------------------------------------------------------------------------------------------
    // Initiation
    // ---------------------------------------------------------------------------------------------

    /** Set the longitudinal-axis indices of the gut.
     *
     * @param l0 The first index, at the seeding edge of the gut.
     * @param l1 The last index, at the opposite edge of the gut.
     * */
    fun setLongSection(l0: Int, l1: Int) {
        longIdx = if(l0 < l1) (l0..l1).toList().toIntArray() else (l1..l0).toList().toIntArray()
        val nl = longIdx.size
        spine = IntArray(nl)
        upper = IntArray(nl)
        lower = IntArray(nl)
    }

    /** Set the current field frame/image to be analysed. */
    abstract fun setFieldImage(image: Any)

    /** Get a pixel value from the current field image.*/
    abstract fun getPixel(i: Int, j: Int): Float

    /** Seed the gut an initiate (seed) its spine.
     *
     * @param rTrans The transverse-axis index range, over which to detect the gut.
     * @return If a gut was detected.
     * */
    fun detectGutAndSeedSpine(rTrans: Pair<Int, Int>): Boolean {
        val gut = findWidestGut(longIdx.first(), rTrans) ?: return false
        spine[0] = gut.first + (gut.second - gut.first) / 2
        updateBoundaries()
        return true
    }

    /** Find the widest gut along a transverse section.
     *
     * @param iLong The longitudinal-axis index at which the transverse section lies.
     * @param rTrans The transverse-axis index range, over which to detect the gut.
     * @return The transverse-axis indices at the lower and upper boundary of the gut or null
     * if not gut was found.
     * */
    fun findWidestGut(iLong: Int, rTrans: Pair<Int, Int>): Pair<Int, Int>? {
        val guts = findGuts(iLong, rTrans)
        if(guts.isEmpty()) return null
        return guts.maxBy { it.second - it.first }
    }

    /** Find all guts along a transverse section.
     *
     * @param iLong The longitudinal-axis index of the transverse section.
     * @param rTrans The transverse-axis index range, over which to detect the gut.
     * @return The transverse-axis indices at the lower and upper boundary of each gut found.
     * */
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

    /** Get the pixel values along a transverse section.
     *
     * @param iLong The longitudinal-axis index of the transverse section.
     * @param rTrans The transverse-axis index range, over which to get pixel values.
     * @return The pixel values along the section.
     * */
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

    /** Update the boundaries of the gut after a new filed has been set by [setFieldImage].*/
    fun updateBoundaries(){
        var transIdx = spine[0]
        for(i in longIdx.indices) {
            upper[i] = findEdge(longIdx[i], transIdx, goUp = true, ofGut = true)
            lower[i] = findEdge(longIdx[i], transIdx, goUp = false, ofGut = true)
            transIdx = lower[i] + (upper[i] - lower[i]) / 2
            spine[i] = transIdx
        }
    }

    /** Find a gut edge.
     *
     * @param iLong The longitudinal-axis index of the edge,
     * @param iTrans The transverse-axis index at which to start the edge search.
     * @param goUp Go up the transverse axis from [iTrans].
     * @param ofGut Find the edge of the gut from somewhere on the gut.
     * i.e. the ([iTrans], [iLong]) coordinate is assumed to lie on the gut.
     * @return The transverse-axis index at the edge of the gut.
     * */
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

    /** Get the diameter at the ith longitudinal index. */
    fun getDiameter(i: Int): Int { return upper[i] - lower[i] }

    /** Get the upper radius at the ith longitudinal index. */
    fun getUpperRadius(i: Int): Int { return upper[i] - spine[i] }

    /** Get the lower radius at the ith longitudinal index. */
    fun getLowerRadius(i: Int): Int { return spine[i] - lower[i] }

    /** Get the spine index at the ith longitudinal index. */
    fun getSpine(i: Int): Int { return spine[i] }
}

/** The gut segmentor used by the app. Uses a bitmap as the field image. */
class BitmapGutSegmentor: GutSegmentor() {

    private lateinit var bitmap: Bitmap

    override fun setFieldImage(image: Any) {
        bitmap = image as Bitmap
        fieldWidth = bitmap.width
        fieldHeight = bitmap.height
    }

    /** Get the NTSC grey-scale value of the (i, j) bitmap pixel. */
    override fun getPixel(i: Int, j: Int): Float { return ntscGrey(bitmap.getPixel(i, j)) }
}

/** The gut segmentor used for unit testing. Uses an array-of-arrays as the field image.
 * (i.e. a data-type that can be created on non-android JVM (unlike a bitmap).
 * */
class ArrayGutSegmentor: GutSegmentor() {

    private lateinit var array: Array<FloatArray>

    override fun setFieldImage(image: Any) {
        array = image as Array<FloatArray>
        fieldHeight = array.size
        fieldWidth = array[0].size
    }

    override fun getPixel(i: Int, j: Int): Float { return array[j][i] }
}
