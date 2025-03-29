package com.scepticalphysiologist.dmaple

import org.junit.Test
import kotlin.math.floor

class Scrap {

    @Test
    fun `example`() {

        val ns = 50
        val nt = 100

        val area_left = 12
        val area_right = 43
        val step_x = 3

        val area_top = 56
        val area_bottom = 85
        val step_y = 3


        var k = 0
        var z = 0
        var k_from_z = 0

        val z_nx = floor((area_right - area_left).toFloat() / step_x.toFloat())
        var z_j = 0
        var z_i = 0

        for (i in area_top until area_bottom step step_y){
            for(j in area_left until area_right step step_x) {
                k = (j * ns) + i

                z_j = (z.toFloat() / z_nx).toInt()
                z_i = z - z_j * z_nx.toInt()
                k_from_z = (area_top * ns) + (area_left * z_j) + (step_x * z_i)

                println("$z === ($i, $j) > $k  === $k_from_z")
                z += 1
            }
        }

        assert(true)

    }


}


/*








 */