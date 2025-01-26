package com.scepticalphysiologist.dmaple.ui

import android.app.Application
import android.content.ComponentName
import android.content.Context
import android.content.Intent
import android.content.ServiceConnection
import android.os.IBinder
import androidx.camera.core.Preview.SurfaceProvider
import androidx.camera.view.PreviewView
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.MutableLiveData
import com.scepticalphysiologist.dmaple.ui.camera.MappingService
import com.scepticalphysiologist.dmaple.ui.camera.MapCreator
import com.scepticalphysiologist.dmaple.ui.camera.MappingRoi
import com.scepticalphysiologist.dmaple.ui.helper.Warnings


class RecorderModel(application: Application) :
    AndroidViewModel(application),
    ServiceConnection
{

    private val app = application

    private var mapper: MappingService? = null

    private var surface: SurfaceProvider? = null

    private var currentAnalyser: Int = 0

    val warnings = MutableLiveData<Warnings>()

    val upDateMap = MutableLiveData<Boolean>(false)

    fun startService(context: Context, preview: PreviewView) {
        this.surface = preview.surfaceProvider
        val intent = Intent(context, MappingService::class.java)
        context.startForegroundService(intent)
        context.bindService(intent, this, Context.BIND_AUTO_CREATE)
    }

    override fun onServiceConnected(p0: ComponentName?, binder: IBinder?) {
        val service = (binder as MappingService.MappingBinder).getService()
        surface?.let {service.setSurface(it)}
        service.ticker.observeForever { this.upDateMap.postValue(!this.upDateMap.value!!) }
        mapper = service
    }

    override fun onServiceDisconnected(p0: ComponentName?) { }

    override fun onCleared() {
        // Stop the mapping service.
        val intent = Intent(app, MappingService::class.java)
        app.stopService(intent)
    }

    // ---------------------------------------------------------------------------------------------
    // Public access to mapping service.
    // ---------------------------------------------------------------------------------------------

    fun startStop(rois: List<MappingRoi>? = null): Boolean {
        return mapper?.let {
            warnings.postValue(it.startStop(rois))
            it.isCreatingMaps()
        } ?: false
    }

    fun isRecording(): Boolean { return mapper?.isCreatingMaps() ?: false }

    fun elapsedSeconds(): Long { return mapper?.elapsedSeconds() ?: 0 }

    fun setCurrentAnalyser(i: Int) {
        mapper?.let{ currentAnalyser = if(i < it.nMapCreators()) i else 0 }
    }

    fun currentAnalyser(): MapCreator? { return mapper?.getMapCreator(currentAnalyser) }

}
