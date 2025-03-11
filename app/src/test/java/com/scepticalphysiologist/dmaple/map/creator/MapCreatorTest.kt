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


    @Test
    fun `reach end condition reached`() {

        val frames = horizontalGutSeries(
            diameters = listOf(50, 55, 60, 65, 60, 55, 50),
            fieldWidth = 100
        )

        val (iw, ih) = Pair(frames[0].width, frames[0].height)
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
        val bytesRequired = 6 * roi.width().toInt() * roi.maps.sumOf { it.bytesPerSample }
        val buffers = (0..2).map{ ByteBuffer.allocate(bytesRequired) }
        creator.provideBuffers(buffers)

        for(frame in frames) creator.updateWithCameraBitmap(frame)

        println("size = ${creator.spaceTimeSampleSize()}")
        println("reached? ${creator.hasReachedBufferLimit()}")


    }


}

