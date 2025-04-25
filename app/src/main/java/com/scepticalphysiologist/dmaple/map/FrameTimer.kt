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

/** Records the duration of a recording and the interval of its frames. */
class FrameTimer {

    /** A monotonic timer. */
    private val source = TimeSource.Monotonic
    /** The time at the start of the recording. */
    private var recordingStart = Instant.now()
    /** The time at the end of the recording. */
    private var recordingEnd = Instant.now()
    /** The time at the start of the last frame in the recording. */
    private var frameStart = TimeSource.Monotonic.markNow()
    /** For each frame in the recording, the interval in milliseconds since the last frame.
     * Zero for the first frame. */
    private var frameIntervalsMilliSec = ArrayDeque<Float>()

    // ---------------------------------------------------------------------------------------------
    // Mark times.
    // ---------------------------------------------------------------------------------------------

    /** Mark the start of a recording. */
    fun markRecordingStart(t: Instant = Instant.now()){
        recordingStart = t
        frameIntervalsMilliSec.clear()
    }

    /** Mark the start of a frame. */
    fun markFrameStart(){
        if(frameIntervalsMilliSec.isEmpty()) frameIntervalsMilliSec.add(0f)
        else frameIntervalsMilliSec.add(frameStart.elapsedNow().inWholeMicroseconds.toFloat() / 1000f)
        frameStart = source.markNow()
    }

    /** Mark the end of a recording. */
    fun markRecordingEnd(t: Instant = Instant.now()){ recordingEnd = t }

    // ---------------------------------------------------------------------------------------------
    // Information
    // ---------------------------------------------------------------------------------------------

    /** Micro-seconds elapsed since the start of the last frame. */
    fun microSecFromFrameStart(): Long { return frameStart.elapsedNow().inWholeMicroseconds }

    /** Seconds since the start of the recording. */
    fun secFromRecordingStart(): Long {
        return Duration.between(recordingStart, Instant.now()).toSeconds()
    }

    /** Milli-second interval of the last frame. */
    fun lastFrameIntervalMilliSec(): Float? { return frameIntervalsMilliSec.lastOrNull() }

    fun nFrames(): Int { return frameIntervalsMilliSec.size}

    /** Mean milli-second interval of the last [n] frames or 0 if their have not been n frames. */
    fun meanFrameIntervalMilliSec(n: Int = frameIntervalsMilliSec.size - 1): Float {
        if(n > frameIntervalsMilliSec.size - 1) return 0f
        return frameIntervalsMilliSec.takeLast(n).average().toFloat()
    }

    /** All the frame intervals in mill-seconds. */
    fun intervalsMilliSec(): List<Float> { return frameIntervalsMilliSec.toList() }

    /** The times at ehe start and end of the recording. */
    fun recordingPeriod(): List<Instant> { return listOf(recordingStart, recordingEnd) }

    /** The second duration of the recording. */
    fun recordingDuration(): Duration { return Duration.between(recordingStart, recordingEnd) }

    // ---------------------------------------------------------------------------------------------
    // I/O
    // ---------------------------------------------------------------------------------------------

    /** Write the recording start and end times and frame intervals to file. */
    fun write(file: File) {
        val strm = BufferedWriter(FileWriter(file))
        strm.write("${recordingStart}\n")
        strm.write("${recordingEnd}\n")
        strm.write(frameIntervalsMilliSec.map{it.toString()}.joinToString("\n"))
        strm.close()
    }

    companion object {
        /** Read the recording start and end times and frame intervals from file. */
        fun read(file: File): FrameTimer? {
            if(!file.exists()) return null
            val timer = FrameTimer()
            val strm = BufferedReader(FileReader(file))
            try {
                timer.markRecordingStart(Instant.parse(strm.readLine()))
                timer.markRecordingEnd(Instant.parse(strm.readLine()))
            }
            catch(_: java.time.format.DateTimeParseException){ return null }
            catch(_: java.io.IOException){return null}
            strm.lines().forEach { line -> line.toFloatOrNull()?.let{timer.frameIntervalsMilliSec.add(it) }}
            strm.close()
            return timer
        }
    }

}
