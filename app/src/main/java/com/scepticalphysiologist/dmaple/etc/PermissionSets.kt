package com.scepticalphysiologist.dmaple.etc

import android.Manifest

/** Permissions that are useful for the functioning of the app. */
enum class PermissionSets (
    /** Explanation for the user as to why these permissions are needed. */
    val rationale: String,

    /** Set of permission IDs as defined by Android.
     * [https://developer.android.com/reference/android/Manifest.permission]. */
    val permissions: Set<String>,
) {
    CONNECTION(
        rationale ="Access to the camera for recording video.",
        permissions = setOf(
            Manifest.permission.CAMERA,
            Manifest.permission.FOREGROUND_SERVICE,
            Manifest.permission.FOREGROUND_SERVICE_CAMERA,
            Manifest.permission.POST_NOTIFICATIONS
        ),
    );

    companion object {
        /** The IDs of all the permissions wanted. */
        fun allPermissions(): Set<String> {
            return entries.map {it.permissions}.reduce { x, y -> x + y }
        }
    }

}