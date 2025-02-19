package com.scepticalphysiologist.dmaple.map.creator

import com.scepticalphysiologist.dmaple.map.MappingRoi
import java.nio.MappedByteBuffer

enum class MapType (val title: String){
    DIAMETER(title = "diameter"),
    RADIUS(title = "radius"),
    SPINE(title = "spine profile");

    /** Factory method for map creators (class [MapCreator]).
     *
     * @param roi The mapping ROI.
     * @param bufferProvider A function that returns a buffer for map creation or null if no
     * buffers are available.
     * @return A map creator or null, if not enough buffers are available.
     * */
    fun makeCreator(roi: MappingRoi): MapCreator {
        return when(this) {
            DIAMETER -> BufferedExampleMap(roi)
            RADIUS -> BufferedExampleMap(roi)
            SPINE -> BufferedExampleMap(roi)
        }
    }

    companion object {

        fun getMapType(creator: MapCreator): MapType {
            return when(creator) {
                is BufferedExampleMap -> DIAMETER
                else -> SPINE
            }
        }
    }
}
