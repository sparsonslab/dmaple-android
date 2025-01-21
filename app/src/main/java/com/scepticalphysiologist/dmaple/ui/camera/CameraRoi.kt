package com.scepticalphysiologist.dmaple.ui.camera

import android.content.Context
import android.graphics.Bitmap
import android.graphics.Color
import android.util.AttributeSet
import android.util.Rational
import android.view.Gravity
import android.view.Surface
import android.view.View
import android.widget.FrameLayout
import androidx.appcompat.app.AppCompatActivity
import androidx.camera.core.AspectRatio
import androidx.camera.core.Camera
import androidx.camera.core.CameraSelector
import androidx.camera.core.ImageAnalysis
import androidx.camera.core.ImageCapture
import androidx.camera.core.ImageProxy
import androidx.camera.core.Preview
import androidx.camera.core.UseCaseGroup
import androidx.camera.core.ViewPort
import androidx.camera.lifecycle.ProcessCameraProvider
import androidx.camera.view.PreviewView
import java.util.concurrent.Executors
import kotlin.math.roundToInt

import android.view.WindowManager
import androidx.core.view.updateLayoutParams
import androidx.lifecycle.LifecycleOwner
import androidx.lifecycle.MutableLiveData
import com.scepticalphysiologist.dmaple.ui.VerticalSlider
import com.scepticalphysiologist.dmaple.ui.helper.Warnings

/**
 * A camera preview and ROI overlay.
 *
 * @constructor
 * TODO
 *
 * @param context
 * @param attributeSet
 */
class CameraRoi(context: Context, attributeSet: AttributeSet?):
    FrameLayout(context, attributeSet),
    ImageAnalysis.Analyzer
{

    // Views
    // -----
    private var aspect = AspectRatio.RATIO_16_9

    private val cameraPreview: PreviewView

    private val roiView: RoiView

    private val spineView: SpineView

    // Controls
    private val thresholdSlider: VerticalSlider


    // Camera

    private val cameraProvider: ProcessCameraProvider

    private val cameraUses: UseCaseGroup

    private val camera: Camera

    private val display = (context.getSystemService(Context.WINDOW_SERVICE) as WindowManager).defaultDisplay

    // State
    private var recording: Boolean = false

    private var analysers = mutableListOf<GutAnalyser>()

    val upDateMap = MutableLiveData<Boolean>(false)

    // ---------------------------------------------------------------------------------------------
    // Construction
    // ---------------------------------------------------------------------------------------------

    init {

        // Camera preview view
        cameraPreview = PreviewView(context, attributeSet)
        cameraPreview.implementationMode = PreviewView.ImplementationMode.COMPATIBLE
        cameraPreview.scaleType = PreviewView.ScaleType.FILL_CENTER
        this.addView(cameraPreview)

        // Camera
        cameraProvider = ProcessCameraProvider.getInstance(context).get()
        cameraProvider.unbindAll()
        cameraUses = buildUseCases()
        camera = cameraProvider.bindToLifecycle(
            lifecycleOwner = context as AppCompatActivity,
            cameraSelector = CameraSelector.DEFAULT_BACK_CAMERA,
            cameraUses
        )

        // Roi and spine overlay.
        spineView = SpineView(context, attributeSet)
        this.addView(spineView)
        roiView = RoiView(context, attributeSet)
        this.addView(roiView)

        // ROI threshold slider.
        thresholdSlider = VerticalSlider(this.context, attributeSet, Pair(0, 255), Color.RED)
        this.addView(thresholdSlider, LayoutParams(40, LayoutParams.MATCH_PARENT, Gravity.RIGHT))
        if(this.context is LifecycleOwner) connectThresholdSlider(this.context as LifecycleOwner)

        // Layout.
        setBackgroundColor(Color.GRAY)
    }

    private fun buildUseCases(): UseCaseGroup {

        val useCaseGroup= UseCaseGroup.Builder()

        val viewport = ViewPort.Builder(
            Rational(1, 1), Surface.ROTATION_0
        ).setScaleType(ViewPort.FIT).build()
        useCaseGroup.setViewPort(viewport)

        val preview = Preview.Builder().setTargetAspectRatio(aspect).build()
        preview.surfaceProvider = cameraPreview.surfaceProvider
        useCaseGroup.addUseCase(preview)

        val imageCapture = ImageCapture.Builder().setTargetAspectRatio(aspect).build()
        useCaseGroup.addUseCase(imageCapture)

        val imageAnalysis = ImageAnalysis.Builder().setTargetAspectRatio(aspect).build()
        imageAnalysis.setAnalyzer(Executors.newSingleThreadExecutor(), this)

        useCaseGroup.addUseCase(imageAnalysis)

        return useCaseGroup.build()
    }


    private fun connectThresholdSlider(owner: LifecycleOwner){
        thresholdSlider.onoff.observe(owner) { isOn ->
            if (isOn) cameraPreview.bitmap?.let { roiView.startThresholding(it) }
            else roiView.stopThresholding()
        }
        thresholdSlider.position.observe(owner) { position ->
            roiView.setThreshold(position)
        }
        roiView.activeRoiChanged.observe(owner) { threshold ->
            threshold?.let { thresholdSlider.setPosition(it) }
            thresholdSlider.visibility = if(threshold != null) View.VISIBLE else View.INVISIBLE
        }
    }


    // ---------------------------------------------------------------------------------------------
    // Analysis
    // ---------------------------------------------------------------------------------------------

    fun startStop(): Boolean {
        val warnings = if(recording) stop() else start()
        warnings.show(this.context)
        return recording
    }

    fun isRecording(): Boolean { return recording }

    fun getAnalyser(i: Int): GutAnalyser? { return if(i < analysers.size) analysers[i] else null }

    private fun start(): Warnings {

        // Cannot not start if there are no ROIs.
        val warnings = Warnings("Start Recording")
        if(roiView.savedRois.isEmpty()) {
            val msg = "There are no areas to map (dashed rectangles).\n" +
                      "Make a mapping area by double tapping a selection."
            warnings.add(msg, true)
            return warnings
        }

        // Convert the frame of the mapping ROIs to the frame of the camera.
        val imageFrame = imageAnalyserFrame() ?: return warnings
        analysers = roiView.roisInNewFrame(imageFrame).map {GutMapper(it)}.toMutableList()

        // Prevent ROI editing.
        roiView.allowEditing(false)
        recording = true
        return warnings
    }

    private fun stop(): Warnings {
        val warnings = Warnings("Stop Recording")

        // Save maps.
        // ?????????

        // Allow ROIs to be edited.
        analysers.clear()
        roiView.allowEditing(true)
        recording = false
        return warnings
    }

    private fun imageAnalyserFrame(): Frame? {
        // Get analyser and update its target orientation.
        val analysis = cameraUses.useCases.filterIsInstance<ImageAnalysis>().firstOrNull() ?: return null
        analysis.targetRotation = display.rotation

        val imageInfo = analysis.resolutionInfo ?: return null
        val or = imageInfo.rotationDegrees + surfaceRotationDegrees(analysis.targetRotation)
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


    // ---------------------------------------------------------------------------------------------
    // Layout
    // ---------------------------------------------------------------------------------------------

    fun resize(w: Int, h: Int) { updateLayoutParams<LayoutParams> { width = w; height = h } }

    fun fullSize(){
        updateLayoutParams<LayoutParams> {
            width = LayoutParams.MATCH_PARENT
            height = LayoutParams.MATCH_PARENT
        }
    }

    private fun constrainSize(w: Int, h: Int) {
        if((width > w) || (height > h)) resize(minOf(w, width), minOf(h, height))
    }

    override fun onLayout(changed: Boolean, left: Int, top: Int, right: Int, bottom: Int) {
        // Make sure the view does not exceed the parent view.
        // Because width/height might not be set to "match parent" (see this.resize()),
        // the view might end up extending past the parent view after rotation.
        val pv = this.parent as View
        constrainSize(pv.width, pv.height)

        // Appropriate aspect ratio for screen size.
        val screenArea = Point(display.width.toFloat(), display.height.toFloat())
        val arr = aspectRatioRatio(this.aspect)
        val ratio = if(screenArea.x < screenArea.y) Point(1f, arr) else Point(arr, 1f)

        // Pad view area to obtain aspect ratio.
        // Set padding so that child views (camera preview and ROI overlay) will
        // be at target aspect ratio.
        val viewArea = Point.ofViewExtent(this)
        val scale = (viewArea / ratio).min()
        val padding = viewArea - ratio * scale
        val (px0, px1) = complement(padding.x.roundToInt())
        val (py0, py1) = complement(padding.y.roundToInt())
        this.setPadding(px0, py0, px1, py1)
        super.onLayout(changed, left, top, right, bottom)
    }

    private fun complement(x: Int): Pair<Int, Int> {
        val x0 = x / 2
        return Pair(x0, x - x0)
    }
    
}
