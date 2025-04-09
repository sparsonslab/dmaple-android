package com.scepticalphysiologist.dmaple.map.creator

/** Parameters used to define map creation globally for the whole field, rather than for
 * specific [FieldRoi]s. */
data class FieldParams(
    var gutsAreAboveThreshold: Boolean = true,
    /** The minimum pixel width of the gut at its seeding edge. */
    var minWidth: Int = 10,
    /** The maximum pixel gap (below threshold region) for thresholding. */
    var maxGap: Int = 2,
    /** Pixels to skip along the spine (reduce the map spatial resolution). */
    var spineSkipPixels: Int = 0,
    /** Pixels width to smooth the spine (required for radius mapping). */
    var spineSmoothPixels: Int = 1
) {

    companion object {
        /** The mapping parameters from the preferences (settings) page. */
        val preference = FieldParams()
    }

}
