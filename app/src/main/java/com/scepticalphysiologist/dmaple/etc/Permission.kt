// Copyright (c) 2025-2025. Dr Sean Paul Parsons. All rights reserved.

package com.scepticalphysiologist.dmaple.etc

import android.Manifest
import android.os.Build

/** Permissions that are useful for the functioning of the app. */
enum class Permission (
    /** The key of the permission in [Manifest.permission]. */
    val manifestKey: String,
    /** The API level (aka SDK version) the permission was introduced. */
    val apiLevel: Int,
    /** Explanation for the user aof why the permission is needed. */
    val rationale: String,

) {
    CAMERA(
        manifestKey = Manifest.permission.CAMERA,
        apiLevel = 1,
        rationale ="Access to the camera for making the maps."
    ),
    FOREGROUND_SERVICE(
        manifestKey = Manifest.permission.FOREGROUND_SERVICE,
        apiLevel = 28,
        rationale ="Allow the app to start a foreground service to record maps."
    ),
    FOREGROUND_SERVICE_CAMERA(
        manifestKey = Manifest.permission.FOREGROUND_SERVICE_CAMERA,
        apiLevel = 34,
        rationale ="Allow the app's foreground service to access to the camera."
    ),
    POST_NOTIFICATIONS(
        manifestKey = Manifest.permission.POST_NOTIFICATIONS,
        apiLevel = 33,
        rationale ="Allow the app to post notifications."
    );

    companion object {
        /** The manifest keys of permissions needed for the current API/SDK of the current device. */
        fun manifestKeysRequiredForApi(): Array<String> {
            return entries.filter{ Build.VERSION.SDK_INT >= it.apiLevel }
                          .map{it.manifestKey}
                          .toTypedArray()
        }

        /** Get the rationales associated with a set of manifest keys. */
        fun permissionRationales(manifestKeys: List<String>): List<String> {
            return manifestKeys.map{ key ->
                entries.firstOrNull { permission ->
                    permission.manifestKey == key
                }?.rationale ?: key.toString()
            }.toList()
        }

    }



}