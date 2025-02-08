package com.scepticalphysiologist.dmaple.ui.camera

import android.content.Context
import android.graphics.Bitmap
import android.graphics.Canvas
import android.graphics.Color
import android.graphics.DashPathEffect
import android.graphics.Paint
import android.graphics.drawable.BitmapDrawable
import android.util.AttributeSet
import android.view.Display
import android.view.GestureDetector
import android.view.MotionEvent
import android.view.View
import android.view.WindowManager
import androidx.lifecycle.MutableLiveData
import com.scepticalphysiologist.dmaple.etc.Edge
import com.scepticalphysiologist.dmaple.etc.Frame
import com.scepticalphysiologist.dmaple.etc.Point
import com.scepticalphysiologist.dmaple.etc.ThresholdBitmap
import com.scepticalphysiologist.dmaple.map.MappingRoi

/** Gesture states for [MappingRoiOverlay]. */
enum class GestureState {
    DOUBLE_TAP,
    LONG_PRESS,
    OTHER
}

/** A view in which mapping ROIs ([MappingRoi]) can be created by finger gestures. This is
 * intended to overlay a camera preview so that ROIs can be drawn over the preview.
 *
 * A single ROI can be:
 * - created (by double tapping outside any existing ROI).
 * - expanded (by dragging its edges - see [fe]).
 * - moved (by dragging near its centre - see [ft]).
 * - it's map-seeding edge selected (by a long press near one edge).
 * - saved (by double tapping near its centre).
 *
 * There can be any number of "saved" ROIs. These are the ones to be mapped. A saved ROI can
 * be made the active ROI by double-tapping near its centre. The same maneuver does the reverse
 * (saves the active ROI). It follows that saved ROIs can not overlap near their centres.
 *
 * The active ROI is drawn with a solid boundary. Saved ROIs are drawn with dashed boundaries. The
 * map seeding edge is drawn thicker than the others.
 */
class MappingRoiOverlay(context: Context?, attributeSet: AttributeSet?):
    View(context, attributeSet),
    GestureDetector.OnDoubleTapListener,
    GestureDetector.OnGestureListener
{
    // Active ROI
    // ----------
    /** The "active" (i.e. currently being edited) ROI or null if there is no active ROI. */
    var activeRoi: MappingRoi? = null
    /** The paint for the [activeRoi]. */
    private val activeRoiPaint = Paint()
    /** A holder for the position of the active ROI as it is dragged. */
    private var drag = Point()
    /** The relative distance of a point from the centre of the ROI, under which it os considered
     * near the ROI's centre (see [Point.relativeDistance]) */
    private val ft: Float = 0.6f
    /** The relative distance of a point from the centre of the ROI, under which it os considered
     * near the ROI (see [Point.relativeDistance]) */
    private val fe: Float = 1.3f
    /** A bitmap drawn during thresholding of the active ROI, to visualize the threshold. */
    private var thresholdBitmap: ThresholdBitmap? = null
    /** Indicates that the active Roi has changed. The value is the threshold
     * of the active Roi (selected or newly created) or null is there is no active ROI. */
    val activeRoiChanged = MutableLiveData<Int?>(null)

    // Saved ROIs
    // ----------
    /** The saved ROIs that will be mapped. */
    val savedRois = mutableListOf<MappingRoi>()
    /** The paint for the saved ROIs. */
    private val savedRoiPaint = Paint()
    /** Indicates (if true) that the saved ROIs have changed (added to or subtracted from). */
    val savedRoiChange = MutableLiveData<Boolean>(false)

    // Ruler
    // -----
    /** A ruler for calibrating distance.*/
    private var ruler: Ruler? = null
    /** The paint for the ruler. */
    private val rulerPaint = Paint()

    // View information
    // ----------------
    /** Information about the display. */
    private val display: Display = (this.context.getSystemService(Context.WINDOW_SERVICE) as WindowManager).defaultDisplay
    /** This view's frame. */
    private var frame: Frame? = null

    // Gesture detection
    // -----------------
    /** General gesture detector. Needed to detect double tap and long press. */
    private val gestureDetector: GestureDetector
    /** Holder of gesture. This is required because of conflicting timing of
     * double tap and long press detection. */
    private var gesture: GestureState = GestureState.OTHER

    // State
    // -----
    /** The ROIs are editable - can be moved, expanded, saved, activated, etc. */
    private var editable: Boolean = true
    /** The index of a saved ROI that has been selected. */
    val selectedRoi = MutableLiveData<Int>(0)


    init {
        // Paints for active and saved ROIs
        activeRoiPaint.color = Color.RED
        activeRoiPaint.style = Paint.Style.STROKE
        activeRoiPaint.strokeWidth = 1.6f
        savedRoiPaint.color = Color.rgb(255, 150, 150)
        savedRoiPaint.style = Paint.Style.STROKE
        savedRoiPaint.setPathEffect(DashPathEffect(floatArrayOf(10f, 5f), 0f))
        savedRoiPaint.strokeWidth = 1.6f
        rulerPaint.color = Color.RED
        rulerPaint.style = Paint.Style.STROKE
        rulerPaint.strokeWidth = 3.2f

        // Gesture detection.
        gestureDetector = GestureDetector(this.context, this)
        gestureDetector.setOnDoubleTapListener(this)

        // Background of the view.
        // Normally is null (i.e. the view is transparent and so the camera preview can be seen).
        // When thresholding is begun, it is set to latest camera frame. i.e. the camera "freezes".
        background = null
    }

    // ---------------------------------------------------------------------------------------------
    // State and data
    // ---------------------------------------------------------------------------------------------

    /** Allow or block ROI editing. */
    fun allowEditing(allow: Boolean = true) { editable = allow }

    /** Set the saved ROIs. */
    fun setSavedRois(rois: List<MappingRoi>){
        savedRois.clear()
        for(roi in rois) savedRois.add(roi.copy())
        invalidate()
    }

    /** Change the active ROI or set it to null (no active ROI). */
    private fun changeActiveRoi(roi: MappingRoi?){
        activeRoi = roi
        activeRoiChanged.postValue(activeRoi?.threshold)
    }

    // ---------------------------------------------------------------------------------------------
    // Thresholding of active ROI
    // ---------------------------------------------------------------------------------------------

    /** Start thresholding the active ROI using the bitmap. */
    fun startThresholding(cameraShot: Bitmap) {
        activeRoi?.let { roi ->
            background = BitmapDrawable(cameraShot)
            thresholdBitmap = ThresholdBitmap.fromImage(cameraShot, roi)
            invalidate()
        }
    }

    /** Set the threshold of the active ROI. */
    fun setThreshold(threshold: Int) {
        thresholdBitmap?.let{
            it.updateThreshold(threshold.toFloat())
            activeRoi?.let{it.threshold = threshold}
            invalidate()
        }
    }

    /** Stop thresholding the active ROI. */
    fun stopThresholding(){
        background = null
        thresholdBitmap = null
        invalidate()
    }

    // ---------------------------------------------------------------------------------------------
    // ROI editing
    // ---------------------------------------------------------------------------------------------

    override fun onTouchEvent(event: MotionEvent?): Boolean {
        // Don't do anything if in threshold mode.
        if((event == null) || background != null) return super.onTouchEvent(event)

        // Detect gesture.
        val isLongPress = gesture == GestureState.LONG_PRESS
        gesture = GestureState.OTHER
        gestureDetector.onTouchEvent(event)
        val isDoubleClick = gesture == GestureState.DOUBLE_TAP

        // Touch point.
        val tp = Point.ofMotionEvent(event)

        // Double click on saved ROI?
        if(isDoubleClick && clickedOnSavedRoi(tp)) return true

        if(!editable) return true

        // Click on ruler?
        if(touchedRuler(tp, isDoubleClick)) return true

        // No active ROI: create a new ROI.
        if(activeRoi == null) {
            if(isDoubleClick) initiate(event)
            return true
        }

        // Active Roi ... (has to come after the above).
        activeRoi?.let {
            // Touch point relative to current ROI.
            val rp = tp.relativeDistance(it)
            val ap = rp.abs()
            // ... near centre
            if((ap.x < ft) && (ap.y < ft)) {
                // .... double-click: add ROI to list.
                if(isDoubleClick) saveActiveRoi()
                // ... otherwise: translate.
                else translate(event)
            }
            // ... near edge
            else if ((ap.x < fe) && (ap.y < fe)) {
                // ... long-press: set seeding edge.
                if(isLongPress) setSeedingEdge(rp)
                // ... otherwise expand.
                else expand(event, rp)
            }
            // ... double-click outside edge: de novo ROI.
            else if (isDoubleClick) initiate(event)
            // ... otherwise clear.
            else clearActiveRoi()
        }
        return true
    }

    /** Save the active ROI. */
    private fun saveActiveRoi() {
        activeRoi?.let {
            savedRois.add(it)
            savedRoiChange.postValue(true)
            clearActiveRoi()
        }
    }

    /** Clear the active ROI. */
    private fun clearActiveRoi() {
        changeActiveRoi(null)
        drag = Point(0f, 0f)
        invalidate()
    }

    private fun touchedRuler(touchPoint: Point, isDouble: Boolean): Boolean {
        ruler?.let {
            if((touchPoint - it.p0).l2() < 100) { if(isDouble) it.editLength(context) else it.p0 = touchPoint }
            else if((touchPoint - it.p1).l2() < 100) {if(isDouble) it.editLength(context) else it.p1 = touchPoint }
            else return false
            invalidate()
            return true
        }
        return false
    }

    private fun clickedOnSavedRoi(touchPoint: Point): Boolean {
        for(i in savedRois.indices) {
            val ap = touchPoint.relativeDistance(savedRois[i]).abs()
            if ((ap.x < ft) && (ap.y < ft)) {
                // If editable - change the saved ROI to the active,
                if(editable) {
                    changeActiveRoi(savedRois.removeAt(i))
                    savedRoiChange.postValue(true)
                    drag = touchPoint
                    invalidate()
                }
                // notify selection
                selectedRoi.postValue(i)
                return true
            }
        }
        return false
    }

    /** Translate the active ROI. */
    private fun translate(event: MotionEvent) {
        activeRoi?.let { roi ->
            if(event.action == MotionEvent.ACTION_MOVE){
                val dx = event.x - drag.x
                val dy = event.y - drag.y
                roi.left += dx
                roi.right += dx
                roi.bottom += dy
                roi.top += dy
                invalidate()
            }
            drag.x = event.x
            drag.y = event.y
        }
    }

    /** Expand the active ROI if the touch event is near its edge(s).
     * @param f The relative distance of the touch from the ROI centre.
     * */
    private fun expand(event: MotionEvent, f: Point) {
        if(event.action != MotionEvent.ACTION_MOVE) return
        activeRoi?.let { roi->
            if(f.x in -fe..-ft) roi.left = event.x
            else if(f.x in ft..fe) roi.right = event.x
            if(f.y in -fe..-ft) roi.top = event.y
            else if(f.y in ft..fe) roi.bottom = event. y
            invalidate()
        }
    }

    /** Set the seeding edge.
     * @param f The relative distance of the touch from the ROI centre.
     * */
    private fun setSeedingEdge(f: Point) {
        activeRoi?.let { roi ->
            if(f.x in -fe..-ft) roi.seedingEdge = Edge.LEFT
            else if(f.x in ft..fe) roi.seedingEdge = Edge.RIGHT
            if(f.y in -fe..-ft) roi.seedingEdge = Edge.TOP
            else if(f.y in ft..fe) roi.seedingEdge = Edge.BOTTOM
            invalidate()
        }
    }

    /** Initiate an active ROI de novo. */
    private fun initiate(event: MotionEvent) {
        if(event.action != MotionEvent.ACTION_DOWN) return
        changeActiveRoi(MappingRoi(Frame.fromView(this, display)))
        activeRoi?.let { roi ->
            roi.left = event.x - 50f
            roi.right = event.x + 50f
            roi.top = event.y  - 50f
            roi.bottom = event.y + 50f
            drag = Point(event.x, event.y)
            invalidate()
        }
    }


    // ---------------------------------------------------------------------------------------------
    // Drawing & layout
    // ---------------------------------------------------------------------------------------------

    override fun onDraw(canvas: Canvas) {
        super.onDraw(canvas)
        activeRoi?.draw(canvas, activeRoiPaint)
        for(roi in savedRois) roi.draw(canvas, savedRoiPaint)
        ruler?.draw(canvas, rulerPaint)
        thresholdBitmap?.draw(canvas)
    }

    override fun onLayout(changed: Boolean, left: Int, top: Int, right: Int, bottom: Int) {
        super.onLayout(changed, left, top, right, bottom)
        if(!changed) return
        val newFrame = Frame.fromView(this, display)
        // Create the ruler upon the first layout.
        if(ruler == null) initiateRuler(newFrame)
        // Transform the ROIs and ruler to any new orientation.
        frame?.let {
            activeRoi?.changeFrame(newFrame)
            for(roi in savedRois) roi.changeFrame(newFrame)
            ruler?.changeFrame(newFrame)
            invalidate()
        }
        // Update the view frame.
        frame = newFrame
    }

    private fun initiateRuler(frame: Frame) {
        val s = frame.size
        ruler = Ruler(
            frame=frame,
            p0 = Point(s.x * 0.5f, s.y * 0.05f),
            p1 = Point(s.x * 0.9f, s.y * 0.05f),
            end = 0.025f * minOf(s.x, s.y)
        )
    }

    // ---------------------------------------------------------------------------------------------
    // Gesture detection
    // ---------------------------------------------------------------------------------------------

    override fun onSingleTapConfirmed(p0: MotionEvent): Boolean { return false }

    override fun onDoubleTap(p0: MotionEvent): Boolean {
        gesture = GestureState.DOUBLE_TAP
        return true
    }

    override fun onDoubleTapEvent(p0: MotionEvent): Boolean { return false }

    override fun onDown(p0: MotionEvent): Boolean { return false }

    override fun onShowPress(p0: MotionEvent) { }

    override fun onSingleTapUp(p0: MotionEvent): Boolean { return false }

    override fun onScroll(p0: MotionEvent?, p1: MotionEvent, p2: Float, p3: Float): Boolean { return false }

    override fun onLongPress(p0: MotionEvent) { gesture = GestureState.LONG_PRESS }

    override fun onFling(p0: MotionEvent?, p1: MotionEvent, p2: Float, p3: Float): Boolean { return false }

}
