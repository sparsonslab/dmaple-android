package com.scepticalphysiologist.dmaple.ui

import android.app.Activity
import android.content.Context
import android.content.pm.ActivityInfo
import android.os.Bundle
import androidx.preference.DropDownPreference
import androidx.preference.PreferenceFragmentCompat
import androidx.preference.PreferenceManager
import androidx.preference.SwitchPreference
import com.scepticalphysiologist.dmaple.MainActivity
import com.scepticalphysiologist.dmaple.R
import com.scepticalphysiologist.dmaple.etc.ThresholdBitmap

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

        fun set(activity: Activity){
            val prefs = PreferenceManager.getDefaultSharedPreferences(activity)
            setScreenRotation(prefs.getString("SCREEN_ORIENTATION", "auto"), activity)
            setFrameRate(prefs.getString("FRAME_RATE_FPS", "30"))
            setThresholdInverted(prefs.getBoolean("THRESHOLD_INVERTED", false))
        }

        private fun setScreenRotation(entry: Any?, activity: Activity) {
            activity.requestedOrientation = ORIENTATION_MAP[entry.toString()] ?: ActivityInfo.SCREEN_ORIENTATION_SENSOR
        }

        private fun setFrameRate(entry: Any?) {
            entry.toString().toIntOrNull()?.let { MainActivity.setMappingServiceFrameRate(it) }
        }

        private fun setThresholdInverted(entry: Any?) {
            ThresholdBitmap.highlightAbove = if(entry is Boolean) entry else entry.toString().toBoolean()
        }

    }

    override fun onCreatePreferences(savedInstanceState: Bundle?, rootKey: String?) {
        setPreferencesFromResource(R.xml.preferences, rootKey)

        // Screen orientation.
        findPreference<DropDownPreference>("SCREEN_ORIENTATION")?.let { pref ->
            val entries = ORIENTATION_MAP.keys.toTypedArray()
            pref.entries = entries
            pref.entryValues = entries
            pref.setOnPreferenceChangeListener { _, newValue ->
                setScreenRotation(newValue, requireActivity())
                true
            }
        }

        // Frame rate.
        findPreference<DropDownPreference>("FRAME_RATE_FPS")?.let { pref ->
            val fps = MainActivity.mapService?.getAvailableFps() ?: listOf(30)
            val entries = fps.map { it.toString() }.toTypedArray()
            pref.entries = entries
            pref.entryValues = entries
            pref.setOnPreferenceChangeListener { _, newValue ->
                setFrameRate(newValue)
                true
            }
        }

        // Thresholding.
        findPreference<SwitchPreference>("THRESHOLD_INVERTED")?.let { pref ->
            pref.setOnPreferenceChangeListener { _, newValue ->
                setThresholdInverted(newValue)
                true
            }
        }

    }



}
