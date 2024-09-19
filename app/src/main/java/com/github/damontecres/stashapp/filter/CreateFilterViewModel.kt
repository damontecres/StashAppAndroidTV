package com.github.damontecres.stashapp.filter

import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.apollographql.apollo.api.Optional
import com.github.damontecres.stashapp.api.fragment.StashData
import com.github.damontecres.stashapp.api.type.StashDataFilter
import com.github.damontecres.stashapp.data.DataType
import com.github.damontecres.stashapp.data.StashFindFilter
import com.github.damontecres.stashapp.util.QueryEngine
import com.github.damontecres.stashapp.util.StashCoroutineExceptionHandler
import com.github.damontecres.stashapp.util.StashServer
import kotlinx.coroutines.launch
import kotlin.reflect.cast
import kotlin.reflect.full.createInstance

class CreateFilterViewModel : ViewModel() {
    val server = MutableLiveData<StashServer>(StashServer.requireCurrentServer())
    val queryEngine = QueryEngine(server.value!!)

    val dataType = MutableLiveData<DataType>()
    val filter = MutableLiveData<StashDataFilter>()
    val findFilter = MutableLiveData<StashFindFilter>()

    val storedItems = mutableMapOf<DataTypeId, NameDescription>()

    fun initialize(
        dataType: DataType,
        initialFilter: StashDataFilter?,
        initialFindFilter: StashFindFilter?,
        callback: () -> Unit,
    ) {
        this.dataType.value = dataType
        this.filter.value = initialFilter ?: dataType.filterType.createInstance()
        this.findFilter.value =
            initialFindFilter ?: StashFindFilter(sortAndDirection = dataType.defaultSort)

        viewModelScope.launch(StashCoroutineExceptionHandler()) {
            getIdsByDataType(dataType.filterType, filter.value!!).entries.forEach {
                val dt = it.key
                val ids = it.value
                val items = queryEngine.getByIds(dt, ids)
                items.forEach { item ->
                    storedItems[DataTypeId(dt, item.id)] = NameDescription(item)
                }
            }
            callback()
        }
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

    fun store(
        dataType: DataType,
        item: StashData,
    ) {
        storedItems[DataTypeId(dataType, item.id)] = NameDescription((item))
    }

    fun lookupIds(
        dataType: DataType,
        ids: List<String>,
    ): Map<String, NameDescription?> {
        return ids.associateWith { id ->
            val key = DataTypeId(dataType, id)
            storedItems[key]
        }
    }

    data class DataTypeId(val dataType: DataType, val id: String)

    data class NameDescription(val name: String?, val description: String?) {
        constructor(item: StashData) : this(extractTitle(item), extractDescription(item))
    }
}
