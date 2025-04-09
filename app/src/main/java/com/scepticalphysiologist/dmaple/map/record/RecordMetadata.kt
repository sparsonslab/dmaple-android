package com.scepticalphysiologist.dmaple.map.record

import java.time.Duration
import java.time.Instant
import java.time.ZoneId
import java.time.format.DateTimeFormatter

/** Some metadata about a recording. */
class RecordMetadata(
    startTime: Instant,
    endTime: Instant
){
    var startDateTime: String = strftime(startTime, "dd/MM/YYYY, HH:mm")
    var duration: String = strfduration(Duration.between(startTime, endTime), "%02d:%02d:%02d")
}

/** Time to formatted string, much like Python's strftime. */
fun strftime(time: Instant, format: String): String {
    val formatter = DateTimeFormatter.ofPattern(format).withZone(ZoneId.systemDefault())
    return formatter.format(time)
}

/** Duration to formatted string. */
fun strfduration(duration: Duration, format: String): String {
    return String.format(format , duration.toHours(), duration.toMinutesPart(), duration.toSecondsPart())
}
