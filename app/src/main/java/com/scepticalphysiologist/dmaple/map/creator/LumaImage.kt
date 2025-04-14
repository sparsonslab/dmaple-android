package com.scepticalphysiologist.dmaple.map.creator

import android.graphics.Bitmap
import androidx.camera.core.ImageProxy


class LumaImage() {


    var proxy: ImageProxy? = null

    var width: Int = 0

    var height: Int = 0

    var bitmap: Bitmap? = null


    fun setImage(proxy: ImageProxy) {
        this.proxy = proxy
        if((proxy.width != this.width) || (proxy.height != this.height)) {
            this.width = proxy.width
            this.height = proxy.height
            bitmap = proxy.toBitmap()
        }
    }


    private fun getSample(k: Int): Int {
        // Need for "and 0xff":
        // https://stackoverflow.com/questions/42097861/android-camera2-yuv-420-888-y-channel-interpretation
        return proxy?.let{it.planes[0].buffer.get(k).toInt() and 0xff} ?: 0
    }

    fun getPixel(i: Int, j: Int): Int {
        return getSample(j * width + i)
    }

    fun getFirstBitmap(): Bitmap? { return bitmap }

}