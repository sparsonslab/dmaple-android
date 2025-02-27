package com.scepticalphysiologist.dmaple.ui

import android.os.Bundle
import androidx.preference.DropDownPreference
import androidx.preference.PreferenceFragmentCompat
import com.scepticalphysiologist.dmaple.MainActivity
import com.scepticalphysiologist.dmaple.R

class Settings: PreferenceFragmentCompat() {
    override fun onCreatePreferences(savedInstanceState: Bundle?, rootKey: String?) {
        setPreferencesFromResource(R.xml.preferences, rootKey)

        // Frame rate.
        findPreference<DropDownPreference>("FRAME_RATE_FPS")?.let { pref ->
            val fps = MainActivity.mapService?.getAvailableFps() ?: listOf(30)
            val entries = fps.map { it.toString() }.toTypedArray()
            pref.entries = entries
            pref.entryValues = entries
            MainActivity.setMappingServiceFrameRate(pref.entry.toString())
            pref.setOnPreferenceChangeListener { _, newValue ->
                MainActivity.setMappingServiceFrameRate(newValue)
                true
            }
        }

    }
}
