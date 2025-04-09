package com.scepticalphysiologist.dmaple.map.record

import android.text.format.DateUtils
import com.scepticalphysiologist.dmaple.etc.strftime
import com.scepticalphysiologist.dmaple.etc.strptime
import java.time.Duration
import java.time.Instant

class RecordMetadata(
    startTime: Instant,
    endTime: Instant
){

    var startDateTime: String = strftime(startTime, "dd/MM/YYYY, HH:mm")
    var duration: String = DateUtils.formatElapsedTime(Duration.between(startTime, endTime).seconds)

    fun startInstant(): Instant {
        return strptime(startDateTime, "dd/MM/YYYY, HH:mm") ?: Instant.now()
    }

}
