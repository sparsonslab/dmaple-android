package com.scepticalphysiologist.dmaple.ui

import android.app.Application
import androidx.camera.view.PreviewView
import androidx.lifecycle.AndroidViewModel
import com.scepticalphysiologist.dmaple.ui.camera.CameraAnalyser
import com.scepticalphysiologist.dmaple.ui.camera.GutAnalyser
import com.scepticalphysiologist.dmaple.ui.camera.MappingRoi


class RecorderModel(application: Application) : AndroidViewModel(application) {


    val cameraAnalyser = CameraAnalyser(application.baseContext)

    val time: Int = 0


    var rois = listOf<MappingRoi>()


    // ---------------------------------------------------------------------------------------------
    // Public access to camera analyser
    // ---------------------------------------------------------------------------------------------

    fun setCameraPreview(preview: PreviewView) { cameraAnalyser.setPreviewView(preview) }

    fun startStop(rois: List<MappingRoi>? = null): Boolean { return cameraAnalyser.startStop(rois) }

    fun isRecording(): Boolean { return cameraAnalyser.isRecording() }

    fun elapsedSeconds(): Long { return cameraAnalyser.elapsedSeconds() }

    fun getAnalyser(i: Int): GutAnalyser? { return cameraAnalyser.getAnalyser(i) }

    // ---------------------------------------------------------------------------------------------
    //
    // ---------------------------------------------------------------------------------------------


}