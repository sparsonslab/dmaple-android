// Copyright (c) 2025-2025. Dr Sean Paul Parsons. All rights reserved.

package com.scepticalphysiologist.dmaple.map.record

import com.scepticalphysiologist.dmaple.geom.Edge
import com.scepticalphysiologist.dmaple.geom.Frame
import com.scepticalphysiologist.dmaple.geom.Point
import com.scepticalphysiologist.dmaple.map.creator.FieldParams
import com.scepticalphysiologist.dmaple.map.creator.MapType
import com.scepticalphysiologist.dmaple.map.field.FieldRoi
import com.scepticalphysiologist.dmaple.map.field.FieldRuler
import org.junit.Assert.assertEquals
import org.junit.Test
import org.robolectric.util.TempDirectory
import java.io.File
import java.time.Instant

class RecordMetadataTest {

    @Test
    fun `serialize and deserialize round trip`(){
        // Given: Some metadata.
        val metadata = RecordMetadata(
            recordingPeriod = listOf(
                Instant.now().minusSeconds(100L),
                Instant.now().plusSeconds(1098L)
            ),
            rois = listOf(
                FieldRoi(
                    frame = Frame(size = Point(1000f, 200f), orientation = 90),
                    c0 = Point(40f, 100.89f), c1 = Point(700f, 150.56f),
                    threshold = 124, seedingEdge = Edge.RIGHT,
                    maps = listOf(MapType.RADIUS, MapType.SPINE),
                    uid = "colon_proximal"
                ),
                FieldRoi(
                    frame = Frame(size = Point(1000f, 200f), orientation = 90),
                    c0 = Point(129.76f,10f), c1 = Point(950f, 120.56f),
                    threshold = 124, seedingEdge = Edge.LEFT,
                    maps = listOf(MapType.DIAMETER),
                    uid = "colon_distal"
                ),
            ),
            ruler = FieldRuler(
                frame = Frame(size = Point(1000f, 200f), orientation = 90),
                p0 = Point(356f, 2.67f), p1 = Point(589.7f, 6.8f),
                unit = "cm"
            ),
            params = FieldParams(
                gutsAreAboveThreshold = false,
                maxGap = 5,
                spineSmoothPixels = 14
            )
        )

        // When: The metadata is serialised and then serialised.
        val recordFolder = TempDirectory().create("record").toFile()
        val metadataFile = File(recordFolder, "metadata.json")
        metadata.serialise(metadataFile)
        val deserialised = RecordMetadata.deserialize(metadataFile)

        // Then: The deserialised metadata is as the original.
        assert(deserialised != null)
        assertEquals(metadata.recordingPeriod[0].epochSecond, deserialised!!.recordingPeriod[0].epochSecond)
        assertEquals(metadata.recordingPeriod[1].epochSecond, deserialised.recordingPeriod[1].epochSecond)
        assertEquals(metadata.rois[1].maps[0], deserialised.rois[1].maps[0])
        assertEquals(metadata.rois[1].uid, deserialised.rois[1].uid)
        assertEquals(metadata.rois[0].c0.y, deserialised.rois[0].c0.y)
        assertEquals(metadata.params.maxGap, deserialised.params.maxGap)
    }

}
