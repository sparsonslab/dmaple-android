package com.scepticalphysiologist.dmaple.ui

import android.app.Application
import android.text.InputType
import androidx.camera.view.PreviewView
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.MutableLiveData
import com.scepticalphysiologist.dmaple.MainActivity
import com.scepticalphysiologist.dmaple.etc.msg.InputRequired
import com.scepticalphysiologist.dmaple.etc.msg.Message
import com.scepticalphysiologist.dmaple.etc.randomAlphaString
import com.scepticalphysiologist.dmaple.map.MappingService
import com.scepticalphysiologist.dmaple.map.creator.MapCreator
import com.scepticalphysiologist.dmaple.map.MappingRoi
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.MainScope
import kotlinx.coroutines.cancel
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import java.time.LocalDateTime
import java.time.format.DateTimeFormatter


class RecorderModel(application: Application): AndroidViewModel(application) {

    /** A reference to the mapping service used to record maps and save state in the background. */
    private val mapper: MappingService?
        get() = MainActivity.mapService

    /** Model state.
     * 0 = Create mapping ROIs.
     * 1 = Record maps.
     * 2 = View maps.
     */
    private var state = if(mapper?.isCreatingMaps() == true) 1 else 0
    /** The index (in [mapper]'s list of map creators) of the current map to be shown. */
    private var currentMapIndex: Int = 0
    /** Indicate warning messages that should be shown, e.g. when starting mapping. */
    val messages = MutableLiveData<Message?>(null)
    /** Indicate the elapsed time (seconds) of mapping. */
    val timer = MutableLiveData<Long>(0L)
    /** A coroutine scope for running the timer. */
    private var scope: CoroutineScope? = null

    // ---------------------------------------------------------------------------------------------
    // Finite state machine
    // ---------------------------------------------------------------------------------------------

    /** Get the model's [state]. */
    fun getState(): Int { return state }

    /** Update the model [state] when (e.g.) a button is pressed. */
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

    // ---------------------------------------------------------------------------------------------
    // Public wrapper to the mapping service.
    // ---------------------------------------------------------------------------------------------

    /** Provide the camera preview (surface provider) for the mapping service. */
    fun setCameraPreview(preview: PreviewView) {
        MainActivity.setMappingServiceCameraPreview(preview)
    }

    /** Set the ROIs used for mapping. */
    fun setMappingRois(viewRois: List<MappingRoi>) { mapper?.setRois(viewRois) }

    /** Get the ROIs used for mapping. */
    fun getMappingRois(): List<MappingRoi> { return mapper?.getRois() ?: listOf() }

    /** Set the exposure level. */
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

    /** Show a dialog asking if the user wants to save the maps. */
    private fun askToSaveMaps() {
        val dialog = InputRequired(
            title = "Save Maps?",
            message = "If you want to save the maps\nyou can set a common file identifier.",
            initialValue = "",
            inputType = InputType.TYPE_CLASS_TEXT
        )
        dialog.positive = Pair("Save", this::saveMaps)
        dialog.negative = Pair("Do not save", this::doNotSaveMaps)
        messages.postValue(dialog)
    }

    /** Save all maps. */
    private fun saveMaps(mapId: String) {
        val id = mapId.ifEmpty { randomAlphaString(20) }
        val dt = LocalDateTime.now().format(DateTimeFormatter.ofPattern("YYMMdd_HHmmss"))
        mapper?.clearCreators(mapFilePrefix = "${dt}_$id")
    }

    /** Do not save maps, but still clear-up. */
    private fun doNotSaveMaps(input: String) { mapper?.clearCreators(mapFilePrefix = null) }

}
