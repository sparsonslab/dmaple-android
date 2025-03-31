package com.scepticalphysiologist.dmaple.ui.record

import android.app.Activity
import android.content.Context
import android.graphics.Bitmap
import android.graphics.Canvas
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
import com.scepticalphysiologist.dmaple.R
import com.scepticalphysiologist.dmaple.geom.Edge
import com.scepticalphysiologist.dmaple.geom.Frame
import com.scepticalphysiologist.dmaple.geom.Point
import com.scepticalphysiologist.dmaple.map.field.RoisAndRuler
import com.scepticalphysiologist.dmaple.map.field.FieldImage
import com.scepticalphysiologist.dmaple.map.field.FieldRoi
import com.scepticalphysiologist.dmaple.map.creator.MapType
import com.scepticalphysiologist.dmaple.map.field.FieldRuler
import com.scepticalphysiologist.dmaple.ui.dialog.RoiInfo
import com.scepticalphysiologist.dmaple.ui.dialog.RulerInfo


/** Gesture states for [RoiOverlay]. */
enum class GestureState {
    DOUBLE_TAP,
    LONG_PRESS,
    OTHER
}

/** A view in which mapping ROIs ([FieldRoi]) can be created by finger gestures. This is
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
class RoiOverlay(context: Context?, attributeSet: AttributeSet?):
    View(context, attributeSet),
    GestureDetector.OnDoubleTapListener,
    GestureDetector.OnGestureListener
{
    // Active ROI
    // ----------
    /** The "active" (i.e. currently being edited) ROI or null if there is no active ROI. */
    var activeRoi: FieldRoi? = null
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
    val savedRois = mutableListOf<FieldRoi>()
    /** The paint for the saved ROIs. */
    private val savedRoiPaint = Paint()

    // Ruler
    // -----
    /** A ruler for calibrating distance.*/
    private var ruler: FieldRuler? = null
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
    /** The UID of a saved ROI that has been selected. */
    val selectedRoi = MutableLiveData<String>("")
    /** An image of a mapping field to be shown in this view's background
     * or null for a transparent background.
     *
     * Normally the background is transparent (null) but for thresholding and showing the field of
     * old recordings it will be fixed.
     * */
    private var backgroundField: FieldImage? = null

    init {
        // Paints for active and saved ROIs
        activeRoiPaint.color = resources.getColor(R.color.roi)
        activeRoiPaint.style = Paint.Style.STROKE
        activeRoiPaint.strokeWidth = 1.6f
        savedRoiPaint.color = resources.getColor(R.color.roi)
        savedRoiPaint.style = Paint.Style.STROKE
        savedRoiPaint.setPathEffect(DashPathEffect(floatArrayOf(10f, 5f), 0f))
        savedRoiPaint.strokeWidth = 1.6f
        rulerPaint.color = resources.getColor(R.color.ruler)
        rulerPaint.style = Paint.Style.STROKE
        rulerPaint.strokeWidth = 3.2f

        // Gesture detection.
        gestureDetector = GestureDetector(this.context, this)
        gestureDetector.setOnDoubleTapListener(this)

        // Background of the view.
        setBackgroundFromField(null)
    }

    // ---------------------------------------------------------------------------------------------
    // State and data
    // ---------------------------------------------------------------------------------------------

    /** Allow or block ROI editing. */
    fun allowEditing(allow: Boolean = true) { editable = allow }

    /** Set the view's background to a field image.
     * @param field The field image or null for a transparent background.
     * */
    fun setBacking(field: FieldImage?) {
        setBackgroundFromField(field?.copy())
        invalidate()
    }

    /** Get the saved ROIs and ruler. */
    fun getRoisAndRuler(): RoisAndRuler { return RoisAndRuler(savedRois, ruler) }

    /** Set the saved ROIs and ruler. */
    fun setRoisAndRuler(field: RoisAndRuler){
        savedRois.clear()
        for(roi in field.rois) savedRois.add(roi.copy())
        field.ruler?.let { ruler = it }
        invalidate()
    }

    /** Change the active ROI or set it to null (no active ROI). */
    private fun changeActiveRoi(roi: FieldRoi?){
        activeRoi = roi
        activeRoiChanged.postValue(activeRoi?.threshold)
    }

    // ---------------------------------------------------------------------------------------------
    // Thresholding of active ROI
    // ---------------------------------------------------------------------------------------------

    /** Start thresholding the active ROI using the bitmap. */
    fun startThresholding(cameraShot: Bitmap) {
        activeRoi?.let { roi ->
            setBackgroundFromField(FieldImage(Frame.fromView(this, display), cameraShot))
            thresholdBitmap = ThresholdBitmap.fromImage(cameraShot, roi.toRect())
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
        setBackgroundFromField(null)
        thresholdBitmap = null
        invalidate()
    }

    // ---------------------------------------------------------------------------------------------
    // ROI editing
    // ---------------------------------------------------------------------------------------------

    override fun onTouchEvent(event: MotionEvent?): Boolean {
        if(event == null) return super.onTouchEvent(event)

        // Detect gesture.
        val isLongPress = gesture == GestureState.LONG_PRESS
        gesture = GestureState.OTHER
        gestureDetector.onTouchEvent(event)
        val isDoubleClick = gesture == GestureState.DOUBLE_TAP

        // Touch point.
        val tp = Point.ofMotionEvent(event)

        // Double click on saved ROI?
        if(isDoubleClick && clickedOnSavedRoi(tp)) return true

        // Don't do anything else if not editable or fixed (non-live) background.
        if((!editable) || (background != null)) return true

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
            val rp = it.relativeDistance(tp)
            val ap = rp.abs()
            // ... near centre
            if((ap.x < ft) && (ap.y < ft)) {
                // ... double-click: add ROI to list.
                if(isDoubleClick) saveActiveRoi()
                // ... long-press: select map type(s).
                else if(isLongPress) requestMapTypes()
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

    /** Open a dialog for the user to set the map types for the active ROI. */
    private fun requestMapTypes() {
        activeRoi?.let { roi ->
            val roiInfo = RoiInfo(roi, this::setMapTypes)
            roiInfo.show(context as Activity)
        }
    }

    /** Callback for setting the active ROI's map types. */
    private fun setMapTypes(selected: List<MapType>) { activeRoi?.let { roi -> roi.maps = selected } }

    /** Save the active ROI. */
    private fun saveActiveRoi() {
        activeRoi?.let {
            it.cropToFrame()
            savedRois.add(it)
            clearActiveRoi()
        }
    }

    /** Clear the active ROI. */
    private fun clearActiveRoi() {
        changeActiveRoi(null)
        drag = Point(0f, 0f)
        invalidate()
    }

    /** Respond to a touch on the ruler. */
    private fun touchedRuler(touchPoint: Point, isDouble: Boolean): Boolean {
        ruler?.let {
            if((touchPoint - it.p0).l2() < 100) { if(isDouble) showRulerDialog(it) else it.p0 = touchPoint }
            else if((touchPoint - it.p1).l2() < 100) {if(isDouble) showRulerDialog(it) else it.p1 = touchPoint }
            else return false
            invalidate()
            return true
        }
        return false
    }

    /** Show the dialog to set the ruler length and units. */
    private fun showRulerDialog(ruler: FieldRuler) {
        RulerInfo(ruler, ::setRuler).show(context as Activity)
    }

    /** Set the ruler length and unit. */
    private fun setRuler(lengthAndUnit: Pair<Float, String>) { ruler?.newLength(lengthAndUnit) }

    private fun clickedOnSavedRoi(touchPoint: Point): Boolean {
        for(i in savedRois.indices) {
            val ap = savedRois[i].relativeDistance(touchPoint).abs()
            if ((ap.x < ft) && (ap.y < ft)) {
                // notify selection
                selectedRoi.postValue(savedRois[i].uid)
                // If editable - change the saved ROI to the active
                if(editable) {
                    changeActiveRoi(savedRois.removeAt(i))
                    drag = touchPoint
                    invalidate()
                }
                return true
            }
        }
        return false
    }

    /** Translate the active ROI. */
    private fun translate(event: MotionEvent) {
        activeRoi?.let { roi ->
            if(event.action == MotionEvent.ACTION_MOVE){
                roi.translate(Point(event.x - drag.x, event.y - drag.y))
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
        changeActiveRoi(FieldRoi(
            frame=Frame.fromView(this, display),
            c0 = Point(event.x - 50f, event.y - 50f),
            c1 = Point(event.x + 50f, event.y + 50f),
            maps= listOf(MapType.DIAMETER)
        ))
        activeRoi?.let {
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
            setBackgroundFromField()
            invalidate()
        }
        // Update the view frame.
        frame = newFrame
    }

    /** Set the background from a mapping field. */
    private fun setBackgroundFromField(field: FieldImage? = backgroundField) {
        backgroundField = field
        backgroundField?.changeFrame(Frame.fromView(this, display))
        background = backgroundField?.let{ BitmapDrawable(it.bitmap) }
    }

    private fun initiateRuler(frame: Frame) {
        val s = frame.size
        ruler = FieldRuler(
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
