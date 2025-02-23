package com.scepticalphysiologist.dmaple.ui

import android.os.Bundle
import androidx.preference.PreferenceFragmentCompat
import com.scepticalphysiologist.dmaple.R

class Settings: PreferenceFragmentCompat() {
    override fun onCreatePreferences(savedInstanceState: Bundle?, rootKey: String?) {
        setPreferencesFromResource(R.xml.preferences, rootKey)
    }
}
