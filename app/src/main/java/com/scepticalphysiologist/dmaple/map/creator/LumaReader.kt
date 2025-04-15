package com.scepticalphysiologist.dmaple.map.creator

import android.graphics.Bitmap
import androidx.camera.core.ImageProxy

/** A wrapper for reading  the luminance ("luma" or Y channel) of a BT.470/PAL-standard YUV [ImageProxy].
 *
 *  The "luma" (luminance) is the same as the NTSC grey-scale value of an RGB image.
 *
 * https://en.wikipedia.org/wiki/Y%E2%80%B2UV
 */
open class LumaReader {

    /** The image proxy object. */
    private var proxy: ImageProxy? = null

    /** The width of the image. */
    var width: Int = 0

    /** The height of the image. */
    var height: Int = 0

    /** A color bitmap of an image from the proxy. */
    var colorBitmap: Bitmap? = null

    /** Set the current image. */
    open fun setYUVImage(proxy: ImageProxy) {
        this.proxy = proxy
        if((proxy.width != this.width) || (proxy.height != this.height)) {
            this.width = proxy.width
            this.height = proxy.height
            colorBitmap = null
        }
        if(colorBitmap == null) colorBitmap = proxy.toBitmap()
    }

    /** Reset the image. */
    fun reset() {
        proxy = null
        width = 0
        height = 0
        colorBitmap = null
    }

    /** Get the luma value (Y in YUV) of the (i, j)th pixel. */
    open fun getPixelLuminance(i: Int, j: Int): Int {
        proxy?.let { img ->
            val k = (j * img.planes[0].rowStride) + (i * img.planes[0].pixelStride)
            // Need for "and 0xff":
            // https://stackoverflow.com/questions/42097861/android-camera2-yuv-420-888-y-channel-interpretation
            return img.planes[0].buffer.get(k).toInt() and 0xff
        }
        return  0
    }

}
