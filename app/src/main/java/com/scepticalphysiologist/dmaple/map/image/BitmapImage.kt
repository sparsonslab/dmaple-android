package com.scepticalphysiologist.dmaple.map.image

import android.graphics.Bitmap
import com.scepticalphysiologist.dmaple.etc.ntscGrey

/** A greyscale object for unit testing. */
class BitmapImage(val bitmap: Bitmap): GreyScaleImage {

    override var width: Int = bitmap.width

    override var height: Int = bitmap.height

    override fun getPixel(i: Int, j: Int): Int { return ntscGrey(bitmap.getPixel(i, j)).toInt() }

}
