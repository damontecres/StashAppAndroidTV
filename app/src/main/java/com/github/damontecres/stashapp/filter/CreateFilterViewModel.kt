package com.github.damontecres.stashapp.filter

import android.util.Log
import android.widget.Toast
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.apollographql.apollo.api.Optional
import com.apollographql.apollo.api.Query
import com.github.damontecres.stashapp.StashApplication
import com.github.damontecres.stashapp.api.fragment.StashData
import com.github.damontecres.stashapp.api.type.SaveFilterInput
import com.github.damontecres.stashapp.api.type.StashDataFilter
import com.github.damontecres.stashapp.data.DataType
import com.github.damontecres.stashapp.data.StashFindFilter
import com.github.damontecres.stashapp.filter.output.FilterWriter
import com.github.damontecres.stashapp.suppliers.DataSupplierFactory
import com.github.damontecres.stashapp.suppliers.FilterArgs
import com.github.damontecres.stashapp.suppliers.StashPagingSource
import com.github.damontecres.stashapp.util.MutationEngine
import com.github.damontecres.stashapp.util.QueryEngine
import com.github.damontecres.stashapp.util.StashCoroutineExceptionHandler
import com.github.damontecres.stashapp.util.StashServer
import kotlinx.coroutines.Job
import kotlinx.coroutines.launch
import kotlin.reflect.cast
import kotlin.reflect.full.createInstance

/**
 * Tracks state while the user builds a new filter
 */
class CreateFilterViewModel : ViewModel() {
    val server = MutableLiveData(StashServer.requireCurrentServer())
    val abbreviateCounters: Boolean get() = server.value!!.serverPreferences.abbreviateCounters
    val queryEngine = QueryEngine(server.value!!)

    val filterName = MutableLiveData<String?>(null)
    val dataType = MutableLiveData<DataType>()
    val objectFilter = MutableLiveData<StashDataFilter>()
    val findFilter = MutableLiveData<StashFindFilter>()

    val storedItems = mutableMapOf<DataTypeId, NameDescription>()

    val resultCount = MutableLiveData(-1)
    private var countJob: Job? = null

    private val currentSavedFilters = mutableMapOf<String?, String>()

    val ready = MutableLiveData(false)

    /**
     * Initialize the state
     */
    fun initialize(
        dataType: DataType,
        initialFilter: FilterArgs?,
    ) {
        ready.value = false

        this.dataType.value = dataType
        this.objectFilter.value =
            initialFilter?.objectFilter ?: dataType.filterType.createInstance()
        this.findFilter.value =
            initialFilter?.findFilter ?: StashFindFilter(sortAndDirection = dataType.defaultSort)
        this.filterName.value = initialFilter?.name

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
            ready.value = true
        }
        viewModelScope.launch(StashCoroutineExceptionHandler()) {
            queryEngine.getSavedFilters(dataType).forEach {
                currentSavedFilters[it.name] = it.id
            }
        }
    }

    /**
     * Update the object filter with the new sub-value
     */
    fun <ValueType : Any> updateFilter(
        filterOption: FilterOption<StashDataFilter, ValueType>,
        newItem: ValueType?,
    ) {
        Log.v(TAG, "updateFilter: name=${filterOption.name}, value==null: ${newItem == null}")
        val currFilter = objectFilter.value!!
        val newFilter =
            filterOption.setter(
                dataType.value!!.filterType.cast(currFilter),
                Optional.presentIfNotNull(newItem),
            )
        objectFilter.value = newFilter
    }

    /**
     * Update the [resultCount] using the current [findFilter] & [objectFilter]
     */
    fun updateCount() {
        resultCount.value = -1
        countJob?.cancel()
        countJob =
            viewModelScope.launch(
                StashCoroutineExceptionHandler { ex ->
                    Toast.makeText(
                        StashApplication.getApplication(),
                        "Error querying: ${ex.message}",
                        Toast.LENGTH_LONG,
                    )
                },
            ) {
                val supplier =
                    DataSupplierFactory(server.value!!.version).create<Query.Data, StashData, Query.Data>(
                        FilterArgs(
                            dataType = dataType.value!!,
                            findFilter = findFilter.value,
                            objectFilter = objectFilter.value,
                        ),
                    )
                val pagingSource =
                    StashPagingSource<Query.Data, StashData, Any, Query.Data>(queryEngine, supplier)
                resultCount.value = pagingSource.getCount()
            }
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
    ): Map<String, NameDescription?> =
        ids.associateWith { id ->
            val key = DataTypeId(dataType, id)
            storedItems[key]
        }

    fun getSavedFilterId(name: String?): String? = currentSavedFilters[name]

    /**
     * A composite of [DataType] and ID because IDs can be reused between data types
     */
    data class DataTypeId(
        val dataType: DataType,
        val id: String,
    )

    /**
     * A name (or title) and description of a [StashData] item
     */
    data class NameDescription(
        val name: String?,
        val description: String?,
    ) {
        constructor(item: StashData) : this(extractTitle(item), extractDescription(item))
    }

    fun createFilterArgs(): FilterArgs =
        FilterArgs(
            dataType = dataType.value!!,
            name = filterName.value,
            findFilter = findFilter.value,
            objectFilter = objectFilter.value,
        ).withResolvedRandom()

    suspend fun createSaveFilterInput(): SaveFilterInput {
        val queryEngine = QueryEngine(server.value!!)
        // Save it
        val filterWriter =
            FilterWriter(dataType.value!!) { dataType, ids ->
                queryEngine
                    .getByIds(dataType, ids)
                    .associate { it.id to extractTitle(it) }
            }
        val findFilter =
            findFilter.value ?: StashFindFilter(
                null,
                dataType.value!!.defaultSort,
            )
        val objectFilterMap = filterWriter.convertFilter(objectFilter.value!!)
        val existingId = getSavedFilterId(filterName.value)
        return SaveFilterInput(
            id = Optional.presentIfNotNull(existingId),
            mode = dataType.value!!.filterMode,
            name = filterName.value!!,
            find_filter =
                Optional.presentIfNotNull(
                    findFilter.toFindFilterType(1, 40),
                ),
            object_filter = Optional.presentIfNotNull(objectFilterMap),
            ui_options = Optional.absent(),
        )
    }

    suspend fun saveFilter() {
        val mutationEngine = MutationEngine(server.value!!)
        val input = createSaveFilterInput()
        mutationEngine.saveFilter(input)
    }

    companion object {
        private const val TAG = "CreateFilterViewModel"
    }
}
