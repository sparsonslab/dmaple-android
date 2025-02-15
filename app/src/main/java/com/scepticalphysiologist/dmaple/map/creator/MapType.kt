package com.scepticalphysiologist.dmaple.map.creator

enum class MapType (

    val title: String,

    val creatorClass: Class<out MapCreator>


){
    DIAMETER(
        title = "diameter",
        creatorClass = BufferedExampleMap::class.java
    ),
    RADIUS(
        title = "radius",
        creatorClass = BufferedExampleMap::class.java
    ),
    SPINE(
        title = "spine profile",
        creatorClass = BufferedExampleMap::class.java
    );

}
