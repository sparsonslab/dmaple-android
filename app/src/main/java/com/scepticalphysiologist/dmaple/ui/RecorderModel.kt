package com.scepticalphysiologist.dmaple.ui

import android.app.Application
import androidx.camera.view.PreviewView
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.MutableLiveData
import com.scepticalphysiologist.dmaple.MainActivity
import com.scepticalphysiologist.dmaple.map.field.FieldImage
import com.scepticalphysiologist.dmaple.map.MappingService
import com.scepticalphysiologist.dmaple.map.creator.MapCreator
import com.scepticalphysiologist.dmaple.map.field.RoisAndRuler
import com.scepticalphysiologist.dmaple.map.record.MappingRecord
import com.scepticalphysiologist.dmaple.ui.dialog.ComposeDialog
import com.scepticalphysiologist.dmaple.ui.dialog.SaveInfo
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.MainScope
import kotlinx.coroutines.cancel
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch

/** The states the recorder. */
enum class RecState {
    PRE_RECORD,
    RECORDING,
    POST_RECORD,
    OLD_RECORD
}

class RecorderModel(application: Application): AndroidViewModel(application) {

    /** A reference to the mapping service used to record maps and save state in the background. */
    private val mapper: MappingService?
        get() = MainActivity.mapService

    /** Model state.*/
    private var state: RecState =
        if(mapper?.isCreatingMaps() == true) RecState.RECORDING else RecState.PRE_RECORD

    /** Indicate warning messages that should be shown, e.g. when starting mapping. */
    val messages = MutableLiveData<ComposeDialog?>(null)
    /** Indicate the elapsed time (seconds) of mapping. */
    val timer = MutableLiveData<Long>(0L)
    /** A coroutine scope for running the timer. */
    private var scope: CoroutineScope? = null

    // ---------------------------------------------------------------------------------------------
    // Finite state machine
    // ---------------------------------------------------------------------------------------------

    /** Get the model's [state]. */
    fun getState(): RecState { return state }

    /** Update the model [state] when (e.g.) a button is pressed.
     *
     * @param field The field ROIS and ruler (required for when starting a recording).
     * */
    fun updateState(field: RoisAndRuler){
        if(mapper == null) return

        if((state == RecState.RECORDING) || mapper!!.isCreatingMaps()) {
            messages.postValue(mapper!!.startStop())
            if(!mapper!!.isCreatingMaps()) {
                stopTimer()
                state = RecState.POST_RECORD
            }
        }
        else if(state == RecState.PRE_RECORD) {
            mapper!!.setRoisAndRuler(field)
            messages.postValue(mapper!!.startStop())
            if(mapper!!.isCreatingMaps()) {
                startTimer()
                state = RecState.RECORDING
            }
        }
        else if (state == RecState.POST_RECORD) {
            askToSaveMaps()
            state = RecState.PRE_RECORD
        }
        else if (state == RecState.OLD_RECORD) {
            doNotSaveMaps()
            state = RecState.PRE_RECORD
        }
    }

    // ---------------------------------------------------------------------------------------------
    // Public wrapper to the mapping service.
    // ---------------------------------------------------------------------------------------------

    /** Provide the camera preview (surface provider) for the mapping service. */
    fun setCameraPreview(preview: PreviewView) { MainActivity.setMappingServiceCameraPreview(preview) }

    /** Get the ROIs used for mapping. */
    fun getRoisAndRuler(): RoisAndRuler {
        return mapper?.getRoisAndRuler() ?: RoisAndRuler(listOf(), null)
    }

    /** Set the exposure level. */
    fun setExposure(fraction: Float) { mapper?.setExposure(fraction) }

    /** Update the currently shown map given the UID of a selected ROI. */
    fun updateCurrentlyShownMap(selectedRoiUid: String) { mapper?.setNextMap(selectedRoiUid) }

    /** Get the currently shown map - its creator and map index. */
    fun getCurrentlyShownMap(): Pair<MapCreator?, Int> { return mapper?.getCurrentMapCreator() ?: Pair(null, 0) }

    /** Get the last image of the mapping field. */
    fun getLastFieldImage(): FieldImage? { return mapper?.getLastFieldImage() }

    /** Load a recording. */
    fun loadRecording(record: MappingRecord): Boolean {
        val loaded = mapper?.loadRecord(record) ?: false
        if(loaded) state = RecState.OLD_RECORD
        return loaded
    }

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
    private fun askToSaveMaps() { messages.postValue(SaveInfo(::saveMaps, ::doNotSaveMaps)) }

    /** Save all maps. */
    private fun saveMaps(dir: String) { mapper?.saveAndClear(dir) }

    /** Do not save maps, but still clear-up. */
    private fun doNotSaveMaps(input: String = "") { mapper?.saveAndClear(null) }

}
