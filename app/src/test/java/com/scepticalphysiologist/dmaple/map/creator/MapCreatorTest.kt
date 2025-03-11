package com.scepticalphysiologist.dmaple.map.creator


import android.app.Application
import android.graphics.Bitmap
import com.scepticalphysiologist.dmaple.etc.Edge
import com.scepticalphysiologist.dmaple.etc.Frame
import com.scepticalphysiologist.dmaple.etc.Point
import com.scepticalphysiologist.dmaple.map.field.FieldRoi


import org.junit.Test

import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config
import org.robolectric.shadows.ShadowBitmap

import java.nio.ByteBuffer




@RunWith(RobolectricTestRunner::class)
@Config(shadows = [ShadowBitmap::class])
class MapCreatorTest {

    @Config(shadows = [ShadowBitmap::class])
    @Test
    fun `mock x`() {



        val bm = Bitmap.createBitmap(200, 100, Bitmap.Config.ARGB_8888)
        bm.setPixel(100, 50, 8)

        dosomething(bm)

    }


    fun dosomething(bm: Bitmap) {
        println("pixel = ${bm.getPixel(100, 50)}")
    }



    @Test
    fun `reach end condition reached`() {

        val frames = horizontalGutSeries(
            diameters = listOf(45, 50, 55, 50, 50, 45),
            fieldWidth = 100
        )
        val (iw, ih) = imageSize(frames[0])


        val roi = FieldRoi(
            frame = Frame(size = Point(iw.toFloat(),  ih.toFloat()), orientation = 0),
            threshold = 50,
            seedingEdge = Edge.RIGHT,
            maps = listOf(MapType.RADIUS),
            uid = "abcdefgh0123456789"
        )
        roi.left = 10f
        roi.right = iw.toFloat() - 10f
        roi.top = 10f
        roi.bottom = ih.toFloat() - 10f




        val creator = MapCreator(roi)
        val bytesRequired = frames.size * roi.width().toInt() * roi.maps.sumOf { it.bytesPerSample }
        val buffers = (0..2).map{ ByteBuffer.allocate(bytesRequired - 10) }
        creator.provideBuffers(buffers)





    }


}

fun horizontalGutSeries(
    diameters: List<Int>,
    fieldWidth: Int,
): List<Array<FloatArray>> {
    val fieldHeight = diameters.max() + 50
    val cent = fieldHeight / 2
    return diameters.map{ diam ->
        createImage(fieldWidth, fieldHeight, 0f).also { image ->
            for(i in 0 until fieldWidth) paintSlice(
                image, i, cent, diam,100f, true
            )
        }
    }
}