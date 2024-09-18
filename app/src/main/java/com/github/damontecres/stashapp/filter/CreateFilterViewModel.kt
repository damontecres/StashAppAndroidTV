package com.github.damontecres.stashapp.filter

import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.ViewModel
import com.apollographql.apollo.api.Optional
import com.github.damontecres.stashapp.api.type.StashDataFilter
import com.github.damontecres.stashapp.data.DataType
import com.github.damontecres.stashapp.data.StashFindFilter
import com.github.damontecres.stashapp.util.StashServer
import kotlin.reflect.cast
import kotlin.reflect.full.createInstance

class CreateFilterViewModel : ViewModel() {
    val server = MutableLiveData<StashServer>(StashServer.requireCurrentServer())

    val dataType = MutableLiveData<DataType>()
    val filter = MutableLiveData<StashDataFilter>()
    val findFilter = MutableLiveData<StashFindFilter>()

    fun initialize(
        dataType: DataType,
        initialFilter: StashDataFilter?,
        initialFindFilter: StashFindFilter?,
    ) {
        this.dataType.value = dataType
        this.filter.value = initialFilter ?: dataType.filterType.createInstance()
        this.findFilter.value =
            initialFindFilter ?: StashFindFilter(sortAndDirection = dataType.defaultSort)
    }

    fun <ValueType : Any> updateFilter(
        filterOption: FilterOption<StashDataFilter, ValueType>,
        newItem: ValueType?,
    ) {
        val currFilter = filter.value!!
        val newFilter =
            filterOption.setter(
                dataType.value!!.filterType.cast(currFilter),
                Optional.presentIfNotNull(newItem),
            )
        filter.value = newFilter
    }

    fun <ValueType : Any> getValue(filterOption: FilterOption<StashDataFilter, ValueType>): ValueType? {
        val currFilter = filter.value!!
        val value = filterOption.getter(dataType.value!!.filterType.cast(currFilter))
        return value.getOrNull()
    }
}
