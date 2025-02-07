package com.scepticalphysiologist.dmaple.map

import android.app.NotificationChannel
import android.app.NotificationManager
import android.content.Context
import android.content.Intent
import android.content.pm.ServiceInfo
import android.hardware.camera2.CaptureRequest
import android.os.Binder
import android.os.IBinder
import android.view.Display
import android.view.WindowManager
import androidx.annotation.OptIn
import androidx.camera.camera2.interop.Camera2Interop
import androidx.camera.camera2.interop.ExperimentalCamera2Interop
import androidx.camera.core.AspectRatio
import androidx.camera.core.Camera
import androidx.camera.core.CameraSelector
import androidx.camera.core.ImageAnalysis
import androidx.camera.core.ImageProxy
import androidx.camera.core.Preview
import androidx.camera.core.Preview.SurfaceProvider
import androidx.camera.core.UseCase
import androidx.camera.lifecycle.ProcessCameraProvider
import androidx.core.app.NotificationCompat
import androidx.core.app.ServiceCompat
import androidx.lifecycle.LifecycleService
import com.scepticalphysiologist.dmaple.MainActivity
import com.scepticalphysiologist.dmaple.etc.Frame
import com.scepticalphysiologist.dmaple.etc.surfaceRotationDegrees
import com.scepticalphysiologist.dmaple.etc.Warnings
import com.scepticalphysiologist.dmaple.map.creator.BufferedExampleMap
import com.scepticalphysiologist.dmaple.map.creator.MapCreator
import java.io.File
import java.io.IOException
import java.io.RandomAccessFile
import java.nio.MappedByteBuffer
import java.nio.channels.FileChannel
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
@OptIn(ExperimentalCamera2Interop::class)
class MappingService: LifecycleService(), ImageAnalysis.Analyzer {

    companion object {

        // Camera
        // ------
        /** The aspect ratio of the camera. */
        const val CAMERA_ASPECT_RATIO = AspectRatio.RATIO_16_9
        /** Approximate interval between frames (milliseconds). */
        const val APPROX_FRAME_INTERVAL_MS: Long = 33

        // Map data buffering
        // ------------------
        /** A set of files in the app's storage used to create "mapped byte buffers" for
         * storing map data as it is created. The names of the files mapped to their random access
         * streams or null, if the file is not being used for buffering.
         *
         * Mapped byte buffers can be very large (up to 2GB) without taking up any actual
         * heap memory.
         * https://developer.android.com/reference/java/nio/MappedByteBuffer
         * */
        private val mapBuffers = mutableMapOf<String, RandomAccessFile?>(
            "buffer_1.dat" to null,
            "buffer_2.dat" to null,
            "buffer_3.dat" to null,
            "buffer_4.dat" to null,
            "buffer_5.dat" to null,
        )

        /** The size (bytes) of each buffering file listed in [mapBuffers].
         *
         * 100 MB ~= 60 min x 60 sec/min x 30 frame/sec x 1000 bytes/frame.
         * */
        private const val MAP_BUFFER_SIZE: Long = 100_000_000L

        /** Initialise the buffering files.
         *
         * This must be called by the main activity at creation.
         * */
        fun initialiseBuffers() {
            for((bufferFile, accessStream) in mapBuffers) {
                if(accessStream != null) continue
                val file = File(MainActivity.storageDirectory, bufferFile)
                if(!file.exists()) file.createNewFile()
                val fileSize = file.length()
                if(fileSize < MAP_BUFFER_SIZE) {
                    val strm = RandomAccessFile(file, "rw")
                    try { strm.setLength(MAP_BUFFER_SIZE) }
                    // ... in case there isn't enough memory available.
                    catch (_: IOException) {
                        strm.close()
                        return
                    }
                    strm.close()
                }
            }
        }

        /** Get a free buffer or null if no buffers are free. */
        fun getFreeBuffer(): MappedByteBuffer? {
            for((bufferFile, accessStream) in mapBuffers) {
                if(accessStream != null) continue
                val file = File(MainActivity.storageDirectory, bufferFile)
                val strm = RandomAccessFile(file, "rw")
                mapBuffers[bufferFile] = strm
                return strm.channel.map(FileChannel.MapMode.READ_WRITE, 0, file.length())
            }
            return null
        }

        /** Free all buffers. */
        fun freeAllBuffers() {
            for((bufferFile, accessStream) in mapBuffers) {
                accessStream?.channel?.close()
                accessStream?.close()
                mapBuffers[bufferFile] = null
            }
        }
    }

    // Camera
    // ------
    /** The camera provider. */
    private lateinit var cameraProvider: ProcessCameraProvider
    /** The camera. */
    private lateinit var camera: Camera
    /** The device display. */
    private lateinit var display: Display
    /** The camera preview. */
    private var preview: Preview? = null
    /** The view ("surface provider") of the camera preview. */
    private var surface: SurfaceProvider? = null
    /** The camera image analyser. */
    private var analyser: ImageAnalysis? = null

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
        display = (this.getSystemService(Context.WINDOW_SERVICE) as WindowManager).defaultDisplay
        cameraProvider = ProcessCameraProvider.getInstance(this).get()
        cameraProvider.unbindAll()
        setPreview(freeze = false)
        setAnalyser()
    }

    /** Set the preview.
     * @param freeze Turn off auto-focus, -exposure and -white-balance.
     * */
    private fun setPreview(freeze: Boolean) {
        unBindUse(preview)
        preview = Preview.Builder().also { builder ->
            builder.setTargetAspectRatio(CAMERA_ASPECT_RATIO)
            if(freeze) {
                val interOperator = Camera2Interop.Extender(builder)
                interOperator.setCaptureRequestOption(CaptureRequest.CONTROL_AWB_LOCK, true)
                interOperator.setCaptureRequestOption(CaptureRequest.CONTROL_AE_LOCK, true)
                interOperator.setCaptureRequestOption(CaptureRequest.CONTROL_AF_MODE, CaptureRequest.CONTROL_AF_MODE_OFF)
            }
        }.build().also { use ->
            surface?.let {s -> use.surfaceProvider = s}
            bindUse(use)
        }
    }

    private fun setAnalyser() {
        unBindUse(analyser)
        analyser = ImageAnalysis.Builder().also { builder ->
            builder.setTargetAspectRatio(CAMERA_ASPECT_RATIO)
            builder.setBackpressureStrategy(ImageAnalysis.STRATEGY_KEEP_ONLY_LATEST)
            builder.setImageQueueDepth(10)
        }.build().also { use ->
            use.setAnalyzer(Executors.newFixedThreadPool(5), this)
            bindUse(use)
        }
    }

    private fun unBindUse(use: UseCase?) {
        cameraProvider.unbind(use)
    }

    private fun bindUse(use: UseCase?) {
        camera = cameraProvider.bindToLifecycle(this, CameraSelector.DEFAULT_BACK_CAMERA, use)
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
    fun setSurface(provider: SurfaceProvider) {
        surface = provider
        preview?.surfaceProvider = provider
    }

    /** Set the camera exposure (as a fraction of the available range). */
    fun setExposure(fraction: Float) {
        val range = camera.cameraInfo.exposureState.exposureCompensationRange
        val exposure = (range.lower + fraction * (range.upper - range.lower)).toInt()
        camera.cameraControl.setExposureCompensationIndex(exposure)
    }

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
        val imageFrame = analyser?.let{imageAnalysisFrame(it)} ?: return warning
        creators = rois.mapNotNull { roi ->
            getFreeBuffer()?.let { buff -> BufferedExampleMap(roi.inNewFrame(imageFrame), buff) }
        }.toMutableList()
        if(creators.size < rois.size) {
            val nNotCreated = rois.size - creators.size
            val msg = "There are not enough buffers to process all maps.\n" +
                      "The last $nNotCreated maps will not be created."
            warning.add(msg, false)
        }


        // State
        setPreview(freeze = true)
        creating = true
        startTime = Instant.now()
        return warning
    }

    /** Stop creating maps. */
    private fun stop(): Warnings {
        val warnings = Warnings("Stop Recording")

        // Save and clear maps.
        for(creator in creators) creator.saveAndClose()
        creators.clear()

        // Free buffers.
        freeAllBuffers()
        System.gc()

        // State
        setPreview(freeze = false)
        creating = false
        startTime = null
        return warnings
    }

    /** Get the frame of the image analyser. */
    private fun imageAnalysisFrame(analyser: ImageAnalysis): Frame? {
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
