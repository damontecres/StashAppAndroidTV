package com.github.damontecres.stashapp.filter

import android.app.Application
import android.util.Log
import android.widget.Toast
import androidx.compose.ui.text.AnnotatedString
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.apollographql.apollo.api.Optional
import com.apollographql.apollo.api.Query
import com.github.damontecres.stashapp.R
import com.github.damontecres.stashapp.StashApplication
import com.github.damontecres.stashapp.api.fragment.StashData
import com.github.damontecres.stashapp.api.type.SaveFilterInput
import com.github.damontecres.stashapp.api.type.StashDataFilter
import com.github.damontecres.stashapp.data.DataType
import com.github.damontecres.stashapp.data.StashFindFilter
import com.github.damontecres.stashapp.di.server.MutationEngine
import com.github.damontecres.stashapp.di.server.QueryEngine
import com.github.damontecres.stashapp.di.server.ServerRepository
import com.github.damontecres.stashapp.di.services.InterfaceService
import com.github.damontecres.stashapp.di.services.NavigationManager
import com.github.damontecres.stashapp.di.services.ServerLogger
import com.github.damontecres.stashapp.filter.output.FilterWriter
import com.github.damontecres.stashapp.suppliers.DataSupplierFactory
import com.github.damontecres.stashapp.suppliers.FilterArgs
import com.github.damontecres.stashapp.suppliers.StashPagingSource
import com.github.damontecres.stashapp.util.StashCoroutineExceptionHandler
import kotlinx.coroutines.Job
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import org.koin.core.annotation.InjectedParam
import org.koin.core.annotation.KoinViewModel
import kotlin.reflect.cast
import kotlin.reflect.full.createInstance

/**
 * Tracks state while the user builds a new filter
 */
@KoinViewModel
class CreateFilterViewModel(
    private val application: Application,
    private val serverRepository: ServerRepository,
    private val serverLogger: ServerLogger,
    private val queryEngine: QueryEngine,
    private val mutationEngine: MutationEngine,
    val navigationManager: NavigationManager,
    private val interfaceService: InterfaceService,
    @InjectedParam private val dataType: DataType,
    @InjectedParam private val initialFilter: FilterArgs?,
) : ViewModel() {
    val currentServer get() = serverRepository.currentServer

    private val _state = MutableStateFlow(CreateFilterState(dataType, initialFilter))
    val state: StateFlow<CreateFilterState> = _state

    val storedItems = mutableMapOf<DataTypeId, NameDescription>()

    private var countJob: Job? = null

    private val currentSavedFilters = mutableMapOf<String?, String>()

    /**
     * Initialize the state
     */
    fun initialize() {
        val title =
            AnnotatedString(
                application.getString(
                    R.string.create_filter_for_type,
                    application.getString(dataType.stringId),
                ),
            )
        interfaceService.setTitle(title)
        _state.update {
            it.copy(
                title = title,
                ready = false,
            )
        }

        // Fetch all of the labels for any existing IDs in the initial object filter
        viewModelScope.launch(StashCoroutineExceptionHandler(autoToast = true)) {
            getIdsByDataType(dataType, state.value.objectFilter).entries.forEach {
                val dt = it.key
                val ids = it.value
                val items = queryEngine.getByIds(dt, ids)
                items.forEach { item ->
                    storedItems[DataTypeId(dt, item.id)] = NameDescription(item)
                }
            }
            _state.update { it.copy(ready = true) }
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
        _state.update {
            val newFilter =
                filterOption.setter(
                    dataType.filterType.cast(it.objectFilter),
                    Optional.presentIfNotNull(newItem),
                )
            it.copy(objectFilter = newFilter)
        }
    }

    /**
     * Update the result count using the current findFilter & objectFilter
     */
    fun updateCount() {
        _state.update { it.copy(resultCount = -1) }
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
                    DataSupplierFactory(serverRepository.currentServerVersion).create<Query.Data, StashData, Query.Data>(
                        FilterArgs(
                            dataType = dataType,
                            findFilter = state.value.findFilter,
                            objectFilter = state.value.objectFilter,
                        ),
                    )
                val pagingSource =
                    StashPagingSource<Query.Data, StashData, Any, Query.Data>(
                        queryEngine,
                        supplier,
                    )
                val newCount = pagingSource.getCount()
                _state.update { it.copy(resultCount = newCount) }
            }
    }

    /**
     * Get the sub-value for the current object filter
     */
    fun <ValueType : Any> getValue(filterOption: FilterOption<StashDataFilter, ValueType>): ValueType? {
        val currFilter = state.value.objectFilter
        val value = filterOption.getter(dataType.filterType.cast(currFilter))
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
        state.value.let {
            FilterArgs(
                dataType = dataType,
                name = it.filterName,
                findFilter = it.findFilter,
                objectFilter = it.objectFilter,
            ).withResolvedRandom()
        }

    suspend fun createSaveFilterInput(): SaveFilterInput {
        // Save it
        val filterWriter =
            FilterWriter(dataType) { dataType, ids ->
                queryEngine
                    .getByIds(dataType, ids)
                    .associate { it.id to extractTitle(it) }
            }
        val findFilter = state.value.findFilter
        val objectFilterMap = filterWriter.convertFilter(state.value.objectFilter)
        val existingId = getSavedFilterId(state.value.filterName)
        return SaveFilterInput(
            id = Optional.presentIfNotNull(existingId),
            mode = dataType.filterMode,
            name = state.value.filterName ?: "",
            find_filter =
                Optional.presentIfNotNull(
                    findFilter.toFindFilterType(1, 40),
                ),
            object_filter = Optional.presentIfNotNull(objectFilterMap),
            ui_options = Optional.absent(),
        )
    }

    suspend fun saveFilter() {
        val input = createSaveFilterInput()
        mutationEngine.saveFilter(input)
    }

    fun updateFilterName(filterName: String) {
        viewModelScope.launch {
            _state.update { it.copy(filterName = filterName) }
        }
    }

    fun updateFindFilter(findFilter: StashFindFilter) {
        viewModelScope.launch {
            _state.update { it.copy(findFilter = findFilter) }
            updateCount()
        }
    }

    fun updateObjectFilter(objectFilter: StashDataFilter) {
        viewModelScope.launch {
            _state.update { it.copy(objectFilter = objectFilter) }
            updateCount()
        }
    }

    companion object {
        private const val TAG = "CreateFilterViewModel"
    }
}

data class CreateFilterState(
    val filterName: String?,
    val objectFilter: StashDataFilter,
    val findFilter: StashFindFilter,
    val resultCount: Int = -1,
    val ready: Boolean = false,
    val title: AnnotatedString = AnnotatedString(""),
) {
    constructor(
        dataType: DataType,
        initialFilter: FilterArgs?,
    ) : this(
        filterName = initialFilter?.name,
        objectFilter = initialFilter?.objectFilter ?: dataType.filterType.createInstance(),
        findFilter =
            initialFilter?.findFilter
                ?: StashFindFilter(sortAndDirection = dataType.defaultSort),
    )
}
