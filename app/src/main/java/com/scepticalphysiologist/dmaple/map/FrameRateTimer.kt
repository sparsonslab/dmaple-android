// Copyright (c) 2025-2025. Dr Sean Paul Parsons. All rights reserved.

package com.scepticalphysiologist.dmaple.map

import java.io.BufferedReader
import java.io.BufferedWriter
import java.io.File
import java.io.FileReader
import java.io.FileWriter
import java.time.Duration
import java.time.Instant
import kotlin.time.TimeSource

class FrameRateTimer {

    private val source = TimeSource.Monotonic

    private var recordingStart = Instant.now()

    private var frameStart = TimeSource.Monotonic.markNow()

    private var frameIntervalsMilliSec = ArrayDeque<Float>()

    private var recordingEnd = Instant.now()

    // ---------------------------------------------------------------------------------------------
    // Start recording
    // ---------------------------------------------------------------------------------------------

    fun start(t: Instant = Instant.now()){
        recordingStart = t
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

    fun meanFrameIntervalMilliSec(n: Int = frameIntervalsMilliSec.size - 1): Float? {
        if(n > frameIntervalsMilliSec.size - 1) return null
        return frameIntervalsMilliSec.takeLast(n).average().toFloat()
    }

    // ---------------------------------------------------------------------------------------------
    // End recording
    // ---------------------------------------------------------------------------------------------

    fun stop(t: Instant = Instant.now()){ recordingEnd = t }

    fun recordingPeriod(): List<Instant> { return listOf(recordingStart, recordingEnd) }

    fun recordingDurationSec(): Float {
        return Duration.between(recordingStart, recordingEnd).toMillis().toFloat() * 0.001f
    }

    fun intervalsMilliSec(): List<Float> { return frameIntervalsMilliSec.toList() }

    fun write(file: File) {
        val strm = BufferedWriter(FileWriter(file))
        strm.write("${recordingStart}\n")
        strm.write("${recordingEnd}\n")
        strm.write(frameIntervalsMilliSec.map{it.toString()}.joinToString("\n"))
        strm.close()
    }

    companion object {

        fun read(file: File): FrameRateTimer? {
            if(!file.exists()) return null
            val timer = FrameRateTimer()
            val strm = BufferedReader(FileReader(file))
            try {
                timer.start(Instant.parse(strm.readLine()))
                timer.stop(Instant.parse(strm.readLine()))
            }
            catch(_: java.time.format.DateTimeParseException){ return null }
            catch(_: java.io.IOException){return null}
            strm.lines().forEach { line -> line.toFloatOrNull()?.let{timer.frameIntervalsMilliSec.add(it) }}
            strm.close()
            return timer
        }

    }

}
