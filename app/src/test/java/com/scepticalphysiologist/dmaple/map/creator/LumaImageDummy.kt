package com.scepticalphysiologist.dmaple.map.creator

import android.graphics.Bitmap
import androidx.camera.core.ImageProxy
import com.scepticalphysiologist.dmaple.etc.ntscGrey


/** For testing. */
class LumaImageDummy(bitmap: Bitmap): LumaReader() {

    init {
        colorBitmap = bitmap
        width = bitmap.width
        height = bitmap.height
    }

    override fun getPixelLuminance(i: Int, j: Int): Int {
        return ntscGrey(colorBitmap?.getPixel(i, j) ?: 0).toInt()
    }

    override fun setYUVImage(proxy: ImageProxy) {}

}
