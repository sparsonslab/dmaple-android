package com.scepticalphysiologist.dmaple.ui

import android.app.Application
import android.content.ComponentName
import android.content.Context
import android.content.Intent
import android.content.ServiceConnection
import android.os.IBinder
import androidx.camera.view.PreviewView
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.MutableLiveData
import com.scepticalphysiologist.dmaple.ui.camera.MappingService
import com.scepticalphysiologist.dmaple.ui.camera.MapCreator
import com.scepticalphysiologist.dmaple.ui.camera.MappingRoi
import com.scepticalphysiologist.dmaple.ui.helper.Warnings
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
    /** The index (in [mapper]'s list of map creators) of the current map to be shown. */
    private var currentMapIndex: Int = 0

    val warnings = MutableLiveData<Warnings>()

    private var scope: CoroutineScope? = null
    val timer = MutableLiveData<Long>(0L)

    // ---------------------------------------------------------------------------------------------
    // Mapping service initiation and connection.
    // ---------------------------------------------------------------------------------------------

    fun connectMappingService(context: Context, ) {
        val intent = Intent(context, MappingService::class.java)
        context.startForegroundService(intent)
        context.bindService(intent, this, Context.BIND_AUTO_CREATE)
    }

    override fun onServiceConnected(p0: ComponentName?, binder: IBinder?) {
        val service = (binder as MappingService.MappingBinder).getService()
        mapper = service
        mappingServiceConnected.postValue(true)
    }

    override fun onServiceDisconnected(p0: ComponentName?) { }

    fun setPreviewView(preview: PreviewView) { mapper?.setSurface(preview.surfaceProvider) }

    // ---------------------------------------------------------------------------------------------
    // Public access (wrapper) to mapping service.
    // ---------------------------------------------------------------------------------------------

    fun setSavedRois(viewRois: List<MappingRoi>) { mapper?.setRois(viewRois) }

    fun getSavedRois(): List<MappingRoi> { return mapper?.getRois() ?: listOf() }

    fun startStop(): Boolean {
        return mapper?.let {
            warnings.postValue(it.startStop())
            val isCreating = it.isCreatingMaps()
            if(isCreating) startTimer() else stopTimer()
            isCreating
        } ?: false
    }

    fun isCreatingMaps(): Boolean { return mapper?.isCreatingMaps() ?: false }

    fun setCurrentMap(i: Int) {
        mapper?.let{ currentMapIndex = if(i < it.nMapCreators()) i else 0 }
    }

    fun currentMapCreator(): MapCreator? { return mapper?.getMapCreator(currentMapIndex) }

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

}
