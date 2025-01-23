package com.scepticalphysiologist.dmaple.ui.camera

import android.content.Context
import android.graphics.Color
import android.util.AttributeSet
import android.view.Gravity
import android.view.View
import android.widget.FrameLayout
import androidx.camera.core.AspectRatio
import androidx.camera.view.PreviewView
import kotlin.math.roundToInt

import android.view.WindowManager
import androidx.core.view.updateLayoutParams
import androidx.lifecycle.LifecycleOwner
import androidx.lifecycle.MutableLiveData
import com.scepticalphysiologist.dmaple.ui.VerticalSlider

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
    View.OnLayoutChangeListener
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
    private val display = (context.getSystemService(Context.WINDOW_SERVICE) as WindowManager).defaultDisplay


    // ---------------------------------------------------------------------------------------------
    // Construction
    // ---------------------------------------------------------------------------------------------

    init {
        // Camera preview view
        cameraPreview = PreviewView(context, attributeSet)
        cameraPreview.implementationMode = PreviewView.ImplementationMode.COMPATIBLE
        cameraPreview.scaleType = PreviewView.ScaleType.FILL_CENTER
        this.addView(cameraPreview)

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
    // Public
    // ---------------------------------------------------------------------------------------------

    fun getRois(): List<MappingRoi> { return roiView.savedRois }

    fun selectedRoiObject(): MutableLiveData<Int> { return roiView.selectedRoi }

    fun getCameraPreview(): PreviewView { return cameraPreview }

    fun allowEditing(allow: Boolean = true) { roiView.allowEditing(allow) }



    // ---------------------------------------------------------------------------------------------
    // Layout
    // ---------------------------------------------------------------------------------------------

    fun resize(w: Int, h: Int) {
        updateSize(w, h)
        // Now this view is detached from its parent size, the parent will not call this view's
        // onLayout when it is resized. Therefore listen to the parent layout directly and update
        // this view's layout when the parent changes size.
        (this.parent as View).addOnLayoutChangeListener(this)
    }

    fun fullSize(){
        updateSize(LayoutParams.MATCH_PARENT, LayoutParams.MATCH_PARENT)
        (this.parent as View).removeOnLayoutChangeListener(this)
    }

    private fun updateSize(w: Int, h: Int) {
        updateLayoutParams<LayoutParams> { width = w; height = h }
    }

    /** Respond to the resize of another view. */
    override fun onLayoutChange(p0: View?, l0: Int, t0: Int, r0: Int, b0: Int, l1: Int, t1: Int, r1: Int, b1: Int) {
        if((r0 - l0 != r1 - l1) || (b0 - t0 != b1 - t1)) updateLayout()
    }

    /** Respond to the resize of the parent view when its decides the layout of children needs to change. */
    override fun onLayout(changed: Boolean, left: Int, top: Int, right: Int, bottom: Int) {
        super.onLayout(changed, left, top, right, bottom)
        if(changed) updateLayout()
    }

    private fun updateLayout() {
        println("UPDATE LAYOUT: camera roi")

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
    }

    private fun constrainSize(w: Int, h: Int) {
        if((width > w) || (height > h)) resize(minOf(w, width), minOf(h, height))
    }

    private fun complement(x: Int): Pair<Int, Int> {
        val x0 = x / 2
        return Pair(x0, x - x0)
    }

}
