package com.scepticalphysiologist.dmaple.map.creator

import android.graphics.Bitmap
import com.scepticalphysiologist.dmaple.etc.ntscGrey
import com.scepticalphysiologist.dmaple.map.field.FieldRoi
import kotlin.math.ceil

/** Segments a gut in an camera field.
 *
 * Uses sim[ple threshold detection to identify the lower and upper bounds of the gut which can
 * then be used to calculate diameter, radius, etc.
 */
class GutSegmentor(roi: FieldRoi, val params: FieldParams) {

    // The gut and its field
    // ----------------------
    /** The field */
    private lateinit var image: LumaImage
    /** The gut is horizontal within the field of view. */
    val gutIsHorizontal: Boolean = roi.seedingEdge.isVertical()
    /** The grey-scale threshold at the gut boundary. */
    private val threshold: Float = roi.threshold.toFloat()

    private val transAxis: Pair<Int, Int>

    private val spineSmoothWin: Int = ceil(
        params.spineSmoothPixels.toFloat() / (params.spineSkipPixels + 1f)
    ).toInt()

    // Boundaries
    // ----------
    /** The pixel indices along the longitudinal axis of the gut. */
    val longIdx: IntArray
    /** The transverse-axis indices of the spine along the centre of the gut. */
    val spine: IntArray
    /** The transverse-axis indices of the smoothed spine along the centre of the gut.*/
    val spineSmoothed: IntArray
    /** The transverse-axis indices of the upper boundary of the gut. */
    val upper: IntArray
    /** The transverse-axis indices of the lower boundary of the gut. */
    val lower: IntArray

    // ---------------------------------------------------------------------------------------------
    // Initiation
    // ---------------------------------------------------------------------------------------------

    init {
        // Axes longitudinal and transverse to gut.
        fun pairFloatToInt(pair: Pair<Float, Float>): Pair<Int, Int> {
            return Pair(pair.first.toInt(), pair.second.toInt())
        }
        val (l0, l1) = pairFloatToInt(roi.longitudinalAxis())
        transAxis = pairFloatToInt(roi.transverseAxis())

        // Longitudinal samples.
        val step = params.spineSkipPixels + 1
        longIdx = if(l0 < l1) (l0..l1 step step).toList().toIntArray()
        else (l1..l0 step step).toList().toIntArray()
        val nl = longIdx.size

        // Holding arrays.
        spine = IntArray(nl)
        spineSmoothed = IntArray(nl)
        upper = IntArray(nl)
        lower = IntArray(nl)
    }

    /** Set the current field frame/image to be analysed. */
    fun setFieldImage(image: LumaImage) { this.image = image }

    /** Get the NTSC grey-scale value of the (i, j) bitmap pixel. */
    private fun getPixel(i: Int, j: Int): Float { return image.getPixel(i, j).toFloat() }

    /** Seed the gut an initiate (seed) its spine.
     *
     * @param rTrans The transverse-axis index range, over which to detect the gut.
     * @return If a gut was detected.
     * */
    fun detectGutAndSeedSpine(): Boolean {
        val gut = findWidestGut(longIdx.first(), transAxis) ?: return false
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
        val rTransChecked = conformTransRange(rTrans)
        val section = transverseSection(iLong, rTransChecked)
        val guts = mutableListOf<Pair<Int, Int>>()
        var w = 0
        var g = 0
        val t0 = rTransChecked.first
        val n = section.size - 1
        for((i, v) in section.withIndex()) {
            if((i < n) && (v || ((w > 0) && (g < params.maxGap)))) {
                w += 1
                if(v) g = 0 else g += 1
            }
            else {
                if(w - g >= params.minWidth) guts.add(Pair(t0 + i - w, t0 + i - g - if(v) 0 else 1))
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
    private fun transverseSection(iLong: Int, rTrans: Pair<Int, Int>): BooleanArray {
        val range = (rTrans.first..rTrans.second)
        return (if(gutIsHorizontal)
                 range.map{ (getPixel(iLong, it) > threshold) xor !(params.gutsAreAboveThreshold) }
            else range.map{ (getPixel(it, iLong) > threshold) xor !(params.gutsAreAboveThreshold) }
        ).toBooleanArray()
    }

    /** Conform a transverse-index range to the actual transverse size of the  field. */
    private fun conformTransRange(rTrans: Pair<Int, Int>): Pair<Int, Int> {
        var (t0, t1) = rTrans
        if(t0 > t1) {
            t0 = rTrans.second
            t1 = rTrans.first
        }
        val tmax = if(gutIsHorizontal) image.height else image.width
        if(t0 !in 0 until tmax) t0 = 0
        if(t1 !in 0 until tmax) t1 = tmax - 1
        return Pair(t0, t1)
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
            spineSmoothed[i] = transIdx
        }
        if(spineSmoothWin > 1) {
            val w = spineSmoothWin / 2
            for(i in 0 until longIdx.size - spineSmoothWin) {
                spineSmoothed[i + w] = 0
                for (j in 0 until spineSmoothWin)
                    spineSmoothed[i + w] += spine[i + j]
                spineSmoothed[i + w] = spineSmoothed[i + w] / spineSmoothWin
            }
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
        val findAbove = params.gutsAreAboveThreshold == ofGut
        var i = iTrans
        var g = 0
        if(gutIsHorizontal){
            if(findAbove) while((i >= 0) && (i < image.height) && (g <= params.maxGap)) {
                if(getPixel(iLong, i) > threshold) g = 0 else g += 1
                i += d
            }
            else while((i >= 0) && (i < image.height) && (g <= params.maxGap)){
                if(getPixel(iLong, i) < threshold) g = 0 else g += 1
                i += d
            }
        }
        else {
            if (findAbove) while ((i >= 0) && (i < image.width) && (g <= params.maxGap)) {
                if (getPixel(i, iLong) > threshold) g = 0 else g += 1
                i += d
            }
            else while ((i >= 0) && (i < image.width) && (g <= params.maxGap)) {
                if (getPixel(i, iLong) < threshold) g = 0 else g += 1
                i += d
            }
        }
        val k = i - d * (g + 1)
        return if(goUp == (k > iTrans)) k else iTrans
    }

    // ---------------------------------------------------------------------------------------------
    // Values
    // ---------------------------------------------------------------------------------------------

    /** Get the diameter at the ith longitudinal index. */
    fun getDiameter(i: Int): Int { return 1 + upper[i] - lower[i] }

    /** Get the upper radius at the ith longitudinal index. */
    fun getUpperRadius(i: Int): Int { return 1 + upper[i] - spine[i] }

    /** Get the lower radius at the ith longitudinal index. */
    fun getLowerRadius(i: Int): Int { return spine[i] - lower[i] }

    /** Get the spine index at the ith longitudinal index. */
    fun getSpine(i: Int): Int { return spineSmoothed[i] }

}
