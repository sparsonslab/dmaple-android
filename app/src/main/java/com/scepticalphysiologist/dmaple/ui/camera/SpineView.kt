package com.scepticalphysiologist.dmaple.ui.camera

import android.content.Context
import android.graphics.Bitmap
import android.graphics.Canvas
import android.graphics.Color
import android.graphics.Paint
import android.graphics.Path
import android.graphics.RectF
import android.graphics.drawable.BitmapDrawable
import android.util.AttributeSet
import android.view.View
import androidx.core.graphics.blue
import androidx.core.graphics.green
import androidx.core.graphics.red

class SpineView(context: Context?, attributeSet: AttributeSet?): View(context, attributeSet) {


    val spines = mutableListOf<Path>()
    val spinePaint = Paint()

    init {
        spinePaint.color = Color.GREEN
        spinePaint.style = Paint.Style.STROKE
        spinePaint.strokeWidth = 0.8f
    }



    // ---------------------------------------------------------------------------------------------
    // Spines
    // ---------------------------------------------------------------------------------------------


    fun drawSpine(bm: Bitmap, roi: RectF) {

        val p = Path()
        p.setLastPoint(roi.left, roi.top)
        p.lineTo(roi.right, roi.bottom)
        val c = bm.getPixel(roi.left.toInt(), roi.top.toInt())
        println("RGB, L = ${c.red}, ${c.green}, ${c.blue}")
        spines.add(p)
        invalidate()
    }



    fun clear() {
        spines.clear()
        invalidate()
    }

    override fun onDraw(canvas: Canvas) {
        for(spine in spines) canvas.drawPath(spine, spinePaint)

    }

}