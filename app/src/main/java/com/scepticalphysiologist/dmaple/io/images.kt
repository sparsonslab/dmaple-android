package com.scepticalphysiologist.dmaple.io

import mil.nga.tiff.FieldType
import mil.nga.tiff.FileDirectory
import mil.nga.tiff.Rasters
import mil.nga.tiff.TIFFImage
import mil.nga.tiff.util.TiffConstants
import java.lang.IndexOutOfBoundsException
import java.nio.ByteBuffer


fun createByteChannelTiff(buffer: ByteBuffer, nx: Int, ny: Int, nc: Int = 1): TIFFImage {

    val dir = FileDirectory()
    dir.setImageWidth(nx)
    dir.setImageHeight(ny)
    dir.samplesPerPixel = nc
    dir.setBitsPerSample(8)

    val raster = Rasters(nx, ny, nc, FieldType.BYTE)
    dir.compression = TiffConstants.COMPRESSION_NO
    dir.planarConfiguration = TiffConstants.PLANAR_CONFIGURATION_CHUNKY
    dir.setRowsPerStrip(raster.calculateRowsPerStrip(dir.planarConfiguration))
    dir.writeRasters = raster

    try {
        for(j in 0 until ny)
            for(i in 0 until nx)
                for(k in 0 until nc)
                    raster.setPixelSample(
                        k, i, j, buffer.get(nc * (j * nx + i) + k) - Byte.MIN_VALUE
                    )
    } catch(_: IndexOutOfBoundsException) {}

    val img = TIFFImage()
    img.add(dir)
    return img

}
