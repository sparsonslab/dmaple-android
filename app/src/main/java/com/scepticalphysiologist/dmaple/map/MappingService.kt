package com.scepticalphysiologist.dmaple.map

import android.app.NotificationChannel
import android.app.NotificationManager
import android.content.Context
import android.content.Intent
import android.content.pm.ServiceInfo
import android.graphics.Bitmap
import android.hardware.camera2.CameraCharacteristics
import android.hardware.camera2.CaptureRequest
import android.os.Binder
import android.os.Environment
import android.os.IBinder
import android.util.Range
import android.view.Display
import android.view.WindowManager
import androidx.annotation.OptIn
import androidx.camera.camera2.interop.Camera2CameraInfo
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
import com.scepticalphysiologist.dmaple.geom.Frame
import com.scepticalphysiologist.dmaple.geom.Point
import com.scepticalphysiologist.dmaple.geom.surfaceRotationDegrees
import com.scepticalphysiologist.dmaple.ui.msg.Warnings
import com.scepticalphysiologist.dmaple.etc.strftime
import com.scepticalphysiologist.dmaple.map.creator.MapCreator
import com.scepticalphysiologist.dmaple.map.buffer.MapBufferProvider
import com.scepticalphysiologist.dmaple.map.field.FieldImage
import com.scepticalphysiologist.dmaple.map.field.FieldRoi
import com.scepticalphysiologist.dmaple.map.field.FieldRuler
import com.scepticalphysiologist.dmaple.map.record.MappingRecord
import com.scepticalphysiologist.dmaple.map.field.RoisAndRuler
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.MainScope
import kotlinx.coroutines.launch
import java.io.File
import java.time.Duration
import java.time.Instant
import java.util.concurrent.Executors
import kotlin.system.measureTimeMillis

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
 * - View ROIs will be persisted across configuration changes other then rotation (which [RoiOverlay]
 * already handles).
 *
 */
@OptIn(ExperimentalCamera2Interop::class)
class MappingService: LifecycleService(), ImageAnalysis.Analyzer {

    companion object {
        /** The aspect ratio of the camera. */
        const val CAMERA_ASPECT_RATIO = AspectRatio.RATIO_16_9
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

    // Camera State
    // ------------
    /** Approximate frame rate (frames/second). */
    private var frameRateFps: Int = getAvailableFps().max()
    /** Approximate interval between frames (milliseconds). */
    private val frameIntervalMs: Long
        get() = (1000f / frameRateFps.toFloat()).toLong()
    /** Auto exposure, white-balance and focus are on. */
    private var autosOn: Boolean = true

    // Recording
    // ---------
    /** The mapping ROIs in their last view frame. */
    private var rois = mutableListOf<FieldRoi>()
    /** The measurement ruler in its last view frame. */
    private var ruler: FieldRuler? = null
    /** Map creators. */
    private var creators = mutableListOf<MapCreator>()
    /** The currently shown map: its [creators] index and map index within that creator. */
    private var currentMap: Pair<Int, Int> = Pair(0, 0)
    /** Maps are being created ("recording"). */
    private var creating: Boolean = false
    /** Provides file-mapped byte buffers for holding map data as it is created.
     * 10 buffers gives 10 spatio-temporal maps.
     * 100 MB ~= 60 min x 60 sec/min x 30 frame/sec x 1000 bytes/frame.
     * */
    val bufferProvider = MapBufferProvider(
        sourceDirectory = MainActivity.storageDirectory!!,
        nBuffers = 10,
        bufferByteSize = 100_000_000L
    )
    /** The instant that creation of maps started. */
    private var startTime: Instant = Instant.now()
    /** The last bitmap captured from the camera. */
    private var lastCapture: Bitmap? = null
    /** The coroutine scope for recording maps. */
    private var scope: CoroutineScope = MainScope()

    // ---------------------------------------------------------------------------------------------
    // Initiation
    // ---------------------------------------------------------------------------------------------

    /** Set-up the camera. */
    override fun onCreate() {
        super.onCreate()
        display = (this.getSystemService(Context.WINDOW_SERVICE) as WindowManager).defaultDisplay
        bufferProvider.initialiseBuffers()
        cameraProvider = ProcessCameraProvider.getInstance(this).get()
        cameraProvider.unbindAll()
        setPreview()
        setAnalyser()
    }

    /** Set the preview use case of CameraX. */
    private fun setPreview() {
        unBindUse(preview)
        preview = Preview.Builder().also { builder ->
            builder.setTargetAspectRatio(CAMERA_ASPECT_RATIO)
            // Frame rate.
            val inop = Camera2Interop.Extender(builder)
            inop.setCaptureRequestOption(
                CaptureRequest.CONTROL_AE_TARGET_FPS_RANGE, Range(frameRateFps, frameRateFps)
            )
            // Auto exposure and white-balance.
            if(!autosOn) {
                inop.setCaptureRequestOption(CaptureRequest.CONTROL_AWB_LOCK, true)
                inop.setCaptureRequestOption(CaptureRequest.CONTROL_AE_LOCK, true)
                inop.setCaptureRequestOption(CaptureRequest.CONTROL_AF_MODE, CaptureRequest.CONTROL_AF_MODE_OFF)
            }
        }.build().also { use ->
            surface?.let {s -> use.surfaceProvider = s}
            bindUse(use)
        }
    }

    /** Set the image analyser use case of CameraX. */
    private fun setAnalyser() {
        unBindUse(analyser)
        analyser = ImageAnalysis.Builder().also { builder ->
            builder.setTargetAspectRatio(CAMERA_ASPECT_RATIO)
            builder.setBackpressureStrategy(ImageAnalysis.STRATEGY_KEEP_ONLY_LATEST)
            builder.setImageQueueDepth(5)
        }.build().also { use ->
            use.setAnalyzer(Executors.newFixedThreadPool(5), this)
            bindUse(use)
        }
    }

    /** Bind a CameraX use case. */
    private fun bindUse(use: UseCase?) {
        camera = cameraProvider.bindToLifecycle(this, CameraSelector.DEFAULT_BACK_CAMERA, use)
    }

    /** Unbind a CameraX use case. */
    private fun unBindUse(use: UseCase?) { cameraProvider.unbind(use) }

    // ---------------------------------------------------------------------------------------------
    // Binding access to the service
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
        try { ServiceCompat.startForeground(
            this, startId, notification, ServiceInfo.FOREGROUND_SERVICE_TYPE_CAMERA
        ) }
        // These exceptions are thrown if camera permissions have not been given.
        catch(_: java.lang.SecurityException) {}
        catch(_: java.lang.RuntimeException) {}
        catch(_: android.os.RemoteException) {}

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

    /** Set the frame rate (frames per second). */
    fun setFps(fps: Int) {
        val available = getAvailableFps()
        frameRateFps = if(fps in available) fps else available.max()
        setPreview()
    }

    /** Get the available frame rates (frames per second). */
    fun getAvailableFps(): List<Int> {
        if(::camera.isInitialized) {
            Camera2CameraInfo.from(camera.cameraInfo).getCameraCharacteristic(
                CameraCharacteristics.CONTROL_AE_AVAILABLE_TARGET_FPS_RANGES
            )?.let { ranges ->
                return ranges.filter { it.lower == it.upper }.map{ it.lower }
            }
        }
        return listOf(30)
    }

    /** Get the pixel width and height of the mapping field. */
    fun getFieldSize(): Point? {
        return analyser?.resolutionInfo?.let { info ->
            Point(info.resolution.width.toFloat(), info.resolution.height.toFloat())
        }
    }

    /** Set the field's ROIs and ruler. */
    fun setRoisAndRuler(field: RoisAndRuler) {
        rois.clear()
        for(roi in field.rois) rois.add(roi.copy())
        ruler = field.ruler
    }

    /** Get the field's ROIs and ruler. e.g. for when a view of the field needs to be reconstructed. */
    fun getRoisAndRuler(): RoisAndRuler { return RoisAndRuler(rois, ruler) }

    /** Start or stop map creation, depending on the current creation state. */
    fun startStop(): Warnings { return if(creating) stop() else start() }

    /** Is the mapping service currently creating maps? */
    fun isCreatingMaps(): Boolean { return creating }

    /** The number of seconds since the service started mapping. */
    fun elapsedSeconds(): Long {
        return (Duration.between(startTime, Instant.now()).toMillis() / 1000f).toLong()
    }

    /** Get current map's creator and map index. */
    fun getCurrentMapCreator(): Pair<MapCreator?, Int> {
        val (currentCreatorIdx, currentMapIdx) = currentMap
        if(currentCreatorIdx < creators.size) return Pair(creators[currentCreatorIdx], currentMapIdx)
        return Pair(null, 0)
    }

    /** Get the last image of the mapping field. */
    fun getLastFieldImage(): FieldImage? {
        return lastCapture?.let { bitmap ->
            // We can assume that the last captured bitmap is in the same frame as a current creator.
            creators.firstOrNull()?.roi?.frame?.let { frame -> FieldImage(frame, bitmap) }
        }
    }

    /** Given a selected ROI, set the next map to show. */
    fun setNextMap(roiUID: String) { currentMap = getNextMap(roiUID) }

    /** Load a (old) recording.
     *
     * @param record The record to be loaded.
     * @return Whether the record was loaded (will not be loaded if a record is being created).
     * */
    fun loadRecord(record: MappingRecord): Boolean {
        if(creating) return false
        setRoisAndRuler(RoisAndRuler(record.creators.map { it.roi }, null))
        clearCreators()
        record.loadMapTiffs(bufferProvider::getFreeBuffer)
        creators.addAll(record.creators)
        lastCapture = record.field
        currentMap = Pair(0, 0)
        return true
    }

    /** If maps are not being created, save the maps, clear the creators and free-up resources.
     *
     * @param folderSuffix A suffix given to the folder containing all maps from a recording. The
     * folder begins with the date and time.
     * */
    fun saveAndClear(folderSuffix: String?) = scope.launch(Dispatchers.Default) {
        if(creating) return@launch
        folderSuffix?.let {
            val loc = File(
                Environment.getExternalStoragePublicDirectory(Environment.DIRECTORY_DOCUMENTS),
                strftime(startTime, "YYMMdd_HHmmss_") + it
            )
            MappingRecord(loc, lastCapture, creators).write()
            MappingRecord.read(loc)?.let {MappingRecord.records.add(it)}
        }
        clearCreators()
    }

    // ---------------------------------------------------------------------------------------------
    // Map creation
    // ---------------------------------------------------------------------------------------------

    /** Start creating maps from a set of ROIs. */
    private fun start(): Warnings {
        // Preliminary checks.
        val warning = Warnings("Start Recording")
        if(creators.isNotEmpty()) warning.add(message =
            "Maps are still being saved.\n" +
            "Please wait.",
            causesStop = true
        )
        if(rois.isEmpty()) warning.add(message =
            "There are no areas to map (dashed rectangles).\n" +
            "Make a mapping area by double tapping a selection.",
            causesStop = true
        )
        if(warning.shouldStop()) return warning

        // Create map creators.
        val imageFrame = imageAnalysisFrame() ?: return warning
        val nBuffersRequired = rois.sumOf { roi -> roi.maps.sumOf { map -> map.nMaps } }
        if(nBuffersRequired > bufferProvider.nFreeBuffers()) {
            warning.add(message =
                "There are not enough buffers to process all maps.\n" +
                "Please reduce the number of ROIs or map types selected.",
                causesStop = false
            )
            return warning
        }
        for(roi in rois) {
            val creator = MapCreator(roi.inNewFrame(imageFrame))
            ruler?.let{ creator.setSpatialResolutionFromRuler(it) }
            creator.setTemporalResolutionFromFPS(frameRateFps.toFloat())
            val buffers = (0 until creator.nMaps).map{ bufferProvider.getFreeBuffer()}.filterNotNull()
            if(creator.provideBuffers(buffers)) creators.add(creator)
        }

        // State
        autosOn = false
        setPreview()
        creating = true
        startTime = Instant.now()
        return warning
    }

    /** Stop creating maps. */
    private fun stop(): Warnings {
        // State
        creating = false
        autosOn = true
        setCreatorTemporalRes()
        setPreview()
        return Warnings("Stop Recording")
    }

    /** Set the temporal resolution of the creators based upon
     * the time from the start of the recording to now. */
    private fun setCreatorTemporalRes() {
        // todo - this is not very accurate for short recordings because the frame interval
        //     often jitters to very long values at the start of the recording.
        val dur = 0.001f * Duration.between(startTime, Instant.now()).toMillis().toFloat()
        for(creator in creators) creator.setTemporalResolutionFromDuration(dur)
    }

    /** Clear all creators, freeing up their buffers and resetting the current map. */
    private fun clearCreators() {
        if(creating) return
        creators.clear()
        currentMap = Pair(0, 0)
        bufferProvider.freeAllBuffers()
        System.gc()
    }

    /** Get the frame of the image analyser. */
    private fun imageAnalysisFrame(): Frame? {
        // The frame orientation is the sum of the ImageInfo and analyser target.
        // From the definition of ImageAnalysis.targetRotation:
        // "The rotation value of ImageInfo will be the rotation, which if applied to the output
        //  image [of the analyser], will make the image match [the] target rotation specified here."
        // https://developer.android.com/reference/androidx/camera/core/ImageAnalysis.Builder#setTargetRotation(int)
        // The values for the Samsung SM-X110 are:
        // target = display    image info      sum
        // -----------------------------------------
        //  0                   90              90
        //  90                  0               90
        //  180                 270             450
        //  270                 180             450
        analyser?.let {
            it.targetRotation = display.rotation
            val imageInfo = it.resolutionInfo ?: return null
            val or = imageInfo.rotationDegrees + surfaceRotationDegrees(it.targetRotation)
            return Frame(
                Point(imageInfo.resolution.width.toFloat(), imageInfo.resolution.height.toFloat()),
                orientation = or
            )
        } ?: return null
    }

    /** Analyse each image from the camera feed. Called continuously during the life of the service. */
    override fun analyze(image: ImageProxy) {
        // Uses the pattern given in
        // https://stackoverflow.com/questions/61252975/how-to-decrease-frame-rate-of-android-camerax-imageanalysis
        // to keep the frame-rate fairly constant.
        val elapsed = measureTimeMillis {
            if(creating) {
                // Pass the image to each map creator to analyse.
                lastCapture = image.toBitmap()
                // todo - would be faster to have creators access image pixels directly rather
                //    than convert the whole image to a bitmap. But getting pixels out of image
                //    is a pain (decoding, planes, etc) -  I did try!
                for(creator in creators) creator.updateWithCameraBitmap(lastCapture!!)
            }
        }
        image.use {
            if(elapsed < frameIntervalMs) Thread.sleep(frameIntervalMs - elapsed)
        }
    }

    // ---------------------------------------------------------------------------------------------
    // ROIs and maps
    // ---------------------------------------------------------------------------------------------

    /** Given a selected ROI, get the next map to show. */
    private fun getNextMap(roiUID: String): Pair<Int, Int> {
        val (currentCreatorIdx, currentMapIdx) = currentMap
        // Indices of creators associated with the selected ROI.
        val creatorsFromRoi = creators.indices.filter { creators[it].roi.uid == roiUID }
        // No creators matching the ROI: the first creator and its first map.
        if(creatorsFromRoi.isEmpty()) return Pair(0, 0)
        // Current creator does not come from the ROI: The first creator associate with the ROI.
        if(currentCreatorIdx !in creatorsFromRoi) return Pair(creatorsFromRoi[0], 0)
        // Current creator does come from the ROI ...
        // ... and it has more maps to show: the next map from the same creator
        if(currentMapIdx < creators[currentCreatorIdx].nMaps - 1)
            return Pair(currentCreatorIdx, currentMapIdx + 1)
        // ... and it does not have more maps: the next creator associated with the ROI.
        var j = 1 + creatorsFromRoi.indexOf(currentCreatorIdx)
        if(j >= creatorsFromRoi.size) j = 0
        return Pair(creatorsFromRoi[j], 0)
    }

}
