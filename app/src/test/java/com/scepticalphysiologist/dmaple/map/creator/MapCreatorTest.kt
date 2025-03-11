package com.scepticalphysiologist.dmaple.map.creator

import com.scepticalphysiologist.dmaple.etc.Edge
import com.scepticalphysiologist.dmaple.etc.Frame
import com.scepticalphysiologist.dmaple.etc.Point
import com.scepticalphysiologist.dmaple.map.field.FieldRoi
import org.junit.Assert.assertEquals
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
        // Given:
        // ... a series of camera frames of a gut.
        val frames = horizontalGutSeries(
            diameters = listOf(50, 55, 60, 65, 60, 55, 50),
            fieldWidth = 100
        )
        // ... an ROI over that gut.
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

        // When: The creator is provided with buffers large enough to hold all data from the frames.
        var creator = MapCreator(roi)
        val bytesRequired = frames.size * creator.spaceSamples() * roi.maps.maxOf { it.bytesPerSample }
        var buffers = (0..2).map{ ByteBuffer.allocate(bytesRequired) }
        creator.provideBuffers(buffers)
        for(frame in frames) creator.updateWithCameraBitmap(frame)

        // Then: The buffer limits are not reached.
        assert(!creator.hasReachedBufferLimit())
        assertEquals(frames.size, creator.timeSamples())

        // When: The creator is provided with buffers NOT large enough to hold all data from the frames.
        creator = MapCreator(roi)
        buffers = (0..2).map{ ByteBuffer.allocate(bytesRequired - 100) }
        creator.provideBuffers(buffers)
        for(frame in frames) creator.updateWithCameraBitmap(frame)

        // Then: The buffer limits are reached.
        assert(creator.hasReachedBufferLimit())
        assertEquals(frames.size - 1, creator.timeSamples())
    }

}

