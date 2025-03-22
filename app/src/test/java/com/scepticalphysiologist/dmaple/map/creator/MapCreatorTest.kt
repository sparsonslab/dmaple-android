package com.scepticalphysiologist.dmaple.map.creator

import android.graphics.Bitmap
import com.scepticalphysiologist.dmaple.geom.Edge
import com.scepticalphysiologist.dmaple.geom.Frame
import com.scepticalphysiologist.dmaple.geom.Point
import com.scepticalphysiologist.dmaple.map.field.FieldRoi
import com.scepticalphysiologist.dmaple.ui.camera.ThresholdBitmap
import org.junit.Assert.assertEquals
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config
import org.robolectric.shadows.ShadowBitmap
import java.nio.ByteBuffer


@RunWith(RobolectricTestRunner::class)
@Config(shadows = [ShadowBitmap::class])
class MapCreatorTest {

     lateinit var diameters: List<Int>

     lateinit var frames: List<Bitmap>

     lateinit var roi: FieldRoi

    @Before
    fun setUp() {
        // The gut is not inverted.
        ThresholdBitmap.highlightAbove = false

        // a series of camera frames of a gut positioned horizontally across the whole frame.
        diameters = listOf(50, 55, 60, 65, 60, 55, 50)
        frames = horizontalGutSeries(diameters = diameters, fieldWidth = 100)

        // An ROI over that gut.
        val (iw, ih) = Pair(frames[0].width, frames[0].height)
        roi = FieldRoi(
            frame = Frame(size = Point(iw.toFloat(),  ih.toFloat()), orientation = 0),
            threshold = 50,
            seedingEdge = Edge.RIGHT,
            maps = listOf(MapType.DIAMETER),
            uid = "abcdefgh0123456789"
        )
        roi.left = 10f
        roi.right = iw.toFloat() - 10f
        roi.top = 10f
        roi.bottom = ih.toFloat() - 10f
    }

    @Test
    fun `reach end of buffer condition reached`() {
        // Given: A map creator for the ROI.
        var creator = MapCreator(roi)

        // When: The creator is provided with buffers large enough to hold all data from the frames.
        val bytesRequired = frames.size * creator.spaceSamples() * roi.maps.maxOf { it.bytesPerSample }
        var buffers = (0..2).map{ ByteBuffer.allocate(bytesRequired) }
        creator.provideBuffers(buffers)
        for(frame in frames) creator.updateWithCameraBitmap(frame)

        // Then: The buffer limits are not reached and all frames are in the map.
        assert(!creator.hasReachedBufferLimit())
        assertEquals(frames.size, creator.timeSamples())

        // When: The creator is provided with buffers NOT large enough to hold all data from the frames.
        creator = MapCreator(roi)
        buffers = (0..2).map{ ByteBuffer.allocate(bytesRequired - 100) }
        creator.provideBuffers(buffers)
        for(frame in frames) creator.updateWithCameraBitmap(frame)

        // Then: The buffer limits have been recorded as reached and the last frame is missing from the map.
        assert(creator.hasReachedBufferLimit())
        assertEquals(frames.size - 1, creator.timeSamples())
    }

    @Test
    fun `correct diameter in maps`() {
        // Given: A map creator for the ROI.
        val creator = MapCreator(roi)

        // When: The creator consumes all frames.
        val bytesRequired = frames.size * creator.spaceSamples() * roi.maps.maxOf { it.bytesPerSample }
        val buffers = (0..2).map{ ByteBuffer.allocate(bytesRequired + 1000) }
        creator.provideBuffers(buffers)
        for(frame in frames) creator.updateWithCameraBitmap(frame)
        // ... and is then the diameter map is converted to a TIFF.
        val dmap = creator.toTiff().first().writeRasters

        // Then: The map raster values have the expected diameter.
        val measuredDiameters = (0 until dmap.height).map{
            dmap.getPixelSample(0, 0, it).toShort() - Short.MIN_VALUE
        }.toList()
        assertEquals(diameters, measuredDiameters)
    }

}
