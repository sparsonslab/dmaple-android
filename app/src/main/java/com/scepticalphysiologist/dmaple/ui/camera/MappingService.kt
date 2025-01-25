package com.scepticalphysiologist.dmaple.ui.camera

import android.app.NotificationChannel
import android.app.NotificationManager
import android.content.Context
import android.content.Intent
import android.content.pm.ServiceInfo
import android.os.Binder
import android.os.IBinder
import android.util.Rational
import android.view.Display
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
import androidx.core.app.NotificationCompat
import androidx.core.app.ServiceCompat
import androidx.lifecycle.LifecycleService
import androidx.lifecycle.MutableLiveData
import com.scepticalphysiologist.dmaple.ui.helper.Warnings
import java.time.Duration
import java.time.Instant
import java.util.concurrent.Executors

/** A foreground service that will run the camera and record spatio-temporal maps, irrespective
 * of whether the app is in he background or foreground.
 *
 * If CameraX is run directly from a ViewModel or Fragment it will stop recording after the app
 * has been in the background for a while.
 */
class MappingService: LifecycleService(), ImageAnalysis.Analyzer {

    // Camera
    // ------
    private var aspect = AspectRatio.RATIO_16_9

    private lateinit var cameraProvider: ProcessCameraProvider

    private lateinit var cameraUses: UseCaseGroup

    private lateinit var imageAnalysis: ImageAnalysis

    private lateinit var camera: Camera

    private lateinit var display: Display


    // State
    // -----
    private var recording: Boolean = false

    private var startTime: Instant? = null

    private var analysers = mutableListOf<GutAnalyser>()

    val upDateMap = MutableLiveData<Boolean>(false)

    val warnings = MutableLiveData<Warnings>()

    override fun onCreate() {
        super.onCreate()

        // Camera
        cameraProvider = ProcessCameraProvider.getInstance(this).get()
        cameraProvider.unbindAll()
        display = (this.getSystemService(Context.WINDOW_SERVICE) as WindowManager).defaultDisplay

        // Use cases.
        val useCaseGroup= UseCaseGroup.Builder()
        // ... view port
        val viewport = ViewPort.Builder(
            Rational(1, 1), Surface.ROTATION_0
        ).setScaleType(ViewPort.FIT).build()
        useCaseGroup.setViewPort(viewport)
        // ... preview
        val preview = Preview.Builder().setTargetAspectRatio(aspect).build()
        useCaseGroup.addUseCase(preview)
        // ... image analysis
        imageAnalysis = ImageAnalysis.Builder().setTargetAspectRatio(aspect).build()
        imageAnalysis.setAnalyzer(Executors.newSingleThreadExecutor(), this)
        useCaseGroup.addUseCase(imageAnalysis)
        // ... build
        cameraUses = useCaseGroup.build()

        // Bind to this lifecycle.
        camera = cameraProvider.bindToLifecycle(
            lifecycleOwner = this,
            cameraSelector = CameraSelector.DEFAULT_BACK_CAMERA,
            cameraUses
        )
    }

    // ---------------------------------------------------------------------------------------------
    // Binding access
    // ---------------------------------------------------------------------------------------------

    private val binder = MappingBinder()

    inner class MappingBinder: Binder() {
        fun getService(): MappingService { return this@MappingService }
    }

    override fun onBind(intent: Intent): IBinder? {
        super.onBind(intent)
        return binder
    }

    // ---------------------------------------------------------------------------------------------
    // Service start and stop.
    // ---------------------------------------------------------------------------------------------

    // Called from context.startForegroundService()
    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {

        // Notification and channel.
        val channelId = "MAPPING_CHANNEL"
        val channel = NotificationChannel(channelId, "Mapping", NotificationManager.IMPORTANCE_HIGH)
        (getSystemService(NOTIFICATION_SERVICE) as NotificationManager).createNotificationChannel(channel)
        val notification = NotificationCompat.Builder(this, channelId).also {
            it.setContentTitle("dMapLE")
            it.setContentText("Recording maps")
        }.build()

        // Start in foreground.
        ServiceCompat.startForeground(this, startId, notification, ServiceInfo.FOREGROUND_SERVICE_TYPE_CAMERA)


        return super.onStartCommand(intent, flags, startId)
    }

    // Called from context.stopService()
    override fun onDestroy() {
        // stop camera ????
        super.onDestroy()
    }

    // ---------------------------------------------------------------------------------------------
    // Analysis
    // ---------------------------------------------------------------------------------------------

    fun setPreview(pv: PreviewView) {
        cameraUses.useCases.filterIsInstance<Preview>().firstOrNull()?.let {
            it.surfaceProvider = pv.surfaceProvider
        }
    }

    fun getAnalyser(i: Int): GutAnalyser? { return if(i < analysers.size) analysers[i] else null }

    fun nAnalysers(): Int { return analysers.size }

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
