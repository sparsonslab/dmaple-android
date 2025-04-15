package com.scepticalphysiologist.dmaple.map.creator

import android.graphics.Bitmap
import androidx.camera.core.ImageProxy
import androidx.core.graphics.blue
import androidx.core.graphics.green
import androidx.core.graphics.red
import java.nio.ByteBuffer

/** A wrapper for reading  the luminance ("luma" or Y channel) of a
 * BT.470/PAL-standard YUV [ImageProxy].
 *
 *  The "luma" (luminance) is the same as the NTSC grey-scale value of an RGB image.
 *
 * https://en.wikipedia.org/wiki/Y%E2%80%B2UV
 */
open class LumaReader {

    /** The width of the image. */
    var width: Int = 0

    /** The height of the image. */
    var height: Int = 0

    /** A color bitmap of an image from the proxy. */
    var colorBitmap: Bitmap? = null

    private var buffer: ByteBuffer? = null

    private var rs: Int = 0

    private var ps: Int = 0

    /** Read from a BT.470/PAL YUV formatted [ImageProxy]. */
    open fun setYUVImage(proxy: ImageProxy) {
        buffer = proxy.planes[0].buffer
        rs = proxy.planes[0].rowStride
        ps = proxy.planes[0].pixelStride
        if((proxy.width != this.width) || (proxy.height != this.height)) {
            this.width = proxy.width
            this.height = proxy.height
            colorBitmap = null
        }
        if(colorBitmap == null) colorBitmap = proxy.toBitmap()
    }

    /** Read from a bitmap. */
    fun setBitmap(bitmap: Bitmap) {
        if((bitmap.width != this.width) || (bitmap.height != this.height)) {
            this.width = bitmap.width
            this.height = bitmap.height
            buffer = ByteBuffer.allocate(width * height)
            rs = width
            ps = 1
            colorBitmap = null
        }
        if(colorBitmap == null) colorBitmap = bitmap

        buffer?.let { buf ->
            for(j in 0 until height)
                for(i in 0 until width) {
                    val color = bitmap.getPixel(i, j)
                    val luma = 0.299f * color.red + 0.587f * color.green + 0.114f * color.blue
                    buf.put(j * width  + i, luma.toInt().toByte())
                }
        }
    }

    /** Reset the image. */
    fun reset() {
        width = 0
        height = 0
        colorBitmap = null
        buffer = null
        rs = 0
        ps = 0
    }

    /** Get the luma value (Y in YUV) of the (i, j)th pixel. */
    open fun getPixelLuminance(i: Int, j: Int): Int {
        buffer?.let { buf ->
            val k = (j * rs) + (i * ps)
            // Need for "and 0xff":
            // https://stackoverflow.com/questions/42097861/android-camera2-yuv-420-888-y-channel-interpretation
            return buf.get(k).toInt() and 0xff
        }
        return  0
    }

}
