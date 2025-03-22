package com.scepticalphysiologist.dmaple.map.field

/** A camera field's set of mapping ROIs and measurement ruler. */
data class RoisAndRuler(val rois: List<FieldRoi>, val ruler: FieldRuler?)
