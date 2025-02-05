package com.scepticalphysiologist.dmaple.etc

import android.content.Context
import android.util.AttributeSet
import android.view.Gravity
import android.widget.FrameLayout
import android.widget.SeekBar
import androidx.lifecycle.MutableLiveData

class VerticalSlider(
    context: Context, attributeSet: AttributeSet?,
    private val range: Pair<Int, Int>,
    color: Int
):
    FrameLayout(context, attributeSet),
    SeekBar.OnSeekBarChangeListener
{

    private val slider: SeekBar = SeekBar(context)

    val offset: Float get() = range.first.toFloat()

    val gain: Float get() = (range.second - range.first).toFloat()

    val onoff = MutableLiveData<Boolean>(false)

    val position = MutableLiveData<Int>(0)

    init {
        slider.rotation = 270f
        slider.setOnSeekBarChangeListener(this)
        slider.min = range.first
        slider.max = range.second
        slider.thumb.setTint(color)
        slider.progressDrawable.setTint(color)
        this.addView(slider)
    }

    fun setPosition(p: Int) { this.slider.progress= p }

    override fun onLayout(changed: Boolean, left: Int, top: Int, right: Int, bottom: Int) {
        super.onLayout(changed, left, top, right, bottom)
        val layout = LayoutParams(this.height, this.width)
        layout.gravity = Gravity.CENTER
        layout.setMargins(20, 20, 20, 20)
        slider.layoutParams = layout
        slider.requestLayout()
    }


    override fun onProgressChanged(p0: SeekBar?, p1: Int, p2: Boolean) { position.value = p1 }

    override fun onStartTrackingTouch(p0: SeekBar?) { onoff.value = true }

    override fun onStopTrackingTouch(p0: SeekBar?) { onoff.value = false }

}