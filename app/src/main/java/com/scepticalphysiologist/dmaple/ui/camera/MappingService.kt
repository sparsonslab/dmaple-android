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
import androidx.camera.core.Preview.SurfaceProvider
import androidx.camera.core.UseCaseGroup
import androidx.camera.core.ViewPort
import androidx.camera.lifecycle.ProcessCameraProvider
import androidx.core.app.NotificationCompat
import androidx.core.app.ServiceCompat
import androidx.lifecycle.LifecycleService
import com.scepticalphysiologist.dmaple.MainActivity
import com.scepticalphysiologist.dmaple.ui.helper.Warnings
import java.time.Duration
import java.time.Instant
import java.util.concurrent.Executors

/** A foreground service that will run the camera, record spatio-temporal maps and keep ROI state.
 *
 * It does this for however long the app is "open", is a android "task". i.e. it will never stop
 * in response to:
 * - Changes in current activity or fragment.
 * - Backgrounding of the app (which typically destroys all activities after a few minutes).
 * - Sleep of the app (which typically destroys all activities after a few minutes).
 *
 * This is vital as:
 * - The user may want to record maps for tens-of-minutes or even hours, while putting the app in
 * the background to save battery.
 * - View ROIs will be persisted across configuration changes other then rotation (which [MappingRoiOverlay]
 * already handles).
 *
 */
class MappingService: LifecycleService(), ImageAnalysis.Analyzer {

    companion object {

        /** The aspect ratio of the camera. */
        const val CAMERA_ASPECT_RATIO = AspectRatio.RATIO_16_9

        /** Approximate interval between frames (milliseconds). */
        const val APPROX_FRAME_INTERVAL_MS: Long = 33

        const val APPROX_FRAME_RATE_HZ: Float = 1000f / APPROX_FRAME_INTERVAL_MS.toFloat()

    }

    // Camera
    // ------
    /** The camera. */
    private lateinit var camera: Camera
    /** The device display. */
    private lateinit var display: Display
    /** The camera preview. */
    private lateinit var preview: Preview
    /** The camera image analyser. */
    private lateinit var analyser: ImageAnalysis

    // State
    // -----
    /** The mapping ROIs in their last view frame. */
    private var rois = mutableListOf<MappingRoi>()
    /** Map creators. */
    private var creators = mutableListOf<MapCreator>()
    /** Maps are being created ("recording"). */
    private var creating: Boolean = false
    /** The instant that creation of maps started. */
    private var startTime: Instant? = null

    /** Set-up the camera. */
    override fun onCreate() {
        super.onCreate()

        // Camera provider and display.
        val cameraProvider = ProcessCameraProvider.getInstance(this).get()
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
        preview = Preview.Builder().setTargetAspectRatio(CAMERA_ASPECT_RATIO).build()
        useCaseGroup.addUseCase(preview)
        // ... image analysis
        // todo - bind/unbind during map creation start/stop.
        analyser =ImageAnalysis.Builder().also {
            it.setTargetAspectRatio(CAMERA_ASPECT_RATIO)
            it.setBackpressureStrategy(ImageAnalysis.STRATEGY_KEEP_ONLY_LATEST)
            it.setImageQueueDepth(10)
        }.build()
        analyser.setAnalyzer(Executors.newFixedThreadPool(5), this)
        useCaseGroup.addUseCase(analyser)

        // Bind to this lifecycle.
        camera = cameraProvider.bindToLifecycle(
            lifecycleOwner = this,
            cameraSelector = CameraSelector.DEFAULT_BACK_CAMERA,
            useCaseGroup = useCaseGroup.build()
        )

    }

    // ---------------------------------------------------------------------------------------------
    // Binding access
    // ---------------------------------------------------------------------------------------------

    private val binder = MappingBinder()

    inner class MappingBinder: Binder() {
        fun getService(): MappingService { return this@MappingService }
    }

    override fun onBind(intent: Intent): IBinder {
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

        // Start this service the foreground.
        ServiceCompat.startForeground(
            this, startId, notification, ServiceInfo.FOREGROUND_SERVICE_TYPE_CAMERA
        )
        return super.onStartCommand(intent, flags, startId)
    }

    /** Stop the service when the app is "closed" (task removed) by the user. */
    override fun onTaskRemoved(rootIntent: Intent?) {
        this.stopService(rootIntent)
        // TODO - save maps.
        super.onTaskRemoved(rootIntent)
    }

    // ---------------------------------------------------------------------------------------------
    // Public interface
    // ---------------------------------------------------------------------------------------------

    /** Set the surface provider (physical view) for the camera preview. */
    fun setSurface(provider: SurfaceProvider) { preview.surfaceProvider = provider }

    /** Set the ROIs. e.g. when ROIs are updated in a view. */
    fun setRois(viewRois: List<MappingRoi>) {
        rois.clear()
        for(roi in viewRois) rois.add(roi.copy())
    }

    /** Get the ROIs. e.g. for when a view of the ROIs needs to be reconstructed. */
    fun getRois(): List<MappingRoi> { return rois }

    /** Start or stop map creation, depending on the current creation state. */
    fun startStop(): Warnings { return if(creating) stop() else start() }

    /** Is the mapping service currently creating maps? */
    fun isCreatingMaps(): Boolean { return creating }

    /** Get the ith map creator. */
    fun getMapCreator(i: Int): MapCreator? { return if(i < creators.size) creators[i] else null }

    /** The number of map creators. */
    fun nMapCreators(): Int { return creators.size }

    /** The number of seconds since the service started mapping. */
    fun elapsedSeconds(): Long {
        return startTime?.let {
            (Duration.between(it, Instant.now()).toMillis() / 1000f).toLong()
        } ?: 0
    }

    // ---------------------------------------------------------------------------------------------
    // Map creation
    // ---------------------------------------------------------------------------------------------

    /** Start creating maps from a set of ROIs. */
    private fun start(): Warnings {
        // No reason to start if there are no mapping ROIs.
        val warning = Warnings("Start Recording")
        if(rois.isEmpty()) {
            val msg = "There are no areas to map (dashed rectangles).\n" +
                    "Make a mapping area by double tapping a selection."
            warning.add(msg, true)
            return warning
        }

        // Create map creators.
        // todo - MappingRois should include the MapCreator class to which they are used for.
        val imageFrame = imageAnalysisFrame() ?: return warning
        creators = rois.map { SubstituteMapCreator(it.inNewFrame(imageFrame)) }.toMutableList()

        // Allocate creator buffering and back-up.
        val (timeSamples, writeFraction) = timeSampleAllocation(
            bytesPerTimeSample = creators.map{it.bytesPerTimeSample()}.sum(),
            maxBufferingMinutes = 10f
        )
        for(creator in creators) creator.allocateBufferAndBackUp(timeSamples, writeFraction)
        val bufferMin = String.format("%.1f", timeSamples.toFloat() / (60f  * APPROX_FRAME_RATE_HZ))
        warning.add("Maps will viewable live to ~$bufferMin minutes", false)

        // State
        creating = true
        startTime = Instant.now()
        return warning
    }

    /** Calculate the number of time samples that be allocated to buffer memory of the creators
     * and the fraction of these time samples that will be written at each buffer-write event.
     *
     * @param bytesPerTimeSample The number of bytes required per time sample across all maps.
     * @param maxBufferingMinutes The maximum length of time for which buffer memory should be allocated.
     * */
    private fun timeSampleAllocation(
        bytesPerTimeSample: Int, maxBufferingMinutes: Float
    ): Pair<Int, Float> {
        // Allocate to the maps at most 80% of the current free memory.
        //val maxAllocationBytes = 0.8f * MainActivity.freeBytes().toFloat()
        // or 20% of the memory allocated to the app.
        val maxAllocatedBytes = 0.2f * MainActivity.allocatedBytes(this).toFloat()
        println("allocated mem = $maxAllocatedBytes, frate = $APPROX_FRAME_RATE_HZ")

        // The number of time samples buffered should be the minimum of ...
        val timeSamples = minOf(
            // ... the number that takes up the byte allocation
            (maxAllocatedBytes / bytesPerTimeSample).toInt(),
            // ... the number that takes the maximum time allowed.
            (maxBufferingMinutes * 60f * APPROX_FRAME_RATE_HZ).toInt()
        )

        // The fraction of time samples written everytime the buffer reaches near-capacity.
        // The minimum of 20 seconds or 0.2.
        // Short (at most 20 second) writes will improve performance.
        val fileWriteFraction = minOf(20f * APPROX_FRAME_RATE_HZ /  timeSamples, 0.2f)
        return Pair(timeSamples, fileWriteFraction)
    }

    /** Stop creating maps. */
    private fun stop(): Warnings {
        val warnings = Warnings("Stop Recording")

        // Save and clear maps.
        for(creator in creators) creator.saveAndClose()
        creators.clear()

        // State
        creating = false
        startTime = null
        return warnings
    }

    /** Get the frame of the image analyser. */
    private fun imageAnalysisFrame(): Frame? {
        // Get analyser and update its target orientation.
        analyser.targetRotation = display.rotation
        // The frame orientation is the sum of the image and analyser target.
        // (see the definition of ImageAnalysis.targetRotation)
        val imageInfo = analyser.resolutionInfo ?: return null
        val or = imageInfo.rotationDegrees + surfaceRotationDegrees(analyser.targetRotation)
        return Frame(
            width=imageInfo.resolution.width.toFloat(),
            height = imageInfo.resolution.height.toFloat(),
            orientation = or
        )
    }

    /** Analyse each image from the camera feed. Called continuously during the life of the service. */
    override fun analyze(image: ImageProxy) {
        if(creating) {
            // Pass the image to each map creator to analyse.
            val bm = image.toBitmap()
            // todo - would be faster to have creators access image pixels directly rather
            //    than convert the whole image to a bitmap. But getting pixels out of image
            //    is a pain (decoding, planes, etc) -  I did try!
            for(creator in creators) creator.updateWithCameraBitmap(bm)
        }
        // Each image must be "closed" to allow preview to continue.
        image.close()

        // Wait for a while before grabbing the next frame.
        Thread.sleep(APPROX_FRAME_INTERVAL_MS)
    }

}
