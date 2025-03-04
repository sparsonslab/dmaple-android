package com.scepticalphysiologist.dmaple


import com.scepticalphysiologist.dmaple.etc.Frame
import com.scepticalphysiologist.dmaple.etc.Point

import org.junit.jupiter.api.Test
import org.robolectric.RobolectricTestRunner
import org.junit.runner.RunWith


class Rect(val left: Int,val top: Int, val right: Int, val bottom: Int) {

    fun width(): Int { return right - left }

    fun height(): Int { return bottom - top }

}

@RunWith(RobolectricTestRunner::class)
class ScrapBookTest {

    @Test
    fun `transform`() {

        val frame1 = Frame(Point(500f, 600f), 97)
        val p1 = listOf(Point(34.7f, 89.6f), Point(340f, 78f))

        val frame2 = Frame(Point(200f, 800f), 120)

        for(p in frame1.transformPoints(p1, frame2, resize = true)) println(p)

        val mat = frame1.transformMatrix(frame2, resize = true)
        val ps = Point.toFloatArray(p1)
        mat.mapPoints(ps)
        println(ps.toList())


        assert(true)
    }




}