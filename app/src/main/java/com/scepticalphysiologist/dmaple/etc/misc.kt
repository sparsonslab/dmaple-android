package com.scepticalphysiologist.dmaple.etc

import java.time.Instant
import java.time.ZoneId
import java.time.format.DateTimeFormatter
import kotlin.random.Random


fun randomAlphaString(n: Int): String {
    val alphas = ('a'..'z').map{it} + ('A'..'Z').map{it}
    return (0 until n).map{alphas[Random.nextInt(alphas.size)]}.joinToString("")
}


/** Time to formatted string, much like Python's strftime. */
fun strftime(time: Instant, format: String): String {
    val formatter = DateTimeFormatter.ofPattern(format).withZone(
        ZoneId.systemDefault()
    )
    return formatter.format(time)
}
