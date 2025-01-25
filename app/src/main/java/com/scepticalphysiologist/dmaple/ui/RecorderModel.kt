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
import com.scepticalphysiologist.dmaple.ui.camera.GutAnalyser
import com.scepticalphysiologist.dmaple.ui.camera.MappingRoi


class RecorderModel(application: Application) :
    AndroidViewModel(application),
    ServiceConnection
{

    private val app = application

    private var service: MappingService? = null

    private var preview: PreviewView? = null

    private var currentAnalyser: Int = 0

    val upDateMap = MutableLiveData<Boolean>(false)

    fun startService(context: Context, preview: PreviewView) {
        this.preview = preview
        val intent = Intent(context, MappingService::class.java)
        context.startForegroundService(intent)
        context.bindService(intent, this, Context.BIND_AUTO_CREATE)
    }

    override fun onServiceConnected(p0: ComponentName?, binder: IBinder?) {
        val serv = (binder as MappingService.MappingBinder).getService()
        preview?.let {serv.setPreview(it)}
        serv.upDateMap.observeForever { this.upDateMap.postValue(!this.upDateMap.value!!) }
        service = serv
    }

    override fun onServiceDisconnected(p0: ComponentName?) { }

    override fun onCleared() {
        val intent = Intent(app, MappingService::class.java)
        app.stopService(intent)
    }

    // ---------------------------------------------------------------------------------------------
    // Public access to mapping service.
    // ---------------------------------------------------------------------------------------------

    fun startStop(rois: List<MappingRoi>? = null): Boolean { return service?.startStop(rois) ?: false }

    fun isRecording(): Boolean { return service?.isRecording() ?: false }

    fun elapsedSeconds(): Long { return service?.elapsedSeconds() ?: 0 }

    fun setCurrentAnalyser(i: Int) {
        service?.let{ currentAnalyser = if(i < it.nAnalysers()) i else 0 }
    }

    fun currentAnalyser(): GutAnalyser? { return service?.getAnalyser(currentAnalyser) }

}
