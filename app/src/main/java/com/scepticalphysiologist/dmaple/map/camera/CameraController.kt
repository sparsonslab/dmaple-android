// Copyright (c) 2025-2025. Dr Sean Paul Parsons. All rights reserved.

package com.scepticalphysiologist.dmaple.map.camera

import android.content.Context
import android.hardware.camera2.CameraCharacteristics
import android.hardware.camera2.CaptureRequest
import android.util.Range
import android.view.WindowManager
import androidx.annotation.OptIn
import androidx.camera.camera2.interop.Camera2CameraControl
import androidx.camera.camera2.interop.Camera2CameraInfo
import androidx.camera.camera2.interop.Camera2Interop
import androidx.camera.camera2.interop.CaptureRequestOptions
import androidx.camera.camera2.interop.ExperimentalCamera2Interop
import androidx.camera.core.Camera
import androidx.camera.core.CameraSelector
import androidx.camera.core.ImageAnalysis
import androidx.camera.core.Preview
import androidx.camera.core.Preview.SurfaceProvider
import androidx.camera.core.UseCase
import androidx.camera.lifecycle.ProcessCameraProvider
import androidx.lifecycle.LifecycleOwner
import com.scepticalphysiologist.dmaple.geom.Frame
import com.scepticalphysiologist.dmaple.geom.Point
import com.scepticalphysiologist.dmaple.map.MappingService.Companion.CAMERA_ASPECT_RATIO
import java.util.concurrent.Executors


@OptIn(ExperimentalCamera2Interop::class)
class CameraController(
    context: Context,
    analyser: ImageAnalysis.Analyzer,
    private val owner: LifecycleOwner
) {

    /** The camera provider. */
    private val cameraProvider: ProcessCameraProvider = ProcessCameraProvider.getInstance(context).get()
    /** The camera. */
    private lateinit var camera: Camera
    /** The camera preview. */
    private var preview: Preview? = null
    /** The view ("surface provider") of the camera preview. */
    private var surface: SurfaceProvider? = null
    /** The camera image analyser. */
    private var analyser: ImageAnalysis? = null
    /** Auto exposure, white-balance and focus are off. */
    private var autosOff: Boolean = false
    /** Approximate frame rate (frames/second). */
    private var frameRateFps: Int = getAvailableFps().max()
    /** Approximate interval between frames (microseconds). */
    val frameIntervalMicroSec: Long get() = (1_000_000f / frameRateFps.toFloat()).toLong()

    // ---------------------------------------------------------------------------------------------
    // Initiation
    // ---------------------------------------------------------------------------------------------

    init {
        cameraProvider.unbindAll()
        setPreview()
        setAnalyser(analyser)
    }

    /** Set the preview use case of CameraX. */
    fun setPreview(switchOffAuto: Boolean? = null) {
        switchOffAuto?.let { autosOff = switchOffAuto }
        unBindUse(preview)
        preview = Preview.Builder().also { builder ->
            builder.setTargetAspectRatio(CAMERA_ASPECT_RATIO)
            // Frame rate.
            val inop = Camera2Interop.Extender(builder)
            inop.setCaptureRequestOption(CaptureRequest.CONTROL_AE_TARGET_FPS_RANGE, Range(frameRateFps, frameRateFps))
            // Auto exposure and white-balance.
            if(autosOff) {
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
    private fun setAnalyser(analyzer: ImageAnalysis.Analyzer) {
        unBindUse(analyser)
        analyser = ImageAnalysis.Builder().also { builder ->
            builder.setTargetAspectRatio(CAMERA_ASPECT_RATIO)
            // We should never use ImageAnalysis.STRATEGY_BLOCK_PRODUCER, because in this mode
            // frames can come in asynchronously (out of order).
            builder.setBackpressureStrategy(ImageAnalysis.STRATEGY_KEEP_ONLY_LATEST)
            builder.setOutputImageFormat(ImageAnalysis.OUTPUT_IMAGE_FORMAT_YUV_420_888)
        }.build().also { use ->
            use.setAnalyzer(Executors.newFixedThreadPool(5), analyzer)
            bindUse(use)
        }
    }

    /** Bind a CameraX use case. */
    private fun bindUse(use: UseCase?) {
        camera = cameraProvider.bindToLifecycle(owner, CameraSelector.DEFAULT_BACK_CAMERA, use)
    }

    /** Unbind a CameraX use case. */
    private fun unBindUse(use: UseCase?) { cameraProvider.unbind(use) }


    // ---------------------------------------------------------------------------------------------
    // Settings
    // ---------------------------------------------------------------------------------------------

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

    /** Set the camera focal distance (as a fraction of the available range).*/
    fun setFocus(fraction: Float) {
        // Focal distance as fraction of the available.
        val distance = Camera2CameraInfo.from(camera.cameraInfo).getCameraCharacteristic(
            CameraCharacteristics.LENS_INFO_MINIMUM_FOCUS_DISTANCE
        )?.let { it * (1f - fraction) } ?: 0f
        // Set focus. Auto-focus (video mode) if fraction is zero.
        val builder = CaptureRequestOptions.Builder()
        if(fraction == 0f) {
            builder.setCaptureRequestOption(CaptureRequest.CONTROL_AF_MODE, CaptureRequest.CONTROL_AF_MODE_CONTINUOUS_VIDEO)
        } else {
            builder.setCaptureRequestOption(CaptureRequest.CONTROL_AF_MODE, CaptureRequest.CONTROL_AF_MODE_OFF)
            builder.setCaptureRequestOption(CaptureRequest.LENS_FOCUS_DISTANCE, distance)
        }
        Camera2CameraControl.from(camera.cameraControl).captureRequestOptions = builder.build()
    }

    /** Set the frame rate (frames per second). */
    fun setFps(fps: Int) {
        val available = getAvailableFps()
        frameRateFps = if(fps in available) fps else available.max()
        setPreview()
    }

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

    /** Get the pixel width and height of the mapping field. */
    fun getFieldSize(): Point? {
        return analyser?.resolutionInfo?.let { info ->
            Point(info.resolution.width.toFloat(), info.resolution.height.toFloat())
        }
    }

    fun getImageFrame(context: Context): Frame? {
        val display = (context.getSystemService(Context.WINDOW_SERVICE) as WindowManager).defaultDisplay
        return analyser?.let{Frame.ofImageAnalyser(it, display) }
    }

}
