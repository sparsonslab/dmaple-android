// Copyright (c) 2025-2025. Dr Sean Paul Parsons. All rights reserved.

package com.scepticalphysiologist.dmaple.map

import com.scepticalphysiologist.dmaple.assertNumbersEqual
import com.scepticalphysiologist.dmaple.map.camera.FrameTimer
import org.junit.Assert.assertEquals
import org.junit.Test
import org.robolectric.util.TempDirectory
import java.io.File

class FrameRateTimerTest {

    @Test
    fun `write read round trip`(){
        // Given: A frame rate timer.
        val timer = FrameTimer()

        // When: The timer is run.
        timer.markRecordingStart()
        var timeStamp: Long = 6453828978
        for(i in 0 until 50){
            timer.markFrame(timeStamp)
            timeStamp += 30_000_000
        }
        timer.markRecordingEnd()

        // When: The timer is written to file and then read back.
        val file = File(TempDirectory().create("example").toFile(), "timer.txt")
        timer.write(file)
        val timerRead = FrameTimer.read(file)

        // Then: The times are as expected.
        assertEquals(timer.recordingPeriod(), timerRead?.recordingPeriod())
        assertNumbersEqual(timer.intervalsMilliSec(), timerRead?.intervalsMilliSec() ?: listOf())
    }

    @Test
    fun `frame rate estimate correct`(){
        // Given: A frame rate timer.
        val timer = FrameTimer()

        // When: The timer is run at the target frame rate.
        val targetIntervalMs = 45L
        timer.markRecordingStart()
        var timeStamp: Long = 6453828998
        for(i in 0 until 50){
            timer.markFrame(timeStamp)
            timeStamp += targetIntervalMs * 1_000_000
        }
        timer.markRecordingEnd()

        // Then: The mean frame rate is as expected.
        timer.lastFrameIntervalMilliSec()?.let { meanInterval ->
            assertEquals(targetIntervalMs.toFloat(), meanInterval, 0.1f)
        }
    }

}
