package com.scepticalphysiologist.dmaple

import android.content.ComponentName
import android.content.Context
import android.content.Intent
import android.content.ServiceConnection
import android.content.pm.PackageManager
import androidx.appcompat.app.AppCompatActivity
import android.os.Bundle
import android.os.IBinder
import android.view.WindowManager
import androidx.camera.core.Preview.SurfaceProvider
import androidx.camera.view.PreviewView
import androidx.lifecycle.MutableLiveData
import com.scepticalphysiologist.dmaple.etc.PermissionSets
import com.scepticalphysiologist.dmaple.ui.dialog.Warnings
import com.scepticalphysiologist.dmaple.map.MappingService
import com.scepticalphysiologist.dmaple.map.record.MappingRecord
import com.scepticalphysiologist.dmaple.ui.Recorder
import com.scepticalphysiologist.dmaple.ui.Settings
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

        /** Keep the screen on, irrespective of the device's sleep settings. */
        var keepScreenOn: Boolean = false

        /** Any messages that need to be broadcast (e.g. for warnings dialog display). */
        val message = MutableLiveData<Warnings?>(null)

        /** Set the view surface onto which the camera feed will be shown. */
        fun setMappingServiceCameraPreview(preview: PreviewView) {
            surface = preview.surfaceProvider
            mapService?.setSurface(surface!!)
        }

        /** Set the rate at which camera frames will be grabbed for mapping. */
        fun setMappingServiceFrameRate(fps: Int) { mapService?.setFps(fps) }

    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)

        // Storage
        storageDirectory = applicationContext.getExternalFilesDir(null)

        // Load records
        MappingRecord.loadRecords()

        // Set static attributes from preferences.
        setPreferences()

        // Ask for permissions.
        requestPermissions()
    }

    override fun onResume() {
        super.onResume()
        // If we are resuming after going into settings, set the preferences.
        setPreferences()
    }

    /** Set system-wide variables from preferences. */
    private fun setPreferences() {
        Settings.setFromPreferences(this)
        if(keepScreenOn) window.addFlags(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON)
        else window.clearFlags(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON)
    }

    // ---------------------------------------------------------------------------------------------
    // Permissions
    // ---------------------------------------------------------------------------------------------

    private fun requestPermissions() {
        val permissionsToAsk = PermissionSets.allPermissions().toTypedArray()
        if(permissionsToAsk.isNotEmpty())requestPermissions(permissionsToAsk, 6543)
    }

    override fun onRequestPermissionsResult(requestCode: Int, permissions: Array<out String>, grantResults: IntArray) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults)
        // If all permissions have been given,
        if(grantResults.all{it == PackageManager.PERMISSION_GRANTED}) {
            connectMappingService(applicationContext)
        } else {
            val warning = Warnings("The App Cannot Run")
            warning.add("You have not given the permissions required for the app to run.\nPlease either:", true)
            warning.add("1. Close and reopen the app and give all the requested permissions.", true)
            warning.add("2. Open the device settings and give the app the Camera permission.", true)
            message.postValue(warning)
        }
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
        // Set the preview surface.
        surface?.let { mapService!!.setSurface(it) }
    }

    override fun onServiceDisconnected(p0: ComponentName?) { }

}
