package com.scepticalphysiologist.dmaple.map.creator

import android.graphics.Bitmap
import androidx.camera.core.ImageProxy

/** A wrapper for extracting the luminance ("luma" or Y channel) of a BT.470/PAL-standard YUV [ImageProxy].
 *
 *  The "luma" (luminance) is the same as the NTSC grey-scale value of an RGB image.
 *
 * https://en.wikipedia.org/wiki/Y%E2%80%B2UV
 */
class LumaImage {

    /** The image proxy object. */
    private var proxy: ImageProxy? = null

    /** The width of the image. */
    var width: Int = 0

    /** The height of the image. */
    var height: Int = 0

    /** A bitmap of the first image from the proxy. */
    var firstBitmap: Bitmap? = null

    /** Set the current image. */
    fun setImage(proxy: ImageProxy) {
        this.proxy = proxy
        if((proxy.width != this.width) || (proxy.height != this.height)) {
            this.width = proxy.width
            this.height = proxy.height
            firstBitmap = proxy.toBitmap()
        }
    }

    /** Get the luma value (Y in YUV) of the (i, j)th pixel. */
    fun getPixel(i: Int, j: Int): Int {
        val k = j * width + i
        // Need for "and 0xff":
        // https://stackoverflow.com/questions/42097861/android-camera2-yuv-420-888-y-channel-interpretation
        return proxy?.let{it.planes[0].buffer.get(k).toInt() and 0xff} ?: 0
    }

}
