package com.scepticalphysiologist.dmaple.ui.record

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
import com.scepticalphysiologist.dmaple.geom.Point
import com.scepticalphysiologist.dmaple.etc.transformBitmap
import com.scepticalphysiologist.dmaple.map.creator.MapCreator
import kotlin.math.floor

/** A view for live display of a spatio-temporal map.
 *
 * For live update:
 * - A static backing array is passed to a creator.
 * - The creator produces a bitmap (backed by the passed array) of the area of the map to be viewed.
 * - The bitmap returned by the creator is further transformed** (rotated and scaled) and passed
 * to the view for display.
 *
 *  **NOTE: It would be nice to have the creator produce a scaled/rotated bitmap and then pass this
 * straight to the view for display but unfortunately this causes all sorts of aliasing problems
 * (I tried it!) because the backing array is being updated by the creator at the same time
 * as being sent to the view.
 */
class MapView(context: Context, attributeSet: AttributeSet):
    androidx.appcompat.widget.AppCompatImageView(context, attributeSet),
    GestureDetector.OnGestureListener,
    ScaleGestureDetector.OnScaleGestureListener
{

    companion object {
        /** Backing array for the shown map's bitmap.
         *
         * Originally I had each [MapCreator] create a backing IntArray each time it was called
         * to create a bitmap. However this caused an out-of-memory exception after a few minutes.
         * After profiling the app, it was found that multiple instances of the array were
         * accumulating. This is prob due to Android's awful garbage collection of bitmaps. Using
         * a backing array attribute avoids memory allocation on each call and then releasing the
         * backing with bitmap.release().
         * https://developer.android.com/topic/performance/graphics/manage-memory
         * https://stackoverflow.com/questions/4959485/bitmap-bitmap-recycle-weakreferences-and-garbage-collection
         * */
        val bitmapBacking = IntArray(1_000_000)

    }

    // Map creation
    // -------------
    /** The creator of the map being shown by this view. */
    private var creator: MapCreator? = null
    /** The ith map of the creator being shown by this view (for where a creator makes more than one map). */
    private var mapIdx: Int = 0
    /** Used for passing the map's bitmap out of the coroutine into the main UI thread. */
    private var newBitmap = MutableLiveData<Bitmap?>(null)
    private var updating: Boolean = false

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
        if(changed) {
            updateViewSize()
            if(!updating) update()
        }
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
        //pixelStep.y = if(zoom.y < 0.5) floor(1f / zoom.y) else 1f


        // Update dependent variables.
        updateViewSizeInBitmapPixels()
        updateMatrix()
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

        pixelStep = (viewSizeInBitmapPixels / Point(100f, 500f)).ceil()
        if(pixelStep.x < 1) pixelStep.x = 1f
        if(pixelStep.y < 1) pixelStep.y = 1f

       // val s = screenPoint(zoom * pixelStep)
        val s = screenPoint(zoom * pixelStep)
        when(display.rotation) {
            Surface.ROTATION_0 -> bitmapMatrix.postScale(-s.x, s.y)
            Surface.ROTATION_90 -> bitmapMatrix.postScale(-s.x, s.y)
            Surface.ROTATION_180 -> bitmapMatrix.postScale(s.x, s.y)
            Surface.ROTATION_270 -> bitmapMatrix.postScale(-s.x, -s.y)
        }
    }

    // ---------------------------------------------------------------------------------------------
    // Map update
    // ---------------------------------------------------------------------------------------------

    /** Update the map being shown. */
    fun updateCreator(creatorAndMapIdx: Pair<MapCreator?, Int>) {
        creator = creatorAndMapIdx.first
        mapIdx = creatorAndMapIdx.second
        if(!updating) update()
    }

    /** Set parameters related to whether the map is being updated live.  */
    fun setLiveUpdateState(updating: Boolean) { this.updating = updating }

    /** Update the map shown.
     *
     * This is intended to be run within a coroutine. Therefore the bitmap of the section of map
     * to be shown is posted as live data so that it can be displayed in the main UI thread.
     * */
    fun update() {
        creator?.let { mapCreator ->
            // Update size.
            updateBitmapSize(mapCreator.spaceTimeSampleSize())
            // Extract section of the map as a bitmap.
            val pE = Point.minOf(bitmapSize, viewSizeInBitmapPixels)
            val p0 = Point.maxOf(bitmapSize - pE - offset, Point())
            val p1 = p0 + pE
            mapCreator.getMapBitmap(
                idx = mapIdx,
                crop = Rect(p0.x.toInt(), p0.y.toInt(), p1.x.toInt(), p1.y.toInt()),
                stepX = pixelStep.x.toInt(), stepY = pixelStep.y.toInt(),
                backing = bitmapBacking,

            )?.let { bm ->
                // Rotate and scale the bitmap and post to the main thread for display.
                newBitmap.postValue(transformBitmap(bm, bitmapMatrix)
            )}
        }
    }

    override fun onAttachedToWindow() {
        super.onAttachedToWindow()
        findViewTreeLifecycleOwner()?.let{ newBitmap.observe(it) { bm-> setImageBitmap(bm) } }
    }

    /** Reset the map view. */
    fun reset() { updateZoom(Point(1f, 1f)) }

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
        if(!updating) update()
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
        if(!updating) update()
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
