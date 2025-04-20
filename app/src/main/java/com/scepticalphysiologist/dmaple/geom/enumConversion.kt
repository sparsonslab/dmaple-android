// Copyright (c) 2025-2025. Dr Sean Paul Parsons. All rights reserved.

package com.scepticalphysiologist.dmaple.geom

import android.view.Surface
import androidx.camera.core.AspectRatio

/** Get the degree value of a Surface rotation enum. */
fun surfaceRotationDegrees(rotation: Int): Int {
    return when(rotation) {
        Surface.ROTATION_0 -> 0
        Surface.ROTATION_90 -> 90
        Surface.ROTATION_180 -> 180
        Surface.ROTATION_270 -> 270
        else -> 0
    }
}

/** Get the ratio value of an AspectRatio ratio enum. */
fun aspectRatioRatio(aspect: Int): Float {
    return when(aspect) {
        AspectRatio.RATIO_16_9 -> 16f/9f
        AspectRatio.RATIO_4_3 -> 4f/3f
        AspectRatio.RATIO_DEFAULT -> 1f
        else -> 1f
    }
}
