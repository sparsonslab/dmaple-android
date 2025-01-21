package com.scepticalphysiologist.dmaple


import org.junit.jupiter.api.Test

class Rect(val left: Int,val top: Int, val right: Int, val bottom: Int) {

    fun width(): Int { return right - left }

    fun height(): Int { return bottom - top }

}


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


}