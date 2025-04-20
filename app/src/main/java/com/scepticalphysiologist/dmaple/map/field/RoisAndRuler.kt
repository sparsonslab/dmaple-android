// Copyright (c) 2025-2025. Dr Sean Paul Parsons. All rights reserved.

package com.scepticalphysiologist.dmaple.map.field

/** A camera field's set of mapping ROIs and measurement ruler. */
data class RoisAndRuler(val rois: List<FieldRoi>, val ruler: FieldRuler?)
