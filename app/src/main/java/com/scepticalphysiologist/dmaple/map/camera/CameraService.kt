// Copyright (c) 2025-2025. Dr Sean Paul Parsons. All rights reserved.

package com.scepticalphysiologist.dmaple.map.camera

import android.content.Context
import android.hardware.camera2.CameraCaptureSession
import android.hardware.camera2.CameraCharacteristics
import android.hardware.camera2.CaptureRequest
import android.hardware.camera2.CaptureResult
import android.hardware.camera2.TotalCaptureResult
import android.util.Range
import android.view.WindowManager
import androidx.annotation.OptIn
import androidx.camera.camera2.interop.Camera2CameraControl
import androidx.camera.camera2.interop.Camera2CameraInfo
import androidx.camera.camera2.interop.Camera2Interop
import androidx.camera.camera2.interop.CaptureRequestOptions
import androidx.camera.camera2.interop.ExperimentalCamera2Interop
import androidx.camera.core.AspectRatio
import androidx.camera.core.Camera
import androidx.camera.core.CameraInfo
import androidx.camera.core.CameraSelector
import androidx.camera.core.DynamicRange
import androidx.camera.core.ImageAnalysis
import androidx.camera.core.Preview
import androidx.camera.core.Preview.SurfaceProvider
import androidx.camera.core.UseCase
import androidx.camera.lifecycle.ProcessCameraProvider
import androidx.camera.video.FileOutputOptions
import androidx.camera.video.Quality
import androidx.camera.video.QualitySelector
import androidx.camera.video.Recorder
import androidx.camera.video.Recording
import androidx.camera.video.VideoCapture
import androidx.camera.video.VideoRecordEvent
import androidx.core.util.Consumer
import androidx.lifecycle.LifecycleOwner
import com.scepticalphysiologist.dmaple.geom.Frame
import com.scepticalphysiologist.dmaple.geom.Point
import com.scepticalphysiologist.dmaple.geom.videoQualityHeight
import java.io.File
import java.util.concurrent.Executors

/** A wrapper around the hideously complicated Android camera API.
 *
 * @param context A context for the camera provider.
 * @param analyser An image analyser for the image analysis use case.
 * @property owner The life cycle owner to which camera use cases will be bound.
 * @param videoFolder A folder for keeping temporary video files.
 */
@OptIn(ExperimentalCamera2Interop::class)
class CameraService(
    context: Context,
    analyser: ImageAnalysis.Analyzer,
    private val owner: LifecycleOwner,
    videoFolder: File? = null
): Consumer<VideoRecordEvent> {

    companion object {
        /** The camera being used for mapping.*/
        val CAMERA_ID = CameraSelector.DEFAULT_BACK_CAMERA
        /** The aspect ratio of the camera. */
        const val ASPECT_RATIO = AspectRatio.RATIO_16_9
    }

    // Camera objects
    // --------------
    /** The camera provider. */
    private val cameraProvider: ProcessCameraProvider = ProcessCameraProvider.getInstance(context).get()
    /** Information about the camera being used. */
    private val cameraInfo: CameraInfo = cameraProvider.getCameraInfo(CAMERA_ID)
    /** The camera object. */
    private lateinit var camera: Camera

    // Camera use cases
    // ----------------
    /** The camera preview. */
    private var preview: Preview? = null
    /** The view ("surface provider") of the camera preview. */
    private var surface: SurfaceProvider? = null
    /** The camera image analyser. */
    private var analyser: ImageAnalysis? = null
    /** Video capture. */
    private var video: VideoCapture<Recorder>? = null

    // State
    // -----
    /** The fraction of the focal distance set by the user (0 indicates auto-focus). */
    private var userSetFocalFraction = 0f
    /** The current (real-time) focal distance. */
    private var currentFocalDistance: Float? = 0f
    /** Approximate frame rate (frames/second). */
    private var frameRateFps: Int = getAvailableFps().max()
    /** Approximate interval between frames (microseconds). */
    val frameIntervalMicroSec: Long get() = (1_000_000f / frameRateFps.toFloat()).toLong()

    // Video
    // -----
    /** Recording object. Null when video is not being recorded. */
    private var recording: Recording? = null
    /** A temporary file for recording video. */
    private val temporaryVideoPath = File(videoFolder, "temp_video.mp4")

    // ---------------------------------------------------------------------------------------------
    // Initiation
    // ---------------------------------------------------------------------------------------------

    init {
        // Set use cases.
        cameraProvider.unbindAll()
        setPreview()
        setAnalyser(analyser)
        setVideoRecorder()

        // Set capture parameters.
        setFps(frameRateFps)
        setAutosMode(autosOn = true)
    }

    /** Set the preview use case of CameraX. */
    private fun setPreview() {

        // Call back for recording the real-time focal distance of the camera.
        val captureCallback = object : CameraCaptureSession.CaptureCallback() {
            override fun onCaptureCompleted(
                session: CameraCaptureSession,
                request: CaptureRequest,
                result: TotalCaptureResult
            ) {
                super.onCaptureCompleted(session, request, result)
                currentFocalDistance = result.get(CaptureResult.LENS_FOCUS_DISTANCE)
            }
        }

        unBindUse(preview)
        preview = Preview.Builder().also { builder ->
            builder.setTargetAspectRatio(ASPECT_RATIO)
            // Listen to the focal distance.
            Camera2Interop.Extender(builder).setSessionCaptureCallback(captureCallback)
        }.build().also { prev ->
            surface?.let {s -> prev.surfaceProvider = s}
            bindUse(prev)
        }
    }

    /** Set the image analyser use case of CameraX. */
    private fun setAnalyser(analyzer: ImageAnalysis.Analyzer) {
        unBindUse(analyser)
        analyser = ImageAnalysis.Builder().also { builder ->
            builder.setTargetAspectRatio(ASPECT_RATIO)
            // We should never use ImageAnalysis.STRATEGY_BLOCK_PRODUCER, because in this mode
            // frames can come in asynchronously (out of order).
            builder.setBackpressureStrategy(ImageAnalysis.STRATEGY_KEEP_ONLY_LATEST)
            builder.setOutputImageFormat(ImageAnalysis.OUTPUT_IMAGE_FORMAT_YUV_420_888)
        }.build().also { use ->
            use.setAnalyzer(Executors.newFixedThreadPool(5), analyzer)
            bindUse(use)
        }
    }

    /** Set the video recorder use case of CameraX.
     * If the bit-rate is < 1000 do not bind. */
    private fun setVideoRecorder(bitsPerSecond: Int = 1_000_000) {
        unBindUse(video)
        if(bitsPerSecond < 1000) return
        val recorder = Recorder.Builder().also { builder ->
            builder.setQualitySelector(QualitySelector.from(getVideoQuality()))
            builder.setAspectRatio(ASPECT_RATIO)
            builder.setTargetVideoEncodingBitRate(bitsPerSecond)
        }.build()
        video = VideoCapture.withOutput(recorder)
        bindUse(video)
    }

    /** Get the video quality. Gets the quality from the preview if it is already bound.*/
    private fun getVideoQuality(): Quality {
        // Video qualities available for the camera.
        val qualities = Recorder.getVideoCapabilities(cameraInfo).getSupportedQualities(DynamicRange.SDR)
        // Base upon the preview.
        preview?.resolutionInfo?.resolution?.let { resolution ->
            qualities.firstOrNull { videoQualityHeight(it) == resolution.height }?.let { quality ->
                return quality
            }
        }
        // Otherwise use HD or (if not available), use the highest.
        if(qualities.contains(Quality.HD)) return Quality.HD
        return qualities.maxBy { QualitySelector.getResolution(cameraInfo, it)?.height ?: 0 }
    }

    /** Bind a CameraX use case. */
    private fun bindUse(use: UseCase?) {
        camera = cameraProvider.bindToLifecycle(owner, CAMERA_ID, use)
    }

    /** Unbind a CameraX use case. */
    private fun unBindUse(use: UseCase?) { cameraProvider.unbind(use) }

    /** Set the camera preview surface. Should be set before using the camera. */
    fun setSurface(provider: SurfaceProvider) {
        surface = provider
        preview?.surfaceProvider = provider
    }

    // ---------------------------------------------------------------------------------------------
    // Controls
    // ---------------------------------------------------------------------------------------------

    /** Set the camera exposure (as a fraction of the available range). */
    fun setExposure(fraction: Float) {
        val range = camera.cameraInfo.exposureState.exposureCompensationRange
        val exposure = (range.lower + fraction * (range.upper - range.lower)).toInt()
        camera.cameraControl.setExposureCompensationIndex(exposure)
    }

    /** Set the camera focal distance (as a fraction of the available range).*/
    fun setFocus(fraction: Float) {
        userSetFocalFraction = fraction
        val builder = CaptureRequestOptions.Builder()
        // If fraction is zero, set auto-focus (video mode).
        if(fraction == 0f) builder.setCaptureRequestOption(
            CaptureRequest.CONTROL_AF_MODE, CaptureRequest.CONTROL_AF_MODE_CONTINUOUS_VIDEO
        )
        // ... otherwise set a fixed focal distance as a fraction of the distance range.
        else {
            val distance = Camera2CameraInfo.from(camera.cameraInfo).getCameraCharacteristic(
                CameraCharacteristics.LENS_INFO_MINIMUM_FOCUS_DISTANCE
            )?.let { it * (1f - fraction) } ?: 0f
            builder.setCaptureRequestOption(CaptureRequest.CONTROL_AF_MODE, CaptureRequest.CONTROL_AF_MODE_OFF)
            builder.setCaptureRequestOption(CaptureRequest.LENS_FOCUS_DISTANCE, distance)
        }
        Camera2CameraControl.from(camera.cameraControl).addCaptureRequestOptions(builder.build())
    }

    /** Set the frame rate (frames per second). */
    fun setFps(fps: Int) {
        val available = getAvailableFps()
        frameRateFps = if(fps in available) fps else available.max()
        val builder = CaptureRequestOptions.Builder()
        builder.setCaptureRequestOption(CaptureRequest.CONTROL_AE_TARGET_FPS_RANGE, Range(frameRateFps, frameRateFps))
        Camera2CameraControl.from(camera.cameraControl).addCaptureRequestOptions(builder.build())
    }

    /** Set auto -focus, -exposure and -white-balance on or off. */
    fun setAutosMode(autosOn: Boolean) {
        val builder = CaptureRequestOptions.Builder()
        builder.setCaptureRequestOption(CaptureRequest.CONTROL_AWB_LOCK, !autosOn)
        builder.setCaptureRequestOption(CaptureRequest.CONTROL_AE_LOCK, !autosOn)
        if(autosOn) setFocus(userSetFocalFraction) else {
            currentFocalDistance?.let { builder.setCaptureRequestOption(CaptureRequest.LENS_FOCUS_DISTANCE, it) }
            builder.setCaptureRequestOption(CaptureRequest.CONTROL_AF_MODE, CaptureRequest.CONTROL_AF_MODE_OFF)
        }
        Camera2CameraControl.from(camera.cameraControl).addCaptureRequestOptions(builder.build())
    }

    /** Set video recording bit rate. */
    fun setVideoBitRate(megabitsPerSecond: Int) {
        // There is no CaptureRequest for video bitrate, so have to rebind the video use case.
        setVideoRecorder(bitsPerSecond = megabitsPerSecond * 1_000_000)
    }

    // ---------------------------------------------------------------------------------------------
    // Video capture
    // ---------------------------------------------------------------------------------------------

    fun startRecording(context: Context) {
        // We are already recording or the bitrate is.
        if(recording != null) return
        // Start recording.
        val fileOptions = FileOutputOptions.Builder(temporaryVideoPath).build()
        video?.output?.prepareRecording(context, fileOptions)?.let { pending ->
            recording = pending.start(Executors.newSingleThreadExecutor(), this)
        }
    }

    fun stopRecording() {
        recording?.stop()
        recording = null
    }

    fun saveRecording(destPath: File) {
        // Don't save if: the folder doesn't exist, the video folder doesn't exist or
        // we are in the middle of a recording.
        if(!destPath.exists() || !temporaryVideoPath.exists() || (recording != null)) return
        // "Rename" (i.e. move) the video.
        temporaryVideoPath.renameTo(File(destPath, "field_video.mp4"))
    }

    override fun accept(value: VideoRecordEvent) {}

    // ---------------------------------------------------------------------------------------------
    // Information
    // ---------------------------------------------------------------------------------------------

    /** Get the approximate frame rate (frames/second). */
    fun getFps(): Int { return frameRateFps }

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

    /** Get the pixel width and height of the images created by the image analysis use case. */
    fun getFieldSize(): Point? {
        return analyser?.resolutionInfo?.let { info ->
            Point(info.resolution.width.toFloat(), info.resolution.height.toFloat())
        }
    }

    /** Get the frame of the images created by the image analysis use case. */
    fun getImageFrame(context: Context): Frame? {
        val display = (context.getSystemService(Context.WINDOW_SERVICE) as WindowManager).defaultDisplay
        return analyser?.let{Frame.ofImageAnalyser(it, display) }
    }

}
