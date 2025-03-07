package com.scepticalphysiologist.dmaple.ui.camera

import android.content.Context
import android.graphics.Canvas
import android.graphics.Color
import android.graphics.Matrix
import android.graphics.Paint
import android.util.AttributeSet
import android.view.View
import android.view.WindowManager
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.findViewTreeLifecycleOwner
import com.scepticalphysiologist.dmaple.etc.Frame
import com.scepticalphysiologist.dmaple.etc.Point
import com.scepticalphysiologist.dmaple.map.creator.MapCreator

/** A view for o showing a mapping spine and boundaries over the camera field. */
class SpineOverlay(context: Context, attributeSet: AttributeSet?): View(context, attributeSet) {

    // Map and creator
    // ---------------
    /** The creator of the map spines being shown by this view. */
    private var creator: MapCreator? = null
    /** The spine points to be plotted. */
    private var spinePoints =  MutableLiveData<FloatArray?>(null)

    // Drawing
    // -------
    /** Information about the display. */
    private val display = (context.getSystemService(Context.WINDOW_SERVICE) as WindowManager).defaultDisplay
    /** The geometric transform matrix applied to the canvas so that points drawn onto to it
     * have the same frame as the map's spine (i.e. the camerax analysis frame) */
    private var canvasTransform: Matrix? = null
    /** The paint used for drawing the spine. */
    private val spinePaint = Paint()

    init {
        spinePaint.color = Color.GREEN
        spinePaint.style = Paint.Style.FILL
        spinePaint.strokeWidth = 5f
    }

    // ---------------------------------------------------------------------------------------------
    // Spine update
    // ---------------------------------------------------------------------------------------------

    /** Update the map being shown. */
    fun updateCreator(creatorAndMapIdx: Pair<MapCreator?, Int>) {
        creator = creatorAndMapIdx.first
    }

    /** Set parameters related to whether the spine is being updated live. */
    fun setLiveUpdateState(updating: Boolean) {
        if(updating) updateCanvasTransform() else spinePoints.postValue(null)
    }

    /** Update the spine.
     *
     * This is intended to be run within a coroutine. Therefore the coordinates of the spine
     * to be shown are posted as live data so that it can be drawn in the main UI thread.
     * */
    fun update() {
        creator?.segmentor?.let { analyser ->
            // Update the spine and post to the main thread for UI display.
            // Plot every 10th point along the spine.
            spinePoints.postValue(Point.toFloatArray(analyser.spine.indices.filter{it % 10 == 0}.map{ k ->
                val i = analyser.longIdx[k].toFloat()
                val j = analyser.spine[k].toFloat()
                if(analyser.gutIsHorizontal) Point(i, j) else Point(j, i)
            }))
        }
    }

    override fun onAttachedToWindow() {
        super.onAttachedToWindow()
        findViewTreeLifecycleOwner()?.let { spinePoints.observe(it) { invalidate() } }
    }

    // ---------------------------------------------------------------------------------------------
    // Spine update
    // ---------------------------------------------------------------------------------------------

    override fun onDraw(canvas: Canvas) {
        canvas.setMatrix(canvasTransform)
        spinePoints.value?.let{canvas.drawPoints(it, spinePaint)}
    }

    override fun onLayout(changed: Boolean, left: Int, top: Int, right: Int, bottom: Int) {
        super.onLayout(changed, left, top, right, bottom)
        if(!changed) return
        updateCanvasTransform()
    }

    private fun updateCanvasTransform() {
        creator?.let { mapCreator ->
            val canvasFrame = Frame.fromView(this, display)
            canvasTransform = mapCreator.roi.frame.transformMatrix(canvasFrame, resize = true)
        }
    }
}
