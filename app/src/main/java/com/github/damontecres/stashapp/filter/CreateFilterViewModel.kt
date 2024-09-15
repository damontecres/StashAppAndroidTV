package com.github.damontecres.stashapp.filter

import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.ViewModel
import com.github.damontecres.stashapp.api.type.SceneFilterType

class CreateFilterViewModel : ViewModel() {
    val filter = MutableLiveData<SceneFilterType>()
}
