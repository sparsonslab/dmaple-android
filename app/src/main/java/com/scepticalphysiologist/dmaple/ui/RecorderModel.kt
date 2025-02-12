package com.scepticalphysiologist.dmaple.ui

import android.app.Application
import android.content.ComponentName
import android.content.Context
import android.content.Intent
import android.content.ServiceConnection
import android.os.IBinder
import android.text.InputType
import androidx.camera.view.PreviewView
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.MutableLiveData
import com.scepticalphysiologist.dmaple.etc.msg.InputRequired
import com.scepticalphysiologist.dmaple.etc.msg.Message
import com.scepticalphysiologist.dmaple.map.MappingService
import com.scepticalphysiologist.dmaple.map.creator.MapCreator
import com.scepticalphysiologist.dmaple.map.MappingRoi
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.MainScope
import kotlinx.coroutines.cancel
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch


class RecorderModel(application: Application) :
    AndroidViewModel(application),
    ServiceConnection
{

    // Mapping service
    // ---------------
    /** The foreground service to record maps ans save state. */
    private var mapper: MappingService? = null
    /** Indicate that the mapping service has been connected to. */
    val mappingServiceConnected = MutableLiveData<Boolean>(false)

    // State
    // -----

    private var state = 0
    /** The index (in [mapper]'s list of map creators) of the current map to be shown. */
    private var currentMapIndex: Int = 0
    /** Indicate warning messages that should be shown, e.g. when starting mapping. */
    val messages = MutableLiveData<Message?>(null)
    /** Indicate the elapsed time (seconds) of mapping. */
    val timer = MutableLiveData<Long>(0L)
    /** A coroutine scope for running the timer. */
    private var scope: CoroutineScope? = null

    // ---------------------------------------------------------------------------------------------
    // Mapping service initiation and connection.
    // ---------------------------------------------------------------------------------------------

    /** Connect ("bind") the mapping service so that it can be called. */
    fun connectMappingService(context: Context) {
        val intent = Intent(context, MappingService::class.java)
        context.startForegroundService(intent)
        context.bindService(intent, this, Context.BIND_AUTO_CREATE)
    }

    /** Once the service is connected, get an instance of it and notify of the connection. */
    override fun onServiceConnected(p0: ComponentName?, binder: IBinder?) {
        val service = (binder as MappingService.MappingBinder).getService()
        mapper = service
        mappingServiceConnected.postValue(true)
    }

    override fun onServiceDisconnected(p0: ComponentName?) { }

    /** Set the view surface onto which the camera feed will be shown. */
    fun setCameraPreviewSurface(preview: PreviewView) { mapper?.setSurface(preview.surfaceProvider) }

    // ---------------------------------------------------------------------------------------------
    // Public wrapper to mapping service.
    // ---------------------------------------------------------------------------------------------

    fun getState(): Int { return state }

    fun updateState(){
        if(mapper == null) return

        // Recording (1)
        if((state == 1) || mapper!!.isCreatingMaps()) {
            messages.postValue(mapper!!.startStop())
            if(!mapper!!.isCreatingMaps()) {
                stopTimer()
                state = 2
            }
        }
        // ROI selection (0)
        else if(state == 0) {
            messages.postValue(mapper!!.startStop())
            if(mapper!!.isCreatingMaps()) {
                startTimer()
                state = 1
            }
        }
        // Post-recording (2)
        else if (state == 2) {
            askToSaveMaps()
            state = 0
        }

    }

    /** Set the ROIs used for mapping. */
    fun setMappingRois(viewRois: List<MappingRoi>) { mapper?.setRois(viewRois) }

    /** Get the ROIs used for mapping. */
    fun getMappingRois(): List<MappingRoi> { return mapper?.getRois() ?: listOf() }

    fun setExposure(fraction: Float) { mapper?.setExposure(fraction) }

    /** Set the current map to be shown in the [MapView]. */
    fun setCurrentlyShownMap(i: Int) {
        mapper?.let{ currentMapIndex = if(i < it.nMapCreators()) i else 0 }
    }

    /** Get the map creator of the current map shown in the map*/
    fun creatorOfCurrentlyShownMap(): MapCreator? { return mapper?.getMapCreator(currentMapIndex) }

    // ---------------------------------------------------------------------------------------------
    // Timer
    // ---------------------------------------------------------------------------------------------

    private fun startTimer() {
        if((scope != null) || (mapper == null) ) return
        scope = MainScope()
        runTimer()
    }

    private fun stopTimer() {
        scope?.cancel()
        scope = null
    }

    private fun runTimer() = scope?.launch(Dispatchers.Default) {
        while(true) {
            mapper?.let {timer.postValue(it.elapsedSeconds())}
            delay(1000)
        }
    }

    // ---------------------------------------------------------------------------------------------
    // Save maps
    // ---------------------------------------------------------------------------------------------

    private fun askToSaveMaps() {
        val dialog = InputRequired(
            title = "Save Maps",
            message = "Set a prefix for all saved maps.",
            initialValue = "",
            inputType = InputType.TYPE_CLASS_TEXT
        )
        dialog.positive = Pair("Save", this::saveMaps)
        dialog.negative = Pair("Do not save", this::doNotSaveMaps)
        messages.postValue(dialog)
    }

    private fun saveMaps(mapFilePrefix: String) {
        mapper?.clearCreators(mapFilePrefix)
    }

    private fun doNotSaveMaps(input: String) {
        mapper?.clearCreators(null)
    }

}
