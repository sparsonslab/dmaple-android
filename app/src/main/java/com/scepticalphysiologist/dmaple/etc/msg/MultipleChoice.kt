package com.scepticalphysiologist.dmaple.etc.msg

import android.content.Context
import android.content.DialogInterface
import android.text.Html
import android.text.Spanned
import android.view.View
import android.view.ViewGroup.LayoutParams
import android.widget.FrameLayout
import android.widget.LinearLayout
import android.widget.ListAdapter
import android.widget.RadioButton
import android.widget.TextView
import androidx.appcompat.app.AlertDialog


/** A message to provide multiple choices. */
class MultipleChoice(
    title: String = "Multiple Choice",
    val choices: MutableList<Pair<String, Boolean>>
):
    Message<List<Int>>(title, ""), // dialog cannot have a message and show multiple choice.
    DialogInterface.OnMultiChoiceClickListener
{

    override fun createDialog(context: Context): AlertDialog.Builder {
        val dialog =  super.createDialog(context)
        val text: Array<Spanned> = choices.map{ Html.fromHtml(it.first) }.toTypedArray()
        val choice: BooleanArray = choices.map{it.second}.toBooleanArray()
        dialog.setMultiChoiceItems(text, choice, this)
        return dialog
    }

    override fun show(context: Context) {
        val dialog = createDialog(context).create()
        dialog.show()
        val maxWidth = getWidestView(context, dialog.listView.adapter)
        dialog.window?.setLayout(maxWidth + 100, LayoutParams.WRAP_CONTENT)
    }

    override fun clickReturn(): List<Int> {
        return choices.indices.filter { choices[it].second }
    }

    override fun onClick(p0: DialogInterface?, idx: Int, chosen: Boolean) {
        if(idx < choices.size) choices[idx] = choices[idx].copy(second = chosen)
    }

}


fun getWidestView(context: Context, adapter: ListAdapter): Int {
    // https://stackoverflow.com/questions/6547154/wrap-content-for-a-listviews-width
    var maxWidth = 0
    var view: View? = null
    val fakeParent = FrameLayout(context)
    for(i in 0 until adapter.count){
        view = adapter.getView(i, view, fakeParent)
        view.measure(View.MeasureSpec.UNSPECIFIED, View.MeasureSpec.UNSPECIFIED)
        val width = view.measuredWidth
        if (width > maxWidth) maxWidth = width
    }
    return maxWidth
}


class ChoiceView(
    val choice: String,
    val chosen: Boolean,
    context: Context
): LinearLayout(context) {

    companion object {

        fun choicesView(context: Context, choices: MutableList<Pair<String, Boolean>>): LinearLayout {

            val layout = LinearLayout(context)
            layout.layoutParams = LayoutParams(LayoutParams.WRAP_CONTENT, LayoutParams.WRAP_CONTENT)
            layout.orientation = LinearLayout.VERTICAL

            for((choice, chosen) in choices){
                layout.addView(ChoiceView(choice, chosen, context))
            }
            return layout

        }

    }

    init {
        orientation = LinearLayout.HORIZONTAL
        layoutParams = LayoutParams(LayoutParams.WRAP_CONTENT, LayoutParams.WRAP_CONTENT)

        val chooser = RadioButton(context)
        chooser.isChecked = chosen
        addView(chooser)

        val text = TextView(context)
        text.text = choice
        text.maxWidth = 200
        text.layoutParams = LayoutParams(LayoutParams.WRAP_CONTENT, LayoutParams.WRAP_CONTENT)
        addView(text)

    }

}
