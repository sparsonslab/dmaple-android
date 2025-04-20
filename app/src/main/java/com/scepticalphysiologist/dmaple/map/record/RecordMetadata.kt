// Copyright (c) 2025-2025. Dr Sean Paul Parsons. All rights reserved.

package com.scepticalphysiologist.dmaple.map.record

import com.google.gson.Gson
import com.google.gson.GsonBuilder
import com.google.gson.JsonSyntaxException
import com.google.gson.TypeAdapter
import com.google.gson.stream.JsonReader
import com.google.gson.stream.JsonWriter
import com.scepticalphysiologist.dmaple.BuildConfig
import com.scepticalphysiologist.dmaple.map.creator.FieldParams
import com.scepticalphysiologist.dmaple.map.field.FieldRoi
import java.io.File
import java.time.Duration
import java.time.Instant
import java.time.format.DateTimeParseException


/** Metadata serialised when a a record is saved, so that it can be loaded later.
 *
 * @property recordingPeriod The start and end of the recording.
 * @property rois The mapping ROIs.
 * @property params The mapping parameters.
 * */
class RecordMetadata(
    val recordingPeriod: List<Instant>,
    val rois: List<FieldRoi>,
    val params: FieldParams
){

    /** The app version for serialisation versioning. */
    val version: String = BuildConfig.VERSION_NAME

    companion object {

        /** GSON (de)serialization object for JSON files. */
        val gson: Gson = GsonBuilder().registerTypeAdapter(Instant::class.java, InstantTypeAdapter()).create()

        /** De-serialise metadata. */
        fun deserialize(file: File): RecordMetadata? {
            fun <T> deserializeAny(file: File, cls: Class<T>): T? {
                if(!file.exists()) return null
                try { return gson.fromJson(file.readText(), cls) }
                catch (_: JsonSyntaxException) { }
                catch(_: java.io.FileNotFoundException) {} // Can happen from access-denied, when file is there.
                return null
            }
            return deserializeAny(file, RecordMetadata::class.java)
        }

    }

    /** Serialise this metadata. */
    fun serialise(file: File){ file.writeText(gson.toJson(this)) }

    /** The recording duration as HH:MM:SS. */
    fun durationString(): String {
        val d = Duration.between(recordingPeriod[0], recordingPeriod[1])
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
