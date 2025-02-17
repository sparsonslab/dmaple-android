package com.scepticalphysiologist.dmaple.etc

import android.content.Context
import android.view.Gravity
import android.view.View
import android.widget.FrameLayout
import android.widget.FrameLayout.LayoutParams
import android.widget.ImageButton
import android.widget.LinearLayout
import android.widget.SeekBar
import androidx.core.view.updateLayoutParams
import androidx.lifecycle.MutableLiveData
import kotlin.math.abs

class SwitchableSlider(
    context: Context,
    private val range: Pair<Int, Int>,
    switchIcon: Int,
    color: Int
):
    LinearLayout(context),
    SeekBar.OnSeekBarChangeListener
{

    private val slider = SeekBar(context)

    private val switch = ImageButton(context)

    val onoff = MutableLiveData<Boolean>(false)

    val position = MutableLiveData<Int>(0)

    init {
        this.orientation = LinearLayout.VERTICAL
        this.layoutParams = LayoutParams(50, LayoutParams.MATCH_PARENT)

        // Padding
        val filler = FrameLayout(context)
        filler.layoutParams = LinearLayout.LayoutParams(LayoutParams.MATCH_PARENT, LayoutParams.WRAP_CONTENT, 0.5f)
        this.addView(filler)

        // Slider
        val sliderFrame = FrameLayout(context)
        sliderFrame.layoutParams = LinearLayout.LayoutParams(LayoutParams.MATCH_PARENT, LayoutParams.WRAP_CONTENT, 0.5f)
        sliderFrame.setPadding(0, 10, 0, 10)
        slider.setOnSeekBarChangeListener(this)
        slider.min = range.first
        slider.max = range.second
        slider.thumb.setTint(color)
        slider.progressDrawable.setTint(color)
        slider.rotation = 270f
        // set width (really height as the slider is rotated) to anything BUT update in onLayout to half of screen height.
        sliderFrame.addView(slider, LayoutParams(0, LayoutParams.MATCH_PARENT, Gravity.CENTER))
        this.addView(sliderFrame)

        // Switch.
        switch.setImageResource(switchIcon)
        switch.background = null
        switch.setOnClickListener { switch(null) }
        // Give 0 weight - height will adjust to the needed size.
        this.addView(switch, LinearLayout.LayoutParams(LayoutParams.MATCH_PARENT, LayoutParams.WRAP_CONTENT, 0f))
    }


    override fun onLayout(changed: Boolean, l: Int, t: Int, r: Int, b: Int) {
        super.onLayout(changed, l, t, r, b)
        slider.updateLayoutParams { width = abs(0.45f * (b - t)).toInt() }
    }

    fun switch(show: Boolean? = null){
        slider.visibility = show?.let{ if(it) View.VISIBLE else View.INVISIBLE } ?:
                if(slider.visibility == View.VISIBLE) View.INVISIBLE else View.VISIBLE
    }

    fun setPosition(p: Int) {
        this.slider.progress = p
        println("slider = $p")
    }

    override fun onProgressChanged(p0: SeekBar?, p1: Int, p2: Boolean) {
        position.value = p1
        println("pos = $p1")
    }

    override fun onStartTrackingTouch(p0: SeekBar?) { onoff.value = true }

    override fun onStopTrackingTouch(p0: SeekBar?) { onoff.value = false }

}