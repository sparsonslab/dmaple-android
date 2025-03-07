package com.scepticalphysiologist.dmaple.ui.camera

import android.content.Context
import android.graphics.Canvas
import android.graphics.Color
import android.graphics.Matrix
import android.graphics.Paint
import android.util.AttributeSet
import android.view.View
import android.view.WindowManager
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
    private var spinePoints: FloatArray? = null

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
    // Public interface
    // ---------------------------------------------------------------------------------------------

    /** Update the map being shown. */
    fun updateCreator(creatorAndMapIdx: Pair<MapCreator?, Int>) {
        creator = creatorAndMapIdx.first
    }

    fun setUpdatingState(updating: Boolean) {
        if(updating) {
            updateCanvasTransform()
        } else {
            spinePoints = null
            invalidate()
        }
    }

    /** Update the spine shown. */
    fun update() {
        creator?.segmentor?.let { analyser ->
            // Plot every 10th point along the spine.
            spinePoints = Point.toFloatArray(analyser.spine.indices.filter{it % 10 == 0}.map{ k ->
                val i = analyser.longIdx[k].toFloat()
                val j = analyser.spine[k].toFloat()
                if(analyser.gutIsHorizontal) Point(i, j) else Point(j, i)
            })
            invalidate()
        }
    }

    // ---------------------------------------------------------------------------------------------
    // Spine update
    // ---------------------------------------------------------------------------------------------

    override fun onDraw(canvas: Canvas) {
        canvas.setMatrix(canvasTransform)
        spinePoints?.let{canvas.drawPoints(it, spinePaint)}
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
