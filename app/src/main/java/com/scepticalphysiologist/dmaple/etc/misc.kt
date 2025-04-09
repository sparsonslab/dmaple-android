package com.scepticalphysiologist.dmaple.etc

import java.time.Duration
import java.time.Instant
import java.time.LocalDateTime
import java.time.ZoneId
import java.time.format.DateTimeFormatter
import java.time.format.DateTimeParseException
import kotlin.random.Random


fun randomAlphaString(n: Int): String {
    val alphas = ('a'..'z').map{it} + ('A'..'Z').map{it}
    return (0 until n).map{alphas[Random.nextInt(alphas.size)]}.joinToString("")
}


/** Time to formatted string, much like Python's strftime. */
fun strftime(time: Instant, format: String): String {
    val formatter = DateTimeFormatter.ofPattern(format).withZone(ZoneId.systemDefault())
    return formatter.format(time)
}

fun strfduration(duration: Duration): String {
    return String.format(
        "%02d:%02d:%02d", duration.toHours(), duration.toMinutesPart(), duration.toSecondsPart()
    )
}

fun strptime(time: String, format: String): Instant? {
    val formatter = DateTimeFormatter.ofPattern(format).withZone(ZoneId.systemDefault())
    try {
        return LocalDateTime.parse(time, formatter).atZone(ZoneId.systemDefault()).toInstant()
    } catch(_: DateTimeParseException) {}
    return null
}

