package com.scepticalphysiologist.dmaple

import android.os.Bundle
import androidx.appcompat.app.AppCompatActivity
import com.scepticalphysiologist.dmaple.ui.Settings


class SettingsActivity : AppCompatActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        supportFragmentManager.beginTransaction().replace(android.R.id.content, Settings()).commit()
    }
}
