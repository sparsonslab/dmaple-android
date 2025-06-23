// Copyright (c) 2025-2025. Dr Sean Paul Parsons. All rights reserved.

package com.scepticalphysiologist.dmaple

import android.content.ComponentName
import android.content.Context
import android.content.Intent
import android.content.ServiceConnection
import android.content.pm.PackageManager
import androidx.appcompat.app.AppCompatActivity
import android.os.Bundle
import android.os.IBinder
import android.util.Log
import android.view.WindowManager
import androidx.camera.core.Preview.SurfaceProvider
import androidx.camera.view.PreviewView
import androidx.lifecycle.MutableLiveData
import com.scepticalphysiologist.dmaple.etc.Permission
import com.scepticalphysiologist.dmaple.ui.dialog.Warnings
import com.scepticalphysiologist.dmaple.map.MappingService
import com.scepticalphysiologist.dmaple.map.buffer.MapBufferProvider
import com.scepticalphysiologist.dmaple.map.record.MappingRecord
import com.scepticalphysiologist.dmaple.ui.Recorder
import com.scepticalphysiologist.dmaple.ui.Settings


class MainActivity : AppCompatActivity(), ServiceConnection {

    companion object {

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

        /** Set the bit-rate (video quality) of captured video. Set to 0 to not capture video. */
        fun setMappingServiceVideoBitRate(kiloBitsPerSecond: Int) { mapService?.setVideoBitRate(kiloBitsPerSecond) }

    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        Log.i("dmaple_lifetime", "main activity: onCreate")
        setContentView(R.layout.activity_main)

        // Prepare.
        MappingRecord.loadRecords()
        setPreferences()
        requestPermissions()
    }

    override fun onResume() {
        super.onResume()
        Log.i("dmaple_lifetime", "main activity: onResume")
        // If we are resuming after going into settings, set the preferences.
        setPreferences()
    }

    override fun onDestroy() {
        super.onDestroy()
        Log.i("dmaple_lifetime", "main activity: onDestroy")
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
        val permissionsToAsk = Permission.requiredManifestPermissions()
        if(permissionsToAsk.isNotEmpty()) requestPermissions(permissionsToAsk, 6543)
    }

    override fun onRequestPermissionsResult(requestCode: Int, permissions: Array<out String>, grantResults: IntArray) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults)
        // If all permissions have been given,
        val nonGranted = permissions.filterIndexed { i, _ -> grantResults[i] == PackageManager.PERMISSION_DENIED }
        if(nonGranted.isEmpty()) connectMappingService(applicationContext)
        else {
            val warning = Warnings("The App Cannot Run")
            warning.add("You have not given the permissions required for the app to run:", true)
            warning.add(Permission.permissionRationales(nonGranted).joinToString("\n"), true)
            warning.add("Please either:", true)
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
        Log.i("dmaple_lifetime", "main activity: connectMappingService")
        val intent = Intent(context, MappingService::class.java)
        context.startForegroundService(intent)
        context.bindService(intent, this, Context.BIND_AUTO_CREATE)
    }

    /** Once the service is connected, get an instance of it set its surface. */
    override fun onServiceConnected(p0: ComponentName?, binder: IBinder?) {
        Log.i("dmaple_lifetime", "main activity: onServiceConnected")
        // Get the service object.
        mapService = (binder as MappingService.MappingBinder).getService()
        mapService?.let { service ->
            // Set the service's surface to the provided preview surface.
            surface?.let { service.setSurface(it) }
            // Set the service's buffer provider to the device's public documents folder.
            // 10 buffers gives 10 spatio-temporal maps.
            // 100 MB ~= 60 min x 60 sec/min x 30 frame/sec x 1000 bytes/frame.
            applicationContext.getExternalFilesDir(null)?.let { documentsFolder ->
                service.setBufferProvider(MapBufferProvider(
                    sourceDirectory = documentsFolder,
                    nBuffers = 10,
                    bufferByteSize = 100_000_000L
                ))
            }
            // Set preferences.
            setPreferences()
        }
    }

    override fun onServiceDisconnected(p0: ComponentName?) {
        Log.i("dmaple_lifetime", "main activity: onServiceDisconnected")
    }

}
