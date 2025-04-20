// Copyright (c) 2025-2025. Dr Sean Paul Parsons. All rights reserved.

package com.scepticalphysiologist.dmaple.ui.dialog

import android.app.Activity
import android.view.ViewGroup
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.platform.ComposeView
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.sp

/** A composable alert dialog that can be shown from a fragment or view. */
abstract class ComposeDialog {

    protected val titleFontSize = 18f.sp

    protected val titleFontWeight = FontWeight.Bold

    protected val mainFontSize = 12f.sp

    /** The dialog composable.*/
    @Composable
    abstract fun MakeDialog()

    /** Show the dialog. */
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
