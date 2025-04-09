package com.scepticalphysiologist.dmaple.map.record

import com.google.gson.TypeAdapter
import com.google.gson.stream.JsonReader
import com.google.gson.stream.JsonWriter
import java.time.Duration
import java.time.Instant
import java.time.format.DateTimeParseException


/** Some metadata about a recording. */
class RecordMetadata(
    val startTime: Instant,
    val endTime: Instant
){

    /** The recording duration as HH:MM:SS. */
    fun durationString(): String {
        val d = Duration.between(startTime, endTime)
        return String.format("%02d:%02d:%02d", d.toHours(), d.toMinutesPart(), d.toSecondsPart())
    }

}

/** GSON type adaptor for [Instant]. */
class InstantTypeAdapter: TypeAdapter<Instant>() {
    override fun write(writer: JsonWriter?, value: Instant?) {
        if(value == null) {
            writer?.nullValue()
            return
        }
        writer?.value(value.toString())
    }

    override fun read(reader: JsonReader?): Instant {
        reader?.nextString()?.let {
            try { return Instant.parse(it) }
            catch (_: DateTimeParseException) {}
        }
        return Instant.now()
    }
}
