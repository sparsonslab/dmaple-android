package com.scepticalphysiologist.dmaple.ui

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.MutableLiveData
import com.scepticalphysiologist.dmaple.ui.helper.Warnings

class RecorderModel(application: Application) : AndroidViewModel(application) {


    val warnings = MutableLiveData<Warnings>()




}