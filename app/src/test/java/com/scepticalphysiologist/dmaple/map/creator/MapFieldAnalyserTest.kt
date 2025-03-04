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
            for(j in 0 until h) {
                image[j][i] = if ((j in 50..80) && (i in 20..60)) 1f else 0f
                if((j in 60..62) || (i in 33..34)) image[j][i] = 0f
            }


        val j: Int = 130
        println(j.toShort())


        val analyser = ArrayGutSegmentor()
        analyser.setFieldImage(image)
        analyser.threshold = 0.5f
        analyser.gutIsAboveThreshold = true
        analyser.minWidth = 10
        analyser.maxGap = 5



        // Horizontal
        analyser.gutIsHorizontal = true
        analyser.setLongSection(58, 10)
        analyser.detectGutAndSeedSpine(Pair(0, 90))
        println(analyser.spine.toList())

        // Vertical
        analyser.gutIsHorizontal = false
        analyser.setLongSection(51, 80)
        analyser.detectGutAndSeedSpine(Pair(0, 90))
        println(analyser.spine.toList())



        assert(true)


    }

}



