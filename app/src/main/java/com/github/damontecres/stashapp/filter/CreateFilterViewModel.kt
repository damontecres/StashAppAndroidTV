package com.github.damontecres.stashapp.filter

import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.ViewModel
import com.github.damontecres.stashapp.api.type.SceneFilterType
import com.github.damontecres.stashapp.data.StashFindFilter
import com.github.damontecres.stashapp.util.StashServer

class CreateFilterViewModel : ViewModel() {
    val server = MutableLiveData<StashServer>(StashServer.requireCurrentServer())

    val filter = MutableLiveData<SceneFilterType>()
    val findFilter = MutableLiveData(StashFindFilter())

    fun <ValueType : Any> updateFilter(
        filterOption: FilterOption<SceneFilterType, ValueType>,
        newItem: ValueType?,
    ) {
        filter.value = filterOption.setter(filter.value!!, newItem)
    }
}
