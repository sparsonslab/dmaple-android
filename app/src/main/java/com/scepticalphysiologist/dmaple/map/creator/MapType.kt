package com.scepticalphysiologist.dmaple.map.creator

enum class MapType (

    val title: String,

    val creator: Class<out MapCreator>

){
    DIAMETER(
        title = "diameter",
        creator = BufferedExampleMap::class.java
    ),
    RADIUS(
        title = "radius",
        creator = BufferedExampleMap::class.java
    ),
    SPINE(
        title = "spine profile",
        creator = BufferedExampleMap::class.java
    );
}
