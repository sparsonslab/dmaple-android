package com.scepticalphysiologist.dmaple.map.creator


import org.junit.jupiter.api.Test

class MapFieldAnalyserTest {

    @Test
    fun `some test`() {

        println("========================")

        val w = 100
        val h = 100
        val image = Array(h) {FloatArray(w)}
        for(i in 0 until w)
            for(j in 0 until h)
                image[j][i] = if((j in 50..70) && (i in 20..60)) 1f else 0f


        val analyser = ArrayFieldAnalyser()
        analyser.setImage(image)
        analyser.threshold = 0.5f
        analyser.gutIsAboveThreshold = true

        analyser.gutIsHorizontal = true
        println(analyser.findGut(25, Pair(20, 80)))


        analyser.gutIsHorizontal = false
        println(analyser.findGut(60, Pair(20, 80)))



        assert(true)


    }

}



