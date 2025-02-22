package com.scepticalphysiologist.dmaple.ui.camera

import android.content.Context
import android.graphics.Color
import android.util.AttributeSet
import android.view.Gravity
import android.view.View
import android.widget.FrameLayout
import androidx.camera.view.PreviewView
import kotlin.math.roundToInt
import android.view.WindowManager
import android.widget.LinearLayout
import androidx.core.view.updateLayoutParams
import androidx.lifecycle.LifecycleOwner
import androidx.lifecycle.MutableLiveData
import com.scepticalphysiologist.dmaple.R
import com.scepticalphysiologist.dmaple.etc.Point
import com.scepticalphysiologist.dmaple.etc.SwitchableSlider
import com.scepticalphysiologist.dmaple.map.MappingRoi
import com.scepticalphysiologist.dmaple.map.MappingService
import com.scepticalphysiologist.dmaple.etc.aspectRatioRatio
import com.scepticalphysiologist.dmaple.map.MappingFieldImage

/** The mapping field of view. The camera feed and overlays for:
 * - drawing mapping ROIs and thresholding them.
 * - showing the mapping process.
 */
class MappingFieldOfView(context: Context, attributeSet: AttributeSet?):
    FrameLayout(context, attributeSet),
    View.OnLayoutChangeListener
{
    // Child Views
    // -----------
    /** The camera feed. The camera is actually run by the [MappingService]. */
    private val cameraFeed = PreviewView(context, attributeSet)
    /** An overlay over the camera feed unto which mappings ROIs can eb drawn. */
    private val roiOverlay = MappingRoiOverlay(context, attributeSet)
    /** An overlay over the camera feed unto which mapping processes *such as spines) can be drawn. */
    private val spineOverlay = SpineView(context, attributeSet)

    // Controls
    // --------
    /** A slider for thresholding mapping ROIs. */
    private val thresholdSlider = SwitchableSlider(context, Pair(0, 255), R.drawable.threshold_steps, Color.RED)
    /** A slide for controlling exposure. */
    private val exposureSlider = SwitchableSlider(context, Pair(0, 100), R.drawable.exposure_sun, Color.YELLOW)
    /** Indicate that exposure has changed - a value between 0 and 1. */
    val exposure = MutableLiveData<Float>(0f)

    // View
    // ----
    /** Display information. */
    private val display = (context.getSystemService(Context.WINDOW_SERVICE) as WindowManager).defaultDisplay
    /** The parent view. */
    private val parentView: View get() = this.parent as View

    init {
        // Camera preview view
        cameraFeed.implementationMode = PreviewView.ImplementationMode.COMPATIBLE
        cameraFeed.scaleType = PreviewView.ScaleType.FILL_CENTER

        // Camera and overlays.
        this.addView(cameraFeed)
        this.addView(spineOverlay)
        this.addView(roiOverlay)

        // Slider controls.
        val sliderGroup = LinearLayout(context)
        sliderGroup.orientation = LinearLayout.HORIZONTAL
        sliderGroup.layoutParams = LayoutParams(LayoutParams.WRAP_CONTENT, LayoutParams.MATCH_PARENT, Gravity.RIGHT)
        sliderGroup.addView(thresholdSlider)
        sliderGroup.addView(exposureSlider)
        thresholdSlider.switch(show=false)
        exposureSlider.switch(show=false)
        this.addView(sliderGroup)

        // Layout.
        setBackgroundColor(Color.GRAY)

        // React to the thresholding slider.
        val owner = context as LifecycleOwner
        // ... start or stop thresholding when the slide is moved or releases.
        thresholdSlider.onoff.observe(owner) { isOn ->
            if (isOn) cameraFeed.bitmap?.let { roiOverlay.startThresholding(it) }
            else roiOverlay.stopThresholding()
        }
        // ... update the ROI's threshold.
        thresholdSlider.position.observe(owner) { position -> roiOverlay.setThreshold(position) }
        // ... set the slider to the threshold of a selected ROI.
        roiOverlay.activeRoiChanged.observe(owner) { threshold ->
            threshold?.let { thresholdSlider.setPosition(it) }
            thresholdSlider.switch(show=false)
            thresholdSlider.visibility = if(threshold != null) View.VISIBLE else View.INVISIBLE
        }

        // Exposure control
        exposureSlider.position.observe(owner) { exposure.postValue(it.toFloat() / 100f) }
    }

    // ---------------------------------------------------------------------------------------------
    // Public wrappers around child views
    // ---------------------------------------------------------------------------------------------

    fun getSavedRois(): List<MappingRoi> { return roiOverlay.savedRois }

    fun setSavedRois(rois: List<MappingRoi>) { roiOverlay.setSavedRois(rois) }

    /** Show a fixed image of a mapping field rather than the camera feed.
     *
     * @param field The field image to show or null to show the camera feed.
     * */
    fun freezeField(field: MappingFieldImage?) {
        roiOverlay.setBacking(field)
    }

    fun roiHasBeenSelected(): MutableLiveData<String> { return roiOverlay.selectedRoi }

    fun savedRoisHaveChanged(): MutableLiveData<Boolean> { return roiOverlay.savedRoiChange }

    fun allowEditing(allow: Boolean = true) {
        exposureSlider.visibility = if(allow) View.VISIBLE else View.INVISIBLE
        roiOverlay.allowEditing(allow)
    }

    fun getCameraPreview(): PreviewView { return cameraFeed }

    fun setExposureSlider(fraction: Float) {
        exposureSlider.setPosition((fraction * 100).toInt())
    }

    // ---------------------------------------------------------------------------------------------
    // Layout
    // ---------------------------------------------------------------------------------------------

    /** Resize the field of view to an arbitrary size. */
    fun resize(w: Int, h: Int) {
        updateSize(w, h)
        // Now this view is detached from its parent size, the parent will not call this view's
        // onLayout when it is resized. Therefore listen to the parent layout directly and update
        // this view's layout when the parent changes size.
        parentView.addOnLayoutChangeListener(this)
    }

    /** Resize the field of view to match its parent view. */
    fun fullSize(){
        updateSize(LayoutParams.MATCH_PARENT, LayoutParams.MATCH_PARENT)
        parentView.removeOnLayoutChangeListener(this)
    }

    private fun updateSize(w: Int, h: Int) {
        updateLayoutParams<LayoutParams> { width = w; height = h }
    }

    /** Respond to the resize of another view. */
    override fun onLayoutChange(
        p0: View?,
        l0: Int, t0: Int, r0: Int, b0: Int,
        l1: Int, t1: Int, r1: Int, b1: Int
    ) {
        if((r0 - l0 != r1 - l1) || (b0 - t0 != b1 - t1)) updateLayout()
    }

    /** Respond to the resize of the parent view when its decides the layout of children needs to change. */
    override fun onLayout(changed: Boolean, left: Int, top: Int, right: Int, bottom: Int) {
        super.onLayout(changed, left, top, right, bottom)
        if(changed) updateLayout()
    }

    /** Update the view's layout so that it does not exceed the size of the parent view and is added
     * such that the field of view maintains the same aspect ratio as specified by the [MappingService].*/
    private fun updateLayout() {
        // Make sure the view does not exceed the parent view.
        // Because width/height might not be set to "match parent" (see this.resize()),
        // the view might end up extending past the parent view after rotation.
        val (w, h) = Pair(parentView.width, parentView.height)
        if((width > w) || (height > h)) resize(minOf(w, width), minOf(h, height))

        // Appropriate aspect ratio for screen size.
        val screenArea = Point(display.width.toFloat(), display.height.toFloat())
        val arr = aspectRatioRatio(MappingService.CAMERA_ASPECT_RATIO)
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

    /** The two nearest integers adding up to another integer. */
    private fun complement(x: Int): Pair<Int, Int> {
        val x0 = x / 2
        return Pair(x0, x - x0)
    }

}
