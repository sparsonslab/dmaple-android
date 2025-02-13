package com.scepticalphysiologist.dmaple

import android.app.ActivityManager
import android.content.ComponentName
import android.content.Context
import android.content.Intent
import android.content.ServiceConnection
import androidx.appcompat.app.AppCompatActivity
import android.os.Bundle
import android.os.IBinder
import androidx.camera.core.Preview.SurfaceProvider
import androidx.camera.view.PreviewView
import com.scepticalphysiologist.dmaple.map.MappingService
import java.io.File


class MainActivity : AppCompatActivity(), ServiceConnection {

    companion object {

        /** The system-created storage directory specific for this app. */
        var storageDirectory: File? = null

        /** The foreground service to record maps and save state.
         *
         * This service is required solely by the [Recorder] fragment's view model
         * ([RecordModel]) but must live here to prevent context leak.
         * */
        var mapService: MappingService? = null
        /** The "surface" that shows the camera feed from the service.
         *
         * This is a "holder" for when the surface is set by the [Recorder] fragment
         * but the service has not connected yet. Usually the order turns out to be:
         * connect service > recorder fragment created > service connected.
         * */
        private var surface: SurfaceProvider? = null

        /** Set the view surface onto which the camera feed will be shown. */
        fun setCameraPreviewSurface(preview: PreviewView) {
            surface = preview.surfaceProvider
            mapService?.setSurface(surface!!)
        }
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)
        storageDirectory = applicationContext.getExternalFilesDir(null)

        // Connect the mapping service and initiate its buffers.
        MappingService.initialiseBuffers()
        connectMappingService(applicationContext)
    }

    // ---------------------------------------------------------------------------------------------
    // Mapping service.
    // ---------------------------------------------------------------------------------------------

    /** Connect ("bind") the mapping service so that it can be called. */
    private fun connectMappingService(context: Context) {
        val intent = Intent(context, MappingService::class.java)
        context.startForegroundService(intent)
        context.bindService(intent, this, Context.BIND_AUTO_CREATE)
    }

    /** Once the service is connected, get an instance of it set its surface. */
    override fun onServiceConnected(p0: ComponentName?, binder: IBinder?) {
        mapService = (binder as MappingService.MappingBinder).getService()
        surface?.let { mapService!!.setSurface(it)}
    }

    override fun onServiceDisconnected(p0: ComponentName?) { }

}
