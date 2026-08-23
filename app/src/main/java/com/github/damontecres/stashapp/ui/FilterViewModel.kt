package com.github.damontecres.stashapp.ui

import androidx.datastore.core.DataStore
import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.ViewModel
import androidx.lifecycle.serialization.saved
import androidx.lifecycle.viewModelScope
import androidx.savedstate.serialization.SavedStateConfiguration
import co.touchlab.kermit.Logger
import com.apollographql.apollo.api.Query
import com.github.damontecres.stashapp.api.fragment.StashData
import com.github.damontecres.stashapp.api.type.SortDirectionEnum
import com.github.damontecres.stashapp.data.DataType
import com.github.damontecres.stashapp.di.server.QueryEngine
import com.github.damontecres.stashapp.di.server.ServerRepository
import com.github.damontecres.stashapp.di.services.InterfaceService
import com.github.damontecres.stashapp.di.services.NavigationManager
import com.github.damontecres.stashapp.di.services.PlayerFactory
import com.github.damontecres.stashapp.proto.StashPreferences
import com.github.damontecres.stashapp.suppliers.DataSupplierFactory
import com.github.damontecres.stashapp.suppliers.FilterArgs
import com.github.damontecres.stashapp.suppliers.StashPagingSource
import com.github.damontecres.stashapp.ui.util.DataLoadingState
import com.github.damontecres.stashapp.util.AlphabetSearchUtils
import com.github.damontecres.stashapp.util.ComposePager
import com.github.damontecres.stashapp.util.OptionalSerializersModule
import com.github.damontecres.stashapp.util.launchIO
import kotlinx.coroutines.Job
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.update
import org.koin.core.annotation.InjectedParam
import org.koin.core.annotation.KoinViewModel

@KoinViewModel
class FilterViewModel(
    private val serverRepository: ServerRepository,
    private val queryEngine: QueryEngine,
    val navigationManager: NavigationManager,
    // TODO remove this
    val preferences: DataStore<StashPreferences>,
    // TODO remove this
    val playerFactory: PlayerFactory,
    private val interfaceService: InterfaceService,
    private val savedStateHandle: SavedStateHandle,
    @InjectedParam initialFilter: FilterArgs,
) : ViewModel() {
    private val _state = MutableStateFlow(FilterPageState())
    val state: StateFlow<FilterPageState> = _state

    val interfaceState get() = interfaceService.state

    //    val pager = MutableLiveData<ComposePager<StashData>>()
    private val config =
        SavedStateConfiguration {
            serializersModule = OptionalSerializersModule
        }
    var filter by savedStateHandle.saved(configuration = config) {
        initialFilter
    }

    val dataType: DataType get() = filter.dataType

    fun init() {
        viewModelScope.launchIO {
            updateFilter(filter)
        }
    }

    private var job: Job? = null

    fun updateFilter(filterArgs: FilterArgs) {
        job?.cancel()
        _state.update { it.copy(pager = DataLoadingState.Loading) }
        job =
            viewModelScope.launchIO {
                interfaceService.setTitle(filterArgs.name, filterArgs.dataType.pluralStringId)
                try {
                    Logger.d { "setFilter: filterArgs=$filterArgs" }
                    val dataSupplierFactory =
                        DataSupplierFactory(serverRepository.currentServerVersion)
                    val dataSupplier =
                        dataSupplierFactory.create<Query.Data, StashData, Query.Data>(filterArgs)
                    val pagingSource =
                        StashPagingSource(
                            queryEngine,
                            dataSupplier,
                        ) { _, _, item -> item }
                    val pager =
                        ComposePager(filterArgs, pagingSource, viewModelScope, pageSize = 100)

                    pager.init()
                    filter = filterArgs
                    _state.update { it.copy(pager = DataLoadingState.Success(pager)) }
                } catch (ex: Exception) {
                    Logger.e(ex) { "Error fetching for $filterArgs" }
                }
            }
    }

    suspend fun findLetterPosition(letter: Char): Int {
        val dataSupplierFactory = DataSupplierFactory(serverRepository.currentServerVersion)
        val letterPosition =
            AlphabetSearchUtils.findPosition(
                letter,
                filter,
                queryEngine,
                dataSupplierFactory,
            )
        val jumpPosition =
            if (filter.sortAndDirection.direction == SortDirectionEnum.DESC) {
                // Reverse if sorting descending
                state.value.pager.successValue?.size?.let {
                    it - letterPosition - 1
                } ?: 0
            } else {
                letterPosition
            }
        return jumpPosition
    }
}

data class FilterPageState(
    val pager: DataLoadingState<ComposePager<StashData>> = DataLoadingState.Pending,
)
