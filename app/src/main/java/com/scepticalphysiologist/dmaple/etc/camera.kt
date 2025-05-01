// Copyright (c) 2025-2025. Dr Sean Paul Parsons. All rights reserved.

package com.scepticalphysiologist.dmaple.etc

import android.hardware.camera2.CameraCharacteristics
import androidx.annotation.OptIn
import androidx.camera.camera2.interop.Camera2CameraInfo
import androidx.camera.camera2.interop.ExperimentalCamera2Interop
import androidx.camera.core.Camera


@OptIn(ExperimentalCamera2Interop::class)
fun cameraLevel(camera: Camera): Int {
    val level = Camera2CameraInfo.from(camera.cameraInfo).getCameraCharacteristic(
        CameraCharacteristics.INFO_SUPPORTED_HARDWARE_LEVEL
    )
    return when(level) {
        CameraCharacteristics.INFO_SUPPORTED_HARDWARE_LEVEL_LEGACY -> 0
        CameraCharacteristics.INFO_SUPPORTED_HARDWARE_LEVEL_EXTERNAL -> 1
        CameraCharacteristics.INFO_SUPPORTED_HARDWARE_LEVEL_LIMITED -> 2
        CameraCharacteristics.INFO_SUPPORTED_HARDWARE_LEVEL_FULL -> 3
        CameraCharacteristics.INFO_SUPPORTED_HARDWARE_LEVEL_3 -> 4
        else -> -1
    }
}

