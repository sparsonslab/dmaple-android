package com.scepticalphysiologist.dmaple.ui.camera

import android.annotation.SuppressLint
import android.content.Context
import android.util.Rational
import android.view.Surface
import android.view.WindowManager
import androidx.camera.core.AspectRatio
import androidx.camera.core.Camera
import androidx.camera.core.CameraSelector
import androidx.camera.core.ImageAnalysis
import androidx.camera.core.ImageProxy
import androidx.camera.core.Preview
import androidx.camera.core.UseCaseGroup
import androidx.camera.core.ViewPort
import androidx.camera.lifecycle.ProcessCameraProvider
import androidx.camera.view.PreviewView
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.LifecycleOwner
import androidx.lifecycle.LifecycleRegistry
import androidx.lifecycle.MutableLiveData
import com.scepticalphysiologist.dmaple.ui.helper.Warnings
import java.time.Duration
import java.time.Instant
import java.util.concurrent.Executors

class CameraAnalyser(context: Context):
    ImageAnalysis.Analyzer,
    LifecycleOwner
{

    private val display = (context.getSystemService(Context.WINDOW_SERVICE) as WindowManager).defaultDisplay

    // Camera
    // ------
    private var aspect = AspectRatio.RATIO_16_9

    private val cameraProvider: ProcessCameraProvider

    private val cameraUses: UseCaseGroup


    private val imageAnalysis: ImageAnalysis

    private val camera: Camera

    // State
    // -----
    private var recording: Boolean = false

    private var startTime: Instant? = null

    private var analysers = mutableListOf<GutAnalyser>()

    val upDateMap = MutableLiveData<Boolean>(false)

    val warnings = MutableLiveData<Warnings>()

    // Lifecycle
    // ---------
    private val lifecycleRegistry: LifecycleRegistry = LifecycleRegistry(this)

    override val lifecycle: Lifecycle
        get() = lifecycleRegistry



    init {

        lifecycleRegistry.currentState = Lifecycle.State.STARTED

        // Camera
        cameraProvider = ProcessCameraProvider.getInstance(context).get()
        cameraProvider.unbindAll()


        val useCaseGroup= UseCaseGroup.Builder()

        val viewport = ViewPort.Builder(
            Rational(1, 1), Surface.ROTATION_0
        ).setScaleType(ViewPort.FIT).build()
        useCaseGroup.setViewPort(viewport)

        val preview = Preview.Builder().setTargetAspectRatio(aspect).build()
        useCaseGroup.addUseCase(preview)

        imageAnalysis = ImageAnalysis.Builder().setTargetAspectRatio(aspect).build()
        imageAnalysis.setAnalyzer(Executors.newSingleThreadExecutor(), this)

        useCaseGroup.addUseCase(imageAnalysis)

        cameraUses = useCaseGroup.build()

        // Bind to this lifecycle.
        camera = cameraProvider.bindToLifecycle(
            lifecycleOwner = this,
            cameraSelector = CameraSelector.DEFAULT_BACK_CAMERA,
            cameraUses
        )

    }


    // ---------------------------------------------------------------------------------------------
    // Public
    // ---------------------------------------------------------------------------------------------

    @SuppressLint("RestrictedApi")
    fun setPreviewView(preview: PreviewView) {
        cameraUses.useCases.filterIsInstance<Preview>().firstOrNull()?.let {
            it.surfaceProvider = preview.surfaceProvider
        }
    }

    fun getAnalyser(i: Int): GutAnalyser? { return if(i < analysers.size) analysers[i] else null }

    fun startStop(rois: List<MappingRoi>? = null): Boolean {
        warnings.postValue(if(recording) stop() else start(rois))
        return recording
    }

    fun isRecording(): Boolean { return recording }

    fun elapsedSeconds(): Long {
        if(startTime == null) return 0
        return (Duration.between(startTime, Instant.now()).toMillis() / 1000f).toLong()
    }


    private fun start(rois: List<MappingRoi>?): Warnings {

        // Cannot not start if there are no ROIs.
        val warning = Warnings("Start Recording")
        if(rois.isNullOrEmpty()) {
            val msg = "There are no areas to map (dashed rectangles).\n" +
                    "Make a mapping area by double tapping a selection."
            warning.add(msg, true)
            return warning
        }

        // Convert the frame of the mapping ROIs to the frame of the camera.
        val imageFrame = imageAnalyserFrame() ?: return warning
        analysers = rois.map {it.inNewFrame(imageFrame)}.map{GutMapper(it)}.toMutableList()

        // State
        recording = true
        startTime = Instant.now()
        return warning
    }

    private fun stop(): Warnings {
        val warnings = Warnings("Stop Recording")

        // Save maps.
        // ?????????

        // State
        analysers.clear()
        recording = false
        return warnings
    }

    private fun imageAnalyserFrame(): Frame? {
        // Get analyser and update its target orientation.
        imageAnalysis.targetRotation = display.rotation

        val imageInfo = imageAnalysis.resolutionInfo ?: return null
        val or = imageInfo.rotationDegrees + surfaceRotationDegrees(imageAnalysis.targetRotation)
        return Frame(
            width=imageInfo.resolution.width.toFloat(),
            height = imageInfo.resolution.height.toFloat(),
            orientation = or
        )
    }


    override fun analyze(image: ImageProxy) {
        // Not recording.
        if(!recording) {
            image.close() // Image must be "closed" to allow preview to continue.
            return
        }

        // Analyse each mapping area.
        val bm = image.toBitmap()
        for(analyser in analysers) analyser.analyse(bm)

        // set live data object to indicate update to view/fragment
        upDateMap.postValue(!upDateMap.value!!)

        // Image must be "closed" to allow preview to continue.
        image.close()
    }

}