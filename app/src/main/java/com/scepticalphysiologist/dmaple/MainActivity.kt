package com.scepticalphysiologist.dmaple

import androidx.appcompat.app.AppCompatActivity
import android.os.Bundle
import java.io.File

class MainActivity : AppCompatActivity() {


    companion object {
        /** The system-created storage directory specific for this app. */
        var storageDirectory: File? = null
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)

        storageDirectory = applicationContext.getExternalFilesDir(null)
    }

}