package com.scepticalphysiologist.dmaple.etc

import android.content.Context
import android.view.Gravity
import android.view.View
import android.widget.FrameLayout.LayoutParams
import android.widget.ImageButton
import android.widget.LinearLayout
import android.widget.SeekBar
import androidx.lifecycle.MutableLiveData

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
        this.orientation = LinearLayout.HORIZONTAL
        switch.setImageResource(switchIcon)
        switch.background = null
        switch.setOnClickListener { switch(null) }
        this.addView(switch)

        slider.setOnSeekBarChangeListener(this)
        slider.min = range.first
        slider.max = range.second
        slider.thumb.setTint(color)
        slider.progressDrawable.setTint(color)
        this.addView(slider, LayoutParams(LayoutParams.MATCH_PARENT, LayoutParams.MATCH_PARENT, Gravity.CENTER))
    }

    fun switch(show: Boolean? = null){
        slider.visibility = show?.let{ if(it) View.VISIBLE else View.INVISIBLE } ?:
        if(slider.visibility == View.VISIBLE) View.INVISIBLE else View.VISIBLE
    }

    fun setPosition(p: Int) { this.slider.progress= p }

    override fun onProgressChanged(p0: SeekBar?, p1: Int, p2: Boolean) { position.value = p1 }

    override fun onStartTrackingTouch(p0: SeekBar?) { onoff.value = true }

    override fun onStopTrackingTouch(p0: SeekBar?) { onoff.value = false }

}
