package com.scepticalphysiologist.dmaple.map.creator

import android.graphics.Bitmap
import androidx.camera.core.ImageProxy
import androidx.core.graphics.blue
import androidx.core.graphics.green
import androidx.core.graphics.red
import java.nio.ByteBuffer

/** A wrapper for reading the luma ("luminance") from images.
 *
 * The luma is the same as the NTSC grey-scale value of an RGB image.
 *
 * https://en.wikipedia.org/wiki/Y%E2%80%B2UV
 */
class LumaReader {

    /** The width of the image. */
    var width: Int = 0
    /** The height of the image. */
    var height: Int = 0
    /** Buffer holding the luminance data. */
    private var buffer: ByteBuffer = ByteBuffer.allocate(1)
    /** Row stride into the buffer. */
    private var rs: Int = 0
    /** Pixel stride into the buffer. */
    private var ps: Int = 0
    /** A color bitmap of the first image read of the current dimensions. */
    var colorBitmap: Bitmap? = null

    /** Reset the reader. */
    fun reset() {
        width = 0
        height = 0
        buffer = ByteBuffer.allocate(0)
        rs = 0
        ps = 0
        colorBitmap = null
    }

    /** Read from a BT.470/PAL YUV formatted [ImageProxy]. */
    fun readYUVImage(proxy: ImageProxy) {
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
    fun readBitmap(bitmap: Bitmap) {
        if((bitmap.width != this.width) || (bitmap.height != this.height)) {
            this.width = bitmap.width
            this.height = bitmap.height
            buffer = ByteBuffer.allocate(width * height)
            rs = width
            ps = 1
            colorBitmap = null
        }
        if(colorBitmap == null) colorBitmap = bitmap

        for(j in 0 until height)
            for(i in 0 until width) {
                val color = bitmap.getPixel(i, j)
                val luma = 0.299f * color.red + 0.587f * color.green + 0.114f * color.blue
                buffer.put(j * width  + i, luma.toInt().toByte())
            }
    }

    /** Get the luma value (Y in YUV) of the (i, j)th pixel. */
    fun getPixelLuminance(i: Int, j: Int): Int {
        val k = (j * rs) + (i * ps)
        // Need for "and 0xff":
        // https://stackoverflow.com/questions/42097861/android-camera2-yuv-420-888-y-channel-interpretation
        return buffer.get(k).toInt() and 0xff
    }

}
