// Copyright (c) 2025-2025. Dr Sean Paul Parsons. All rights reserved.

package com.scepticalphysiologist.dmaple.map

import android.app.NotificationChannel
import android.app.NotificationManager
import android.content.Intent
import android.content.pm.ServiceInfo
import android.os.Binder
import android.os.IBinder
import android.util.Log
import androidx.camera.core.AspectRatio
import androidx.camera.core.ImageAnalysis
import androidx.camera.core.ImageProxy
import androidx.camera.core.Preview.SurfaceProvider
import androidx.core.app.NotificationCompat
import androidx.core.app.ServiceCompat
import androidx.lifecycle.LifecycleService
import com.scepticalphysiologist.dmaple.etc.CountedPath
import com.scepticalphysiologist.dmaple.geom.Point
import com.scepticalphysiologist.dmaple.ui.dialog.Warnings
import com.scepticalphysiologist.dmaple.map.creator.MapCreator
import com.scepticalphysiologist.dmaple.map.buffer.MapBufferProvider
import com.scepticalphysiologist.dmaple.map.camera.CameraService
import com.scepticalphysiologist.dmaple.map.camera.FrameTimer
import com.scepticalphysiologist.dmaple.map.field.FieldImage
import com.scepticalphysiologist.dmaple.map.creator.FieldParams
import com.scepticalphysiologist.dmaple.map.creator.LumaReader
import com.scepticalphysiologist.dmaple.map.field.FieldRoi
import com.scepticalphysiologist.dmaple.map.field.FieldRuler
import com.scepticalphysiologist.dmaple.map.record.MappingRecord
import com.scepticalphysiologist.dmaple.map.field.RoisAndRuler
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.MainScope
import kotlinx.coroutines.launch
import kotlinx.coroutines.runBlocking
import java.io.File
import kotlin.math.abs

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
class MappingService: LifecycleService(), ImageAnalysis.Analyzer {

    companion object {
        /** The aspect ratio of the camera. */
        const val CAMERA_ASPECT_RATIO = AspectRatio.RATIO_16_9
        /** Automatically save any live recording when the app is closed. */
        var AUTO_SAVE_ON_CLOSE: Boolean = false
    }

    // Sub-services
    // ------------
    /** Controls the camera (use cases, focus, exposure, etc.). */
    private lateinit var camera: CameraService
    /** Timer for marking recording duration and frame intervals. */
    private val timer = FrameTimer()
    /** Read luminance values from the camera. */
    private var imageReader: LumaReader = LumaReader()
    /** Provides file-mapped byte buffers for holding map data as it is created. */
    private var bufferProvider = MapBufferProvider(File(""), 0, 0)

    // Field
    // -----
    /** The mapping ROIs in their last view frame. */
    private var rois = mutableListOf<FieldRoi>()
    /** The measurement ruler in its last view frame. */
    private var ruler: FieldRuler? = null

    // Recording State
    // ---------------
    /** Maps are being created ("recording"). */
    private var creating: Boolean = false
    /** Map creators. */
    private var creators = mutableListOf<MapCreator>()
    /** The currently shown map: its [creators] index and map index within that creator. */
    private var currentMap: Pair<Int, Int> = Pair(0, 0)
    /** The coroutine scope for recording maps. */
    private var scope: CoroutineScope = MainScope()

    // ---------------------------------------------------------------------------------------------
    // Initiation
    // ---------------------------------------------------------------------------------------------

    /** Set-up the camera. */
    override fun onCreate() {
        super.onCreate()
        Log.i("dmaple_lifetime", "mapping service: onCreate")
        camera = CameraService(
            context = this, analyser = this, aspectRatio = CAMERA_ASPECT_RATIO, owner = this
        )
    }

    // ---------------------------------------------------------------------------------------------
    // Binding access to the service
    // ---------------------------------------------------------------------------------------------

    private val binder = MappingBinder()

    inner class MappingBinder: Binder() {
        fun getService(): MappingService { return this@MappingService }
    }

    override fun onBind(intent: Intent): IBinder {
        super.onBind(intent)
        Log.i("dmaple_lifetime", "mapping service: onBind")
        return binder
    }

    // ---------------------------------------------------------------------------------------------
    // Service start and stop.
    // ---------------------------------------------------------------------------------------------

    // Called from context.startForegroundService()
    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        Log.i("dmaple_lifetime", "mapping service: onStartCommand")
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

    /** Stop the service when the app is "closed" (task removed) by the user.
     *
     * This can also be achieved in the service's entry in the AndroidManifest.xml by
     * android:stopWithTask="true"
     * But here we can allow automated saving, clean-up, etc.
     * */
    override fun onTaskRemoved(rootIntent: Intent?) {
        Log.i("dmaple_lifetime", "mapping service: onTaskRemoved")
        if(creating) {
            stop()
            runBlocking { saveAndClear(if(AUTO_SAVE_ON_CLOSE) "Auto_Saved" else null) }
            rois.clear()
        }
        stopForeground(true)
        stopSelf()
        super.onTaskRemoved(rootIntent)
    }

    // ---------------------------------------------------------------------------------------------
    // Public wrappers to camera controller
    // ---------------------------------------------------------------------------------------------

    fun setSurface(provider: SurfaceProvider) { camera.setSurface(provider) }

    fun getFieldSize(): Point? { return camera.getFieldSize() }

    fun getAvailableFps(): List<Int> { return camera.getAvailableFps() }

    fun getFps(): Int { return camera.getFps() }

    fun setFps(fps: Int) { camera.setFps(fps) }

    fun setExposure(fraction: Float) { camera.setExposure(fraction) }

    fun setFocus(fraction: Float) { camera.setFocus(fraction) }

    // ---------------------------------------------------------------------------------------------
    // Public interface: Methods to be called after service initiation.
    // ---------------------------------------------------------------------------------------------

    /** Set the map buffer provider. */
    fun setBufferProvider(provider: MapBufferProvider) {
        provider.initialiseBuffers()
        bufferProvider = provider
    }

    /** Set the field's ROIs and ruler. */
    fun setRoisAndRuler(field: RoisAndRuler) {
        rois.clear()
        for(roi in field.rois) rois.add(roi.copy())
        ruler = field.ruler?.copy()
    }

    /** Get the field's ROIs and ruler. e.g. for when a view of the field needs to be reconstructed. */
    fun getRoisAndRuler(): RoisAndRuler { return RoisAndRuler(rois, ruler) }

    // ---------------------------------------------------------------------------------------------
    // Public interface: Record
    // ---------------------------------------------------------------------------------------------

    /** Start or stop map creation, depending on the current creation state. */
    fun startStop(): Warnings { return if(creating) stop() else start() }

    /** Is the mapping service currently creating maps? */
    fun isCreatingMaps(): Boolean { return creating }

    /** The number of seconds since the service started mapping. */
    fun elapsedSeconds(): Long { return timer.secFromRecordingStart() }

    /** The percent error in the frame rate from the expected. */
    fun frameRatePercentError(): Float {
        val mu = 1000f * timer.meanFrameIntervalMilliSec(100)
        val err = if(mu > 0) 100f * abs((mu - camera.frameIntervalMicroSec) / camera.frameIntervalMicroSec) else 0f
        return err
    }

    /** Get current map's creator and map index. */
    fun getCurrentMapCreator(): Pair<MapCreator?, Int> {
        val (currentCreatorIdx, currentMapIdx) = currentMap
        if(currentCreatorIdx < creators.size) return Pair(creators[currentCreatorIdx], currentMapIdx)
        return Pair(null, 0)
    }

    /** Get the last image of the mapping field. */
    fun getLastFieldImage(): FieldImage? {
        return imageReader.colorBitmap?.let { bitmap ->
            // We can assume that the last captured bitmap is in the same frame as a current creator.
            creators.firstOrNull()?.roi?.frame?.let { frame -> FieldImage(frame, bitmap) }
        }
    }

    /** Given a selected ROI, set the next map to show. */
    fun setNextMap(roiUID: String) { currentMap = getNextMap(roiUID) }

    /** Get the allocated buffer size for each map. */
    fun getBufferSize(): Long { return bufferProvider.bufferSize() }

    // ---------------------------------------------------------------------------------------------
    // Public interface: save and load recordings.
    // ---------------------------------------------------------------------------------------------

    /** If maps are not being created, save the maps, clear the creators and free-up resources.
     *
     * @param folderName A user defined name for the folder containing all maps from a recording or
     * null if the maps are not to be saved.
     * */
    fun saveAndClear(folderName: String?) = scope.launch(Dispatchers.Default) {
        if(creating) return@launch
        folderName?.let {
            // Find a valid (not already existing) folder for the record.
            val file = File(MappingRecord.DEFAULT_ROOT, folderName.ifEmpty { MappingRecord.DEFAULT_RECORD_FOLDER })
            val path = CountedPath.fromFile(file = file)
            path.setValidCount(existingPaths = MappingRecord.allDirectoriesInRoot().map{it.absolutePath})
            // Write the record and add it to the record collection.
            val record = MappingRecord(
                location = path.file,
                field = imageReader.colorBitmap,
                ruler = ruler,
                creators = creators,
                timer = timer
            )
            record.write()
            MappingRecord.read(path.file)?.let {MappingRecord.records.add(0, it)}
        }
        clearCreators()
    }

    /** Load a (old) recording.
     *
     * @param record The record to be loaded.
     * @return Whether the record was loaded (will not be loaded if a record is being created).
     * */
    fun loadRecord(record: MappingRecord): Boolean {
        if(creating) return false
        // Check there are enough buffers.
        val rois = record.creators.map { it.roi }
        if(!enoughBuffersForMaps(rois)) return false
        // Load the ROIs, ruler and creators.
        setRoisAndRuler(RoisAndRuler(rois, record.ruler))
        clearCreators()
        record.loadMapTiffs(bufferProvider)
        creators.addAll(record.creators)
        record.field?.let{imageReader.readBitmap(it)}
        currentMap = Pair(0, 0)
        return true
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
        val imageFrame = camera.getImageFrame(this) ?: return warning
        if(!enoughBuffersForMaps()) {
            warning.add(message =
                "There are not enough buffers to process all maps.\n" +
                "Please reduce the number of ROIs or map types selected.",
                causesStop = false
            )
            return warning
        }

        // Create map creators.
        for(roi in rois) {
            // ... initiate and set spatio-temporal resolution.
            val creator = MapCreator(roi.inNewFrame(imageFrame), FieldParams.preference.copy())
            creator.setSpatialPixelsPerUnit(ruler?.getResolution() ?: Pair(1f, "mm"))
            creator.setFrameRatePerSec(camera.getFps().toFloat())
            // ... buffer and add to list.
            val buffers = (0 until creator.nMaps).map{ bufferProvider.getFreeBuffer()}.filterNotNull()
            if(creator.provideBuffers(buffers)) creators.add(creator)
        }

        // State
        camera.setAutosMode(autosOn = false)
        creating = true
        timer.markRecordingStart()
        imageReader.reset()
        return warning
    }

    /** Stop creating maps. */
    private fun stop(): Warnings {
        // State
        creating = false
        timer.markRecordingEnd()
        setCreatorTemporalResolutionFromTimer()
        camera.setAutosMode(autosOn = true)
        return Warnings("Stop Recording")
    }

    /** Check that there are enough buffers for all the maps specified by a set of ROIs.*/
    private fun enoughBuffersForMaps(mapRois: List<FieldRoi> = this.rois): Boolean {
        val nMaps = mapRois.sumOf { roi -> roi.maps.sumOf { map -> map.nMaps } }
        return bufferProvider.nFreeBuffers() >= nMaps
    }

    /** Set the temporal resolution of the creators based upon the actual recording timer. */
    private fun setCreatorTemporalResolutionFromTimer() {
        // To use the timer's recorded frame rate we exclude the first 20 frames (which are
        // often delayed) and have at least 50 frames thereafter (to give a decent average).
        // Otherwise just use the target frame rate.
        val n = timer.nFrames() - 20
        val interval = if(n > 50) 0.001f * timer.meanFrameIntervalMilliSec(n) else 1f / camera.getFps().toFloat()
        for(creator in creators) creator.setFrameIntervalSec(frameIntervalSec = interval)
    }

    /** Clear all creators, freeing up their buffers and resetting the current map. */
    private fun clearCreators() {
        if(creating) return
        creators.clear()
        currentMap = Pair(0, 0)
        bufferProvider.freeAllBuffers()
        System.gc()
    }

    /** Update maps from the latest image captured by the camera. */
    override fun analyze(image: ImageProxy) {
        // Mark the frame. If this is not the next frame, close and return.
        if((!creating) || (!timer.markFrame(image.imageInfo.timestamp))) {
            image.close()
            return
        }
        // Get the luminance (blocking to prevent the device threads from accessing the
        // luminance buffer at the same time) and update the maps.
        // println("${timer.lastFrameIntervalMilliSec()}")
        runBlocking { imageReader.readYUVImage(image) }
        for(creator in creators) creator.updateWithCameraImage(imageReader)
        image.close()
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
