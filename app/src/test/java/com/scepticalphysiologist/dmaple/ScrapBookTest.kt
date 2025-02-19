package com.scepticalphysiologist.dmaple


import android.view.Surface
import com.google.gson.Gson
import com.scepticalphysiologist.dmaple.etc.Edge
import com.scepticalphysiologist.dmaple.etc.Frame
import com.scepticalphysiologist.dmaple.etc.Point
import com.scepticalphysiologist.dmaple.etc.randomAlphaString
import com.scepticalphysiologist.dmaple.map.record.MappingRecord
import com.scepticalphysiologist.dmaple.map.MappingRoi
import com.scepticalphysiologist.dmaple.map.creator.MapType
import org.junit.jupiter.api.Test
import org.robolectric.RobolectricTestRunner
import java.io.File
import org.junit.runner.RunWith


class Rect(val left: Int,val top: Int, val right: Int, val bottom: Int) {

    fun width(): Int { return right - left }

    fun height(): Int { return bottom - top }

}

@RunWith(RobolectricTestRunner::class)
class ScrapBookTest {

    @Test
    fun `example`() {

        val ns = 100
        val nt = 84

        val area = Rect(10, 14, 51, 84)
        val stepX = 1
        val stepY = 2

        var k = 0
        val n = area.width().floorDiv(stepX) * area.height().floorDiv(stepY)
        val arr = IntArray(n)

        for(j in area.top until area.bottom step stepY)
            for(i in area.left until area.right step stepX) {
                println("$n, $k:  ${ns * nt}, ${j * ns + i}")
                arr[k] = 1 //map[j * ns + i]
                k += 1
            }

    }

    @Test
    fun `example 2`() {
        val roi = MappingRoi(
            frame = Frame(Point(800f, 1200f), orientation = Surface.ROTATION_90),
            threshold = 156,
            seedingEdge = Edge.TOP,
            maps = listOf(MapType.RADIUS, MapType.SPINE),
        )

        val gson = Gson()
        val ser = gson.toJson(roi)

        val roi2 = gson.fromJson(ser, MappingRoi::class.java)

        println("=".repeat(30))
        println(ser)
        println("=".repeat(30))
        println(roi2.frame.size)
        println(roi2.frame.orientation)
        println(roi2.threshold)
        println(roi2.seedingEdge)
        println(roi2.maps)

        println("=".repeat(30))
        assert(true)
    }

    @Test
    fun `example 5`() {

        val uid = randomAlphaString(100)

        println("=".repeat(30))
        println(uid.map{it.toInt()}.sum())

    }

    @Test
    fun `example 3`() {

        val root = File("/Users/senparsons/Downloads/250219_103317_gloop")

        val record = MappingRecord.read(root)

        println("=".repeat(30))
        record?.let {

            for((roi, creators) in it.struct) {
                println(roi.bottom)
                println(creators[0].nMaps)
            }

        } ?: println("null")

        println("=".repeat(30))

    }


}