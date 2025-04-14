package com.scepticalphysiologist.dmaple.map.creator

import android.graphics.Bitmap
import androidx.camera.core.ImageProxy
import com.scepticalphysiologist.dmaple.etc.ntscGrey


/** For testing. */
class LumaImageDummy(bitmap: Bitmap): LumaImage() {

    init {
        firstBitmap = bitmap
        width = bitmap.width
        height = bitmap.height
    }

    override fun getPixel(i: Int, j: Int): Int {
        return ntscGrey(firstBitmap?.getPixel(i, j) ?: 0).toInt()
    }

    override fun setImage(proxy: ImageProxy) {}

}
