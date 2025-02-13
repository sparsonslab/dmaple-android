package com.scepticalphysiologist.dmaple.etc.msg

import android.content.Context
import android.content.DialogInterface
import android.text.Html
import android.text.Spanned
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

    override fun clickReturn(): List<Int> {
        return choices.indices.filter { choices[it].second }
    }

    override fun onClick(p0: DialogInterface?, idx: Int, chosen: Boolean) {
        if(idx < choices.size) choices[idx] = choices[idx].copy(second = chosen)
    }

}
