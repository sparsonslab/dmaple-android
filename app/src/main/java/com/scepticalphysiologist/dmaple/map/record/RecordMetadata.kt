package com.scepticalphysiologist.dmaple.map.record

import com.scepticalphysiologist.dmaple.etc.strfduration
import com.scepticalphysiologist.dmaple.etc.strftime
import java.time.Duration
import java.time.Instant

/** Some metadata about a recording. */
class RecordMetadata(
    startTime: Instant,
    endTime: Instant
){
    var startDateTime: String = strftime(startTime, "dd/MM/YYYY, HH:mm")
    var duration: String = strfduration(Duration.between(startTime, endTime))
}
