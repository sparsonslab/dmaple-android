// Copyright (c) 2025-2025. Dr Sean Paul Parsons. All rights reserved.

package com.scepticalphysiologist.dmaple.map

import com.scepticalphysiologist.dmaple.assertNumbersEqual
import org.junit.Assert.assertEquals
import org.junit.Test
import org.robolectric.util.TempDirectory
import java.io.File

class FrameRateTimerTest {

    @Test
    fun `write read round trip`(){
        // Given: A frame rate timer.
        val timer = FrameRateTimer()

        // When: The timer is run at the target frame rate.
        val targetInterval = 30L
        timer.start()
        for(i in 0 until 50){
            timer.markFrameStart()
            Thread.sleep(targetInterval - 2)
        }
        timer.stop()

        // When: The timer is written to file and then read back.
        val file = File(TempDirectory().create("example").toFile(), "timer.txt")
        timer.write(file)
        val timerRead = FrameRateTimer.read(file)

        // Then: The times are as expected.
        assertEquals(timer.recordingPeriod(), timerRead?.recordingPeriod())
        assertNumbersEqual(timer.intervalsMilliSec(), timerRead?.intervalsMilliSec() ?: listOf())
    }

    @Test
    fun `frame rate estimate correct`(){
        // Given: A frame rate timer.
        val timer = FrameRateTimer()

        // When: The timer is run at the target frame rate.
        val targetInterval = 30L
        timer.start()
        for(i in 0 until 50){
            timer.markFrameStart()
            Thread.sleep(targetInterval - 2)
        }
        timer.stop()

        // Then: The mean frame rate is as expected.
        timer.lastFrameIntervalMilliSec()?.let { meanInterval ->
            assertEquals(targetInterval.toFloat(), meanInterval, 2f)
        }
    }

}
