package com.scepticalphysiologist.dmaple.ui.dialog

import android.app.Activity
import android.view.ViewGroup
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.platform.ComposeView
import androidx.compose.ui.unit.sp

abstract class ComposeDialog {

    protected val titleFontSize = 18f.sp

    protected val mainFontSize = 12f.sp

    @Composable
    abstract fun MakeDialog()

    open fun show(activity: Activity) {
        activity.addContentView(
            ComposeView(activity).apply {
                setContent {
                    var showDialog by remember { mutableStateOf(true) }
                    if (showDialog) MakeDialog()
                }
            },
            ViewGroup.LayoutParams(
                ViewGroup.LayoutParams.MATCH_PARENT,
                ViewGroup.LayoutParams.MATCH_PARENT
            )
        )
    }

}
