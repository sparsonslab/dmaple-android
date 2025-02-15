package com.scepticalphysiologist.dmaple.map.creator

import com.scepticalphysiologist.dmaple.map.MappingRoi
import java.nio.MappedByteBuffer

enum class MapType (val title: String){
    DIAMETER(title = "diameter"),
    RADIUS(title = "radius"),
    SPINE(title = "spine profile");

    fun makeCreator(roi: MappingRoi, bufferProvider: (() -> MappedByteBuffer?)): MapCreator? {
        try {
            return when(this) {
                DIAMETER -> BufferedExampleMap(roi, bufferProvider)
                RADIUS -> BufferedExampleMap(roi, bufferProvider)
                SPINE -> BufferedExampleMap(roi, bufferProvider)
            }
        } catch (_: java.lang.IndexOutOfBoundsException) {}
        return null
    }

}
