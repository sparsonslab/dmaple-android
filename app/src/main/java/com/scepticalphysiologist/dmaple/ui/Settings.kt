package com.scepticalphysiologist.dmaple.ui

import android.app.Activity
import android.content.pm.ActivityInfo
import android.graphics.Color
import android.os.Bundle
import androidx.preference.DropDownPreference
import androidx.preference.PreferenceFragmentCompat
import androidx.preference.PreferenceManager
import com.scepticalphysiologist.dmaple.MainActivity
import com.scepticalphysiologist.dmaple.R
import com.scepticalphysiologist.dmaple.ui.record.ThresholdBitmap
import com.scepticalphysiologist.dmaple.map.creator.GutSegmentor
import com.scepticalphysiologist.dmaple.ui.record.SpineOverlay

class Settings: PreferenceFragmentCompat() {

    companion object {

        /** Screen orientation settings.
         *
         * The user may want to set the orientation of rhe screen when the tablet is flat (e.g.
         * above an organ bath) and so the device will not detect an orientation.
         * */
        private val ORIENTATION_MAP = mapOf(
            "auto" to ActivityInfo.SCREEN_ORIENTATION_SENSOR,
            "landscape" to ActivityInfo.SCREEN_ORIENTATION_LANDSCAPE,
            "landscape rev" to ActivityInfo.SCREEN_ORIENTATION_REVERSE_LANDSCAPE,
            "portrait" to ActivityInfo.SCREEN_ORIENTATION_PORTRAIT,
            "portrait rev" to ActivityInfo.SCREEN_ORIENTATION_REVERSE_PORTRAIT
        )

        fun setFromPreferences(activity: Activity){
            val prefs = PreferenceManager.getDefaultSharedPreferences(activity)
            setScreenRotation(prefs.getString("SCREEN_ORIENTATION", "auto"), activity)
            setKeepScreenOn(prefs.getBoolean("KEEP_SCREEN_ON", false))
            setFrameRate(prefs.getString("FRAME_RATE_FPS", "30"))
            setThresholdInverted(prefs.getBoolean("THRESHOLD_INVERTED", false))
            setSpinePixelSkip(prefs.getInt("SPINE_SKIP", 0))
            setSeedMinWidth(prefs.getInt("SEED_MIN_WIDTH", 10))
            setSpineSmooth(prefs.getInt("SPINE_SMOOTH", 1))
            setSpineMaxGap(prefs.getInt("SPINE_MAX_GAP", 2))
        }

        fun setScreenRotation(entry: Any?, activity: Activity) {
            val or = ORIENTATION_MAP[entry.toString()] ?: ActivityInfo.SCREEN_ORIENTATION_SENSOR
            activity.requestedOrientation = or
        }

        private fun setKeepScreenOn(entry: Any?) {
            MainActivity.keepScreenOn = if(entry is Boolean) entry else entry.toString().toBoolean()
        }

        private fun setFrameRate(entry: Any?) {
            entry.toString().toIntOrNull()?.let { MainActivity.setMappingServiceFrameRate(it) }
        }

        private fun setThresholdInverted(entry: Any?) {
            val inverted = if(entry is Boolean) entry else entry.toString().toBoolean()
            ThresholdBitmap.highlightAbove = inverted
            SpineOverlay.spinePaint.color = if(inverted) Color.WHITE else Color.BLACK
        }

        private fun setSpinePixelSkip(entry: Any?) {
            entry.toString().toIntOrNull()?.let { GutSegmentor.spineSkipPixels = it }
        }

        private fun setSeedMinWidth(entry: Any?) {
            entry.toString().toIntOrNull()?.let { GutSegmentor.minWidth = it }
        }

        private fun setSpineSmooth(entry: Any?) {
            entry.toString().toIntOrNull()?.let { GutSegmentor.spineSmoothPixels = it }
        }

        private fun setSpineMaxGap(entry: Any?) {
            entry.toString().toIntOrNull()?.let { GutSegmentor.maxGap = it }
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
                setScreenRotation(newValue, requireActivity())
                true
            }
        }
        findPreference<DropDownPreference>("FRAME_RATE_FPS")?.let { pref ->
            val fps = MainActivity.mapService?.getAvailableFps() ?: listOf(30)
            val entries = fps.map { it.toString() }.toTypedArray()
            pref.entries = entries
            pref.entryValues = entries
        }

    }

}
