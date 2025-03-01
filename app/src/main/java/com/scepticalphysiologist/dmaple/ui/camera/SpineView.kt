package com.scepticalphysiologist.dmaple.ui.camera

import android.content.Context
import android.graphics.Bitmap
import android.graphics.Canvas
import android.graphics.Color
import android.graphics.Matrix
import android.graphics.Paint
import android.graphics.Path
import android.graphics.RectF
import android.graphics.drawable.BitmapDrawable
import android.util.AttributeSet
import android.view.View
import android.view.WindowManager
import androidx.core.graphics.blue
import androidx.core.graphics.green
import androidx.core.graphics.red
import com.scepticalphysiologist.dmaple.etc.Frame
import com.scepticalphysiologist.dmaple.etc.Point
import com.scepticalphysiologist.dmaple.map.creator.MapCreator
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.MainScope
import kotlinx.coroutines.cancel
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch

class SpineView(context: Context, attributeSet: AttributeSet?): View(context, attributeSet) {


    /** The creator of the map spines being shown by this view. */
    private var creator: MapCreator? = null
    /** The coroutine scope used for the live (during mapping) extracting of the viewed bitmap
     * from the map creator. */
    private var scope: CoroutineScope? = null
    /** The approximate update interval (ms) for live display. */
    private val updateInterval: Long = 100L


    private val display = (context.getSystemService(Context.WINDOW_SERVICE) as WindowManager).defaultDisplay
    private var canvasMatrix: Matrix? = null

    private val spinePaint = Paint()
    private var spinePoints: FloatArray? = null


    init {
        spinePaint.color = Color.GREEN
        spinePaint.style = Paint.Style.STROKE
        spinePaint.strokeWidth = 1.9f
    }

    // ---------------------------------------------------------------------------------------------
    // Public interface
    // ---------------------------------------------------------------------------------------------

    /** Update the map being shown. */
    fun updateCreator(creatorAndMapIdx: Pair<MapCreator?, Int>) {
        creator = creatorAndMapIdx.first
    }

    /** Start live view of the map being created. */
    fun start() {
        if(scope != null) return
        scope = MainScope()
        updateLive()
    }

    /** Stop live view of the map being created. */
    fun stop() {
        scope?.cancel()
        scope = null
        spinePoints = null
        invalidate()
    }

    // ---------------------------------------------------------------------------------------------
    // Spine update
    // ---------------------------------------------------------------------------------------------

    /** Coroutine loop for updating the map live. */
    private fun updateLive() = scope?.launch(Dispatchers.Default){
        while(true) {
            update()
            delay(updateInterval)
        }
    }

    /** Update the map shown. */
    private fun update() {
        creator?.let { mapCreator ->
            val r = mapCreator.roi

            spinePoints = listOf(r.left, r.bottom, r.right, r.top).toFloatArray()

            /*
            spinePoints = Point.toFloatArray(mapCreator.spine.indices.map{ k ->
                val i = mapCreator.longIdx[k].toFloat()
                val j = mapCreator.spine[k].toFloat()
                if(mapCreator.gutIsHorizontal) Point(i, j) else Point(j, i)
            })

             */

            invalidate()
        }
    }

    override fun onLayout(changed: Boolean, left: Int, top: Int, right: Int, bottom: Int) {
        super.onLayout(changed, left, top, right, bottom)
        if(!changed) return
        creator?.let { mapCreator ->
            val canvasFrame = Frame.fromView(this, display)
            canvasMatrix = mapCreator.roi.frame.transformMatrix(canvasFrame, resize = true)
        }

    }

    override fun onDraw(canvas: Canvas) {
        canvas.setMatrix(canvasMatrix)
        spinePoints?.let{canvas.drawLines(it, spinePaint)}
    }

}
