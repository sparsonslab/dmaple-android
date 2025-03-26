package com.scepticalphysiologist.dmaple

import com.scepticalphysiologist.dmaple.map.creator.buffer.ShortMap
import com.scepticalphysiologist.dmaple.map.creator.setResolution
import mil.nga.tiff.FieldTagType
import mil.nga.tiff.TIFFImage
import mil.nga.tiff.TiffReader
import mil.nga.tiff.TiffWriter
import java.io.File
import java.nio.ByteBuffer


fun main() {

    val nx = 100
    val map = ShortMap(ByteBuffer.allocate(500_000), nx)
    for(i in 0..50)
        for(j in 0..nx)
            map.addDistance(120)

    val dir = map.toTiffDirectory("xxxxx")
    setResolution(dir, Pair(40f, "cm"), Pair(30f, "s"))
    dir.setStringEntryValue(FieldTagType.ImageUniqueID, "abababs")

    var image = TIFFImage()
    var path = File("/Users/senparsons/Documents/programming/personal/dmaple_android/ex.tiff")
    image.add(dir)
    TiffWriter.writeTiff(path, image)



    path = File("/Users/senparsons/Documents/programming/personal/dmaple_android/image_with_pixelinfo.tif")
    image = TiffReader.readTiff(path)

    val tags = FieldTagType.values()
    for(dir in image.fileDirectories) {
        for(tag in tags) {
            val entry = dir.get(tag)
            try { println("${tag.name}\t${entry.fieldType.name}\t${entry.values}") }
            catch (_: Exception) { }
        }
    }



}

