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

/**
 * Tracks state while the user builds a new filter
 */
class CreateFilterViewModel : ViewModel() {
    val server = MutableLiveData<StashServer>(StashServer.requireCurrentServer())
    val queryEngine = QueryEngine(server.value!!)

    val dataType = MutableLiveData<DataType>()
    val objectFilter = MutableLiveData<StashDataFilter>()
    val findFilter = MutableLiveData<StashFindFilter>()

    val storedItems = mutableMapOf<DataTypeId, NameDescription>()

    /**
     * Initialize the state
     */
    fun initialize(
        dataType: DataType,
        initialFilter: StashDataFilter?,
        initialFindFilter: StashFindFilter?,
        callback: () -> Unit,
    ) {
        this.dataType.value = dataType
        this.objectFilter.value = initialFilter ?: dataType.filterType.createInstance()
        this.findFilter.value =
            initialFindFilter ?: StashFindFilter(sortAndDirection = dataType.defaultSort)

        // Fetch all of the labels for any existing IDs in the initial object filter
        viewModelScope.launch(StashCoroutineExceptionHandler()) {
            getIdsByDataType(dataType, objectFilter.value!!).entries.forEach {
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

    /**
     * Update the object filter with the new sub-value
     */
    fun <ValueType : Any> updateFilter(
        filterOption: FilterOption<StashDataFilter, ValueType>,
        newItem: ValueType?,
    ) {
        val currFilter = objectFilter.value!!
        val newFilter =
            filterOption.setter(
                dataType.value!!.filterType.cast(currFilter),
                Optional.presentIfNotNull(newItem),
            )
        objectFilter.value = newFilter
    }

    /**
     * Get the sub-value for the current object filter
     */
    fun <ValueType : Any> getValue(filterOption: FilterOption<StashDataFilter, ValueType>): ValueType? {
        val currFilter = objectFilter.value!!
        val value = filterOption.getter(dataType.value!!.filterType.cast(currFilter))
        return value.getOrNull()
    }

    /**
     * Store an item's name & description for label purposes
     */
    fun store(
        dataType: DataType,
        item: StashData,
    ) {
        storedItems[DataTypeId(dataType, item.id)] = NameDescription((item))
    }

    /**
     * Get all of the name & descriptions for a list of IDs and [DataType]
     */
    fun lookupIds(
        dataType: DataType,
        ids: List<String>,
    ): Map<String, NameDescription?> {
        return ids.associateWith { id ->
            val key = DataTypeId(dataType, id)
            storedItems[key]
        }
    }

    /**
     * A composite of [DataType] and ID because IDs can be reused between data types
     */
    data class DataTypeId(val dataType: DataType, val id: String)

    /**
     * A name (or title) and description of a [StashData] item
     */
    data class NameDescription(val name: String?, val description: String?) {
        constructor(item: StashData) : this(extractTitle(item), extractDescription(item))
    }
}
