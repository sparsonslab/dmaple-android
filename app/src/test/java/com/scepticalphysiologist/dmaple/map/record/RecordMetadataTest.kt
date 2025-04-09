package com.scepticalphysiologist.dmaple.map.record

import com.google.gson.GsonBuilder
import org.junit.Assert.assertEquals
import org.junit.Test
import java.time.Instant

class RecordMetadataTest {

    @Test
    fun `serialize and deserialize round trip`(){
        // Given: Some metadata.
        val metadata = RecordMetadata(
            startTime = Instant.now().minusSeconds(100L),
            endTime = Instant.now().plusSeconds(1098L)
        )

        // When: The metadata is serialised and then serialised.
        val gson = GsonBuilder().registerTypeAdapter(Instant::class.java, InstantTypeAdapter()).create()
        val serialised = gson.toJson(metadata)
        val deserialised = gson.fromJson(serialised, RecordMetadata::class.java)

        // Then: The deserialised metadata is as the original.
        assertEquals(metadata.startTime.epochSecond, deserialised.startTime.epochSecond)
        assertEquals(metadata.endTime.epochSecond, deserialised.endTime.epochSecond)
    }

}
