// Copyright (c) 2025-2025. Dr Sean Paul Parsons. All rights reserved.

package com.scepticalphysiologist.dmaple

import androidx.test.runner.AndroidJUnit4
import com.scepticalphysiologist.dmaple.geom.Frame
import com.scepticalphysiologist.dmaple.geom.Point

import org.junit.Test
import org.junit.runner.RunWith

/**
 * Instrumented test, which will execute on an Android device.
 *
 * See [testing documentation](http://d.android.com/tools/testing).
 */
@RunWith(AndroidJUnit4::class)
class ExampleInstrumentedTest {



    @Test
    fun `example 1`() {

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