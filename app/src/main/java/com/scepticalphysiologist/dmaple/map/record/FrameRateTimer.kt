// Copyright (c) 2025-2025. Dr Sean Paul Parsons. All rights reserved.

package com.scepticalphysiologist.dmaple.map.record

import java.time.Duration
import java.time.Instant
import kotlin.time.TimeSource

class FrameRateTimer {

    private val source = TimeSource.Monotonic

    private var recordingStart = Instant.now()

    private var frameStart = source.markNow()

    private var frameIntervalsMilliSec = ArrayDeque<Float>()

    private var recordingEnd = Instant.now()

    // ---------------------------------------------------------------------------------------------
    // Start recording
    // ---------------------------------------------------------------------------------------------

    fun start(){
        recordingStart = Instant.now()
        frameIntervalsMilliSec.clear()
    }

    // ---------------------------------------------------------------------------------------------
    // During recording
    // ---------------------------------------------------------------------------------------------

    fun markFrameStart(){
        if(frameIntervalsMilliSec.isEmpty()) frameIntervalsMilliSec.add(0f)
        else frameIntervalsMilliSec.add(frameStart.elapsedNow().inWholeMicroseconds.toFloat() / 1000f)
        frameStart = source.markNow()
    }

    fun millisFromFrameStart(): Long {
        return frameStart.elapsedNow().inWholeMilliseconds
    }

    fun secFromRecordingStart(): Long {
        return Duration.between(recordingStart, Instant.now()).toSeconds()
    }

    fun lastFrameIntervalMilliSec(): Float? { return frameIntervalsMilliSec.lastOrNull() }

    // ---------------------------------------------------------------------------------------------
    // End recording
    // ---------------------------------------------------------------------------------------------

    fun stop(){ recordingEnd = Instant.now() }

    fun recordingPeriod(): List<Instant> { return listOf(recordingStart, recordingEnd) }

    fun recordingDurationSec(): Float {
        return Duration.between(recordingStart, recordingEnd).toMillis().toFloat() * 0.001f
    }

    fun recordingStatistics() {


    }

}
