package com.scepticalphysiologist.dmaple.map.image

/** A greyscale image.
 *
 * This is only needed so that [BitmapImage] can be a test mock for [LumaImage].
 * */
interface GreyScaleImage {

    /** The width of the image. */
    var width: Int

    /** The height of the image. */
    var height: Int

    /** Get the (i,j) the greyscale pixel. */
    fun getPixel(i: Int, j: Int): Int

}
