package com.scepticalphysiologist.dmaple.etc

import android.content.Context
import android.util.AttributeSet
import android.view.Gravity
import android.view.View
import android.view.ViewGroup
import android.widget.Button
import android.widget.FrameLayout
import android.widget.FrameLayout.LayoutParams
import android.widget.ImageButton
import android.widget.LinearLayout
import android.widget.RelativeLayout
import android.widget.SeekBar
import androidx.constraintlayout.widget.ConstraintLayout
import androidx.constraintlayout.widget.ConstraintSet
import androidx.core.view.marginBottom
import androidx.core.view.marginTop
import androidx.core.view.updateLayoutParams
import androidx.lifecycle.MutableLiveData

class VerticalSlider(
    context: Context,
    private val range: Pair<Int, Int>,
    switchIcon: Int,
    color: Int
):
    ConstraintLayout(context),
    SeekBar.OnSeekBarChangeListener
{

    private val slider = SeekBar(context)

    private val switch = ImageButton(context)

    val onoff = MutableLiveData<Boolean>(false)

    val position = MutableLiveData<Int>(0)

    init {


        slider.rotation = 270f
        slider.setOnSeekBarChangeListener(this)
        slider.min = range.first
        slider.max = range.second
        slider.thumb.setTint(color)
        slider.progressDrawable.setTint(color)

        switch.setImageResource(switchIcon)
        switch.background = null
        switch.setOnClickListener {
            slider.visibility = if(slider.visibility == View.VISIBLE) View.INVISIBLE else View.VISIBLE
        }

        val slideFrame = FrameLayout(context)

        slideFrame.addView(
            slider, FrameLayout.LayoutParams(500, LayoutParams.MATCH_PARENT).also{
                it.gravity = Gravity.CENTER
                it.setMargins(20, 20, 20, 20)
            }
        )
        this.addView(
            slideFrame, LayoutParams(LayoutParams.MATCH_PARENT, LayoutParams.MATCH_PARENT).also {
                it.topToTop = LayoutParams.PARENT_ID
                it.bottomToTop = switch.id
            }
        )
        this.addView(
            switch, LayoutParams(LayoutParams.WRAP_CONTENT, LayoutParams.WRAP_CONTENT).also {
                it.bottomToBottom = LayoutParams.PARENT_ID
            }
        )

    }

    fun setPosition(p: Int) { this.slider.progress= p }

    override fun onLayout(changed: Boolean, left: Int, top: Int, right: Int, bottom: Int) {
        super.onLayout(changed, left, top, right, bottom)
        val layout = FrameLayout.LayoutParams((this.height * 0.6).toInt(), this.width)
        layout.gravity = Gravity.CENTER
        layout.setMargins(20, 20, 20, 20)
        slider.layoutParams = layout
        slider.requestLayout()
    }


    override fun onProgressChanged(p0: SeekBar?, p1: Int, p2: Boolean) { position.value = p1 }

    override fun onStartTrackingTouch(p0: SeekBar?) { onoff.value = true }

    override fun onStopTrackingTouch(p0: SeekBar?) { onoff.value = false }

}