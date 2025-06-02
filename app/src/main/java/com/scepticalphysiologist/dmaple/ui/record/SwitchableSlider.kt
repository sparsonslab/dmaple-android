// Copyright (c) 2025-2025. Dr Sean Paul Parsons. All rights reserved.

package com.scepticalphysiologist.dmaple.ui.record

import android.content.Context
import android.view.Gravity
import android.widget.FrameLayout
import android.widget.ImageButton
import android.widget.LinearLayout
import android.widget.SeekBar
import androidx.core.view.updateLayoutParams
import androidx.lifecycle.MutableLiveData
import kotlin.math.abs

/** Vertical slider with a "switch" at the bottom that changes its visibility.
 *
 * @property stateKey A key for saving the state of the slider when it is destroyed.
 * @property range The range of the slider.
 * @param switchIcon The resource id of the icon used for the switch button.
 * @param color The color of the slider.
 * */
class SwitchableSlider(
    context: Context,
    private val stateKey: String,
    private val range: Pair<Int, Int>,
    switchIcon: Int,
    color: Int
):
    LinearLayout(context),
    SeekBar.OnSeekBarChangeListener
{

    companion object {
        /** Progress state of the sliders. */
        private var progressState: MutableMap<String, Int> = mutableMapOf()
    }

    // Views
    // -----
    /** The slider. */
    private val slider = SeekBar(context)
    /** The switch to change slider visibility. */
    private val switch = ImageButton(context)

    // Live data signalling
    // --------------------
    /** Announce that the user has stopped or started tracking. */
    val onoff = MutableLiveData<Boolean>(false)
    /** Announce that the slide position had changed. */
    val position = MutableLiveData<Int>(0)
    /** Announce that the slide fractional position had changed. */
    val fractionalPosition = MutableLiveData<Float>(0f)

    init {
        this.orientation = VERTICAL
        this.layoutParams = LayoutParams(50, LayoutParams.MATCH_PARENT)

        // Padding
        val filler = FrameLayout(context)
        filler.layoutParams = LayoutParams(LayoutParams.MATCH_PARENT, LayoutParams.WRAP_CONTENT, 0.5f)
        this.addView(filler)

        // Slider
        val sliderFrame = FrameLayout(context)
        sliderFrame.layoutParams = LayoutParams(LayoutParams.MATCH_PARENT, LayoutParams.WRAP_CONTENT, 0.5f)
        slider.setOnSeekBarChangeListener(this)
        slider.min = range.first
        slider.max = range.second
        slider.thumb.setTint(color)
        slider.progressDrawable.setTint(color)
        slider.rotation = 270f
        // set width (really height as the slider is rotated) to anything BUT update in onLayout to half of screen height.
        sliderFrame.addView(slider,
            FrameLayout.LayoutParams(0, LayoutParams.MATCH_PARENT, Gravity.CENTER)
        )
        this.addView(sliderFrame)

        // Switch.
        switch.setImageResource(switchIcon)
        switch.background = null
        switch.setOnClickListener { switch(null) }
        // Give 0 weight - height will adjust to the needed size.
        this.addView(switch, LayoutParams(LayoutParams.MATCH_PARENT, LayoutParams.WRAP_CONTENT, 0f))
    }

    override fun onLayout(changed: Boolean, l: Int, t: Int, r: Int, b: Int) {
        super.onLayout(changed, l, t, r, b)
        if(!changed) return
        slider.updateLayoutParams { width = abs(0.45f * (b - t)).toInt() }
        progressState.get(stateKey)?.let{ setPosition(it) }
    }

    /** Switch the slider visibility. */
    fun switch(show: Boolean? = null){
        slider.visibility = show?.let{ if(it) VISIBLE else INVISIBLE } ?:
                if(slider.visibility == VISIBLE) INVISIBLE else VISIBLE
    }

    /** Set the position of the slider. */
    fun setPosition(p: Int) {
        slider.progress = when {
            p > slider.max -> slider.max
            p < slider.min -> slider.min
            else -> p
        }
        setProgressState()
    }

    override fun onProgressChanged(bar: SeekBar?, progress: Int, iniatedByUser: Boolean) {
        position.value = progress
        fractionalPosition.value = (progress - slider.min) / (slider.max - slider.min).toFloat()
        setProgressState()
    }

    override fun onStartTrackingTouch(p0: SeekBar?) { onoff.value = true }

    override fun onStopTrackingTouch(p0: SeekBar?) { onoff.value = false }

    private fun setProgressState() { progressState[stateKey] = slider.progress }

}
