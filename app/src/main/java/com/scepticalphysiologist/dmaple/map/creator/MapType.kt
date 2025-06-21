// Copyright (c) 2025-2025. Dr Sean Paul Parsons. All rights reserved.

package com.scepticalphysiologist.dmaple.map.creator

/** The types of maps that be created. */
enum class MapType (
    /** The title of the map. */
    val title: String,
    /** The number of maps created for the type. */
    val nMaps: Int,
    /** The number of bytes required per space-time sample/pixel per map. */
    val bytesPerSample: Int
){
    DIAMETER(title = "diameter", nMaps = 1, bytesPerSample = 2),
    RADIUS(title = "radius", nMaps = 2, bytesPerSample = 2),
    SPINE(title = "spine profile", nMaps = 1, bytesPerSample = 1),
    LIGHT(title = "light box", nMaps = 1, bytesPerSample = 1);
}
