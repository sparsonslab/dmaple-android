package com.scepticalphysiologist.dmaple

import android.app.ActivityManager
import android.content.Context
import androidx.appcompat.app.AppCompatActivity
import android.os.Bundle
import com.scepticalphysiologist.dmaple.map.MappingService
import java.io.File


class MainActivity : AppCompatActivity() {


    companion object {
        /** The system-created storage directory specific for this app. */
        var storageDirectory: File? = null

        /** The current number of bytes free in memory. */
        fun freeBytes(): Int {
            // Often the runtime returns 0 free memory due to some bug or other.
            // Keep calling it until it returns something sensible.
            var n = 0L
            while(n <= 0) n = Runtime.getRuntime().freeMemory()
            return n.toInt()
        }

        /** The number of bytes allocated to the app. */
        fun allocatedBytes(context: Context): Int {
            val mng = context.getSystemService(Context.ACTIVITY_SERVICE) as ActivityManager
            return 1_000_000 * mng.memoryClass
        }

    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)

        storageDirectory = applicationContext.getExternalFilesDir(null)

        // Initiate buffers for mapping service.
        MappingService.initialiseBuffers()

    }

}