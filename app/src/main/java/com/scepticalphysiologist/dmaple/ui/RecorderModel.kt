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

    /** The index (in [mapper]'s map creators) of the current map to be shown. */
    private var currentMapIndex: Int = 0

    val warnings = MutableLiveData<Warnings>()

    val upDateMap = MutableLiveData<Boolean>(false)

    // ---------------------------------------------------------------------------------------------
    // Mapping service initiation and connection.
    // ---------------------------------------------------------------------------------------------

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
    // Public access (wrapper) to mapping service.
    // ---------------------------------------------------------------------------------------------

    fun startStop(rois: List<MappingRoi>? = null): Boolean {
        return mapper?.let {
            warnings.postValue(it.startStop(rois))
            it.isCreatingMaps()
        } ?: false
    }

    fun isCreatingMaps(): Boolean { return mapper?.isCreatingMaps() ?: false }

    fun elapsedSeconds(): Long { return mapper?.elapsedSeconds() ?: 0 }

    fun setCurrentMap(i: Int) {
        mapper?.let{ currentMapIndex = if(i < it.nMapCreators()) i else 0 }
    }

    fun currentMapCreator(): MapCreator? { return mapper?.getMapCreator(currentMapIndex) }

}
