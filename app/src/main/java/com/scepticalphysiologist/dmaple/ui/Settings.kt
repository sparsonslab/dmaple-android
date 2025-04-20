// Copyright (c) 2025-2025. Dr Sean Paul Parsons. All rights reserved.

package com.scepticalphysiologist.dmaple.ui

import android.app.Activity
import android.content.pm.ActivityInfo
import android.graphics.Color
import android.os.Bundle
import androidx.preference.DropDownPreference
import androidx.preference.PreferenceFragmentCompat
import androidx.preference.PreferenceManager
import androidx.preference.SeekBarPreference
import com.scepticalphysiologist.dmaple.MainActivity
import com.scepticalphysiologist.dmaple.R
import com.scepticalphysiologist.dmaple.map.MappingService
import com.scepticalphysiologist.dmaple.map.creator.FieldParams
import com.scepticalphysiologist.dmaple.ui.record.BackgroundHighlight
import com.scepticalphysiologist.dmaple.ui.record.SpineOverlay

class Settings: PreferenceFragmentCompat() {

    companion object {

        /** Screen orientation settings.
         *
         * The user may want to set the orientation of rhe screen when the tablet is flat (e.g.
         * above an organ bath) and so the device will not detect an orientation.
         * */
        private val ORIENTATION_MAP = mapOf(
            "free" to ActivityInfo.SCREEN_ORIENTATION_SENSOR,
            "landscape" to ActivityInfo.SCREEN_ORIENTATION_LANDSCAPE,
            "landscape reverse" to ActivityInfo.SCREEN_ORIENTATION_REVERSE_LANDSCAPE,
            "portrait" to ActivityInfo.SCREEN_ORIENTATION_PORTRAIT,
            "portrait reverse" to ActivityInfo.SCREEN_ORIENTATION_REVERSE_PORTRAIT
        )

        fun setFromPreferences(activity: Activity){
            val prefs = PreferenceManager.getDefaultSharedPreferences(activity)
            fun <T> getPreference(key: String, default: T): T {
                return when (default) {
                    is Boolean -> prefs.getBoolean(key, default) as T
                    is String -> prefs.getString(key, default) as T
                    is Int -> prefs.getInt(key, default) as T
                    is Long -> prefs.getLong(key, default) as T
                    is Float -> prefs.getFloat(key, default) as T
                    is Double -> prefs.getFloat(key, default.toFloat()).toDouble() as T
                    else -> default
                }
            }

            setScreenRotation(getPreference("SCREEN_ORIENTATION", "free"), activity)
            Recorder.leftHanded = getPreference("SCREEN_FOR_LEFT_HAND", false)
            MainActivity.keepScreenOn = getPreference("KEEP_SCREEN_ON", false)
            MainActivity.setMappingServiceFrameRate(getPreference("FRAME_RATE_FPS", "30").toInt())
            setThresholdInverted(getPreference("THRESHOLD_INVERTED", false))
            FieldParams.preference.spineSkipPixels = getPreference("SPINE_SKIP", 0)
            FieldParams.preference.minWidth = getPreference("SEED_MIN_WIDTH", 10)
            FieldParams.preference.spineSmoothPixels = getPreference("SPINE_SMOOTH", 1)
            FieldParams.preference.maxGap = getPreference("SPINE_MAX_GAP", 2)
            MappingService.AUTO_SAVE_ON_CLOSE = getPreference( "SAVE_ON_CLOSE", false)
        }

        fun setScreenRotation(entry: String, activity: Activity) {
            activity.requestedOrientation = ORIENTATION_MAP[entry] ?: ActivityInfo.SCREEN_ORIENTATION_SENSOR
        }

        private fun setThresholdInverted(inverted: Boolean) {
            FieldParams.preference.gutsAreAboveThreshold = !inverted
            // todo - get the below from the above.
            BackgroundHighlight.backgroundIsAboveThreshold = inverted
            SpineOverlay.spinePaint.color = if(inverted) Color.WHITE else Color.BLACK
        }

    }

    override fun onCreatePreferences(savedInstanceState: Bundle?, rootKey: String?) {
        setPreferencesFromResource(R.xml.preferences, rootKey)
        // Fill drop-down options.
        findPreference<DropDownPreference>("SCREEN_ORIENTATION")?.let { pref ->
            val entries = ORIENTATION_MAP.keys.toTypedArray()
            pref.entries = entries
            pref.entryValues = entries
            // ... so the settings page itself rotates immediately when the setting is changed.
            pref.setOnPreferenceChangeListener { _, newValue ->
                setScreenRotation(newValue.toString(), requireActivity())
                true
            }
        }
        findPreference<DropDownPreference>("FRAME_RATE_FPS")?.let { pref ->
            val fps = MainActivity.mapService?.getAvailableFps() ?: listOf(30)
            val entries = fps.map { it.toString() }.toTypedArray()
            pref.entries = entries
            pref.entryValues = entries
        }

        val fieldSize =MainActivity.mapService?.getFieldSize()?.let { sz ->
            "${sz.x.toInt()} x ${sz.y.toInt()} pixels"
        } ?: "unknown"
        findPreference<SeekBarPreference>("SPINE_SKIP")?.let { pref ->
            pref.setSummaryProvider {
                "Skip pixels along the long axis of the gut, " +
                "to reduce resolution of the map's spatial axis.\n" +
                "The current size of the mapping (camera) field is ${fieldSize}."
            }
        }

    }

}
