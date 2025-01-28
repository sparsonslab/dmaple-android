package com.scepticalphysiologist.dmaple.ui.camera

import android.content.Context
import android.graphics.Bitmap
import android.graphics.Matrix
import android.graphics.Rect
import android.util.AttributeSet
import android.util.Size
import android.view.GestureDetector
import android.view.MotionEvent
import android.view.ScaleGestureDetector
import android.view.Surface
import android.view.WindowManager
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.findViewTreeLifecycleOwner
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.MainScope
import kotlinx.coroutines.cancel
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import kotlin.math.floor

/** A view for live display of a spatio-temporal map.
 *
 */
class MapView(context: Context, attributeSet: AttributeSet):
    androidx.appcompat.widget.AppCompatImageView(context, attributeSet),
    GestureDetector.OnGestureListener,
    ScaleGestureDetector.OnScaleGestureListener
{
    // Map creation
    // -------------
    /** The creator of the map being shown by this view. */
    private var creator: MapCreator? = null
    /** The coroutine scope used for the live (during mapping) extracting of the viewed bitmap
     * from the map creator. */
    private var scope: CoroutineScope? = null
    /** Used for passing the map's bitmap out of the coroutine into the view's main scope. */
    private var newBitmap = MutableLiveData<Bitmap?>(null)
    /** The approximate update interval (ms) for live display. */
    private val updateInterval: Long = 100L

    // Display
    // -------
    /** Information about the display. Needed for determining screen orientation. */
    private val display = (context.getSystemService(Context.WINDOW_SERVICE) as WindowManager).defaultDisplay

    // Gesture detection
    // ------------------
    /** General gesture detector. Needed to detect scrolling of the map. */
    private val gestureDetector: GestureDetector = GestureDetector(this.context, this)
    /** Scale gesture detector. Need to detect pinch zoom of the map. */
    private val scaleGestureDetector: ScaleGestureDetector = ScaleGestureDetector(this.context, this)
    /** Holder for the scale (distance between fingers) at the start of a pinch. */
    private var scaleStart = Point()

    // Space(x)-Time(y) pairs.
    // -------------------------
    /** The size of the view (x = space, y = time). */
    private var viewSize = Point()
    /** The size of the map's bitmap (x = space, y = time). */
    private var bitmapSize = Point()
    /** Zoom of the bitmap (x = space, y = time). */
    private var zoom = Point(1f, 1f)
    /** The ratio of bitmap pixels to view pixels, along the spatial axis at zoom = 1. */
    private var scale  = 1f
    /** Skipping of map pixels to display (x = space, y = time). */
    private var pixelStep = Point(1f, 1f)
    /** The size of the view in terms of bitmap pixels at the current zoom (x = space, y = time). */
    private var viewSizeInBitmapPixels = Point()
    /** A matrix used for rotating and scaling the map's bitmap. */
    private var bitmapMatrix: Matrix = Matrix()
    /** The offset of the end of shown map from the end of the view (x = space, y = time). */
    private val offset = Point(0f, 0f)

    // ---------------------------------------------------------------------------------------------
    // Map layout, scaling and zoom.
    // ---------------------------------------------------------------------------------------------

    /** Convert a screen coordinate to a space-time coordinate.
     * Time is always the larger dimension of the view.
     * */
    private fun spaceTimePoint(screenPoint: Point): Point {
        return if(width > height) screenPoint.swap() else screenPoint
    }

    /** The inverse of [timeSpacePoint]. */
    private fun screenPoint(timeSpacePoint: Point): Point { return spaceTimePoint(timeSpacePoint) }

    override fun onLayout(changed: Boolean, left: Int, top: Int, right: Int, bottom: Int) {
        super.onLayout(changed, left, top, right, bottom)
        if(changed) updateViewSize()
    }

    /** Update the view size [viewSize]. */
    private fun updateViewSize(){
        viewSize = spaceTimePoint(Point(width.toFloat(), height.toFloat()))
        updateScale()
        updateMatrix()
    }

    /** Update the size of the map's bitmap ([bitmapSize]). */
    private fun updateBitmapSize(size: Size){
        bitmapSize.y = size.height.toFloat()
        val w = size.width.toFloat()
        if(bitmapSize.x != w) {
            bitmapSize.x = w
            updateScale()
        }
    }

    /** Update the scale ([scale]). */
    private fun updateScale(){
        scale = bitmapSize.x / viewSize.x
        updateViewSizeInBitmapPixels()
    }

    /** Update the zoom ([zoom]). */
    private fun updateZoom(z: Point) {
        // Restrict zoom ranges.
        // Space (x) cannot be zoomed out to less than the width of the screen (zoom >= 1).
        zoom = Point.maxOf(Point.minOf(z, Point(4f, 5f)), Point(1f, 0.1f))
        // Skip time (y) pixels at low zoom.
        pixelStep.y = if(zoom.y < 0.5) floor(1f / zoom.y) else 1f
        // Update dependent variables.
        updateMatrix()
        updateViewSizeInBitmapPixels()
        // Restrict time anchor point. Cannot have an time offset if full time span of the bitmap
        // is within the view.
        if(bitmapSize.y < viewSizeInBitmapPixels.y) offset.y = 0f
    }

    private fun updateViewSizeInBitmapPixels() {
        viewSizeInBitmapPixels = viewSize * scale / zoom
    }

    /** Update the map's bitmap transformation matrix ([bitmapMatrix]). */
    private fun updateMatrix() {
        bitmapMatrix = Matrix()

        // Rotate so that the time (height) axis of the a map's bitmap is shown
        // along the long axis of the view.
        bitmapMatrix.setRotate(if(width > height) 90f else 0f)

        // Flip so that time goes from top>bottom or left>right and space matches ROI.
        // todo - Does this actually work on all tablets? Is this general??
        val s = screenPoint(zoom * pixelStep)
        when(display.rotation) {
            Surface.ROTATION_0 -> bitmapMatrix.postScale(-s.x, s.y)
            Surface.ROTATION_90 -> bitmapMatrix.postScale(-s.x, s.y)
            Surface.ROTATION_180 -> bitmapMatrix.postScale(s.x, s.y)
            Surface.ROTATION_270 -> bitmapMatrix.postScale(-s.x, -s.y)
        }
    }

    // ---------------------------------------------------------------------------------------------
    // Public interface
    // ---------------------------------------------------------------------------------------------

    /** Update the map being shown. */
    fun updateCreator(currentCreator: MapCreator?) { creator = currentCreator }

    /** Start live view of the map being created. */
    fun start() {
        if(scope != null) return
        findViewTreeLifecycleOwner()?.let{newBitmap.observe(it) {bm-> setImageBitmap(bm)} }
        scope = MainScope()
        updateMap()
    }

    /** Stop live view of the map being created. */
    fun stop() {
        findViewTreeLifecycleOwner()?.let {newBitmap.removeObservers(it)}
        scope?.cancel()
        scope = null
    }

    /** Reset the map view. */
    fun reset() { updateZoom(Point(1f, 1f)) }

    /** Coroutine for updating the map. */
    private fun updateMap() = scope?.launch(Dispatchers.Default){
        while(true) {
            creator?.let { mapCreator ->
                updateBitmapSize(mapCreator.size())
                // Extract section of the map as a bitmap.
                val pE = Point.minOf(bitmapSize, viewSizeInBitmapPixels)
                val p0 = Point.maxOf(bitmapSize - pE - offset, Point())
                val p1 = p0 + pE
                val bm = mapCreator.getImage(Rect(
                    p0.x.toInt(), p0.y.toInt(),
                    p1.x.toInt(), p1.y.toInt(),
                ), stepX = pixelStep.x.toInt(), stepY = pixelStep.y.toInt())
                // Rotate and scale the bitmap and post to the main thread for display.
                bm?.let {
                    newBitmap.postValue(Bitmap.createBitmap(
                        bm, 0, 0, bm.width, bm.height,
                        bitmapMatrix, false
                    ))
                }
            }
            delay(updateInterval)
        }
    }

    // ---------------------------------------------------------------------------------------------
    // Gesture reaction.
    // ---------------------------------------------------------------------------------------------

    /** Zoom the map from a finger pinch or stretch movement.
     *
     * @param s0 The scale (distance between fingers) at the start of the movement.
     * @param s1 The scale (distance between fingers) at the end of the movement.
     * @param f1 The focus (midpoint between fingers) at the end of the movement.
     */
    private fun fingerZoom(s0: Point, s1: Point, f1: Point) {
        // Distance of finger movement (< 0  pinch, > 0 stretch)
        // and zoom factor (> 1 zoom in, < 1 zoom out).
        val dFinger = spaceTimePoint(s1 - s0).abs()
        val zFactor = spaceTimePoint(s1 / s0)
        // Zoom only in one direction, of the larger finger movement.
        if(dFinger.x > dFinger.y) zFactor.y = 1f else zFactor.x = 1f
        // Zoom.
        updateZoom(zFactor * zoom)
    }

    /** Scroll the map from a scrolling finger movement.
     *
     * @param ds The change in finger position during a scroll.
     */
    private fun fingerScroll(ds: Point) {
        val bitmapShift = spaceTimePoint(ds) * scale / zoom
        // todo - Direction of shift should depend on orientation
        val shift = offset + bitmapShift
        if(shift.x > 0) offset.x = shift.x
        if(shift.y > 0) offset.y = shift.y
    }

    // ---------------------------------------------------------------------------------------------
    // Gesture detection
    // ---------------------------------------------------------------------------------------------

    /** Process a motion event passed by a parent view.
     * onTouchEvent() is called in children views before their parent. In this case it makes
     * sense for the map view's parent to process the touch event before it reaches here. Hence
     * this function.
     * */
    fun processMotionEvent(event: MotionEvent): Boolean {
        // todo - adjust event coordinates from parent to child using left/right?
        var res = scaleGestureDetector.onTouchEvent(event)
        res = gestureDetector.onTouchEvent(event) || res
        return res
    }

    override fun onDown(p0: MotionEvent): Boolean { return true }

    override fun onShowPress(p0: MotionEvent) { }

    override fun onSingleTapUp(p0: MotionEvent): Boolean { return true }

    override fun onScroll(down: MotionEvent?, current: MotionEvent, dx: Float, dy: Float): Boolean {
        if(scaleGestureDetector.isInProgress) return false
        fingerScroll(Point(dx, dy))
        return true
    }

    override fun onLongPress(p0: MotionEvent) { }

    override fun onFling(p0: MotionEvent?, p1: MotionEvent, p2: Float, p3: Float): Boolean { return true }

    override fun onScaleBegin(gd: ScaleGestureDetector): Boolean {
        scaleStart.x = gd.currentSpanX
        scaleStart.y = gd.currentSpanY
        return true
    }

    override fun onScale(gd: ScaleGestureDetector): Boolean { return true }

    override fun onScaleEnd(gd: ScaleGestureDetector) {
        fingerZoom(scaleStart, Point(gd.currentSpanX, gd.currentSpanY), Point(gd.focusX, gd.focusY))
    }
}
