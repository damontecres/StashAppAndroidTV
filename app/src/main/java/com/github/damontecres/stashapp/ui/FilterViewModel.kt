package com.github.damontecres.stashapp.ui

import android.util.Log
import androidx.datastore.core.DataStore
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.apollographql.apollo.api.Query
import com.github.damontecres.stashapp.api.fragment.StashData
import com.github.damontecres.stashapp.api.type.SortDirectionEnum
import com.github.damontecres.stashapp.data.DataType
import com.github.damontecres.stashapp.di.server.QueryEngine
import com.github.damontecres.stashapp.di.server.ServerRepository
import com.github.damontecres.stashapp.di.services.NavigationManager
import com.github.damontecres.stashapp.di.services.PlayerFactory
import com.github.damontecres.stashapp.proto.StashPreferences
import com.github.damontecres.stashapp.suppliers.DataSupplierFactory
import com.github.damontecres.stashapp.suppliers.FilterArgs
import com.github.damontecres.stashapp.suppliers.StashPagingSource
import com.github.damontecres.stashapp.util.AlphabetSearchUtils
import com.github.damontecres.stashapp.util.ComposePager
import com.github.damontecres.stashapp.util.launchIO
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.withContext
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
) : ViewModel() {
    val pager = MutableLiveData<ComposePager<StashData>>()

    val currentFilter: FilterArgs? get() = pager.value?.filter
    val dataType: DataType? get() = currentFilter?.dataType

    private var job: Job? = null

    fun setFilter(
        filterArgs: FilterArgs,
        columns: Int,
    ) {
        if (pager.value?.filter != filterArgs) {
            job?.cancel()
            Log.d("FilterPageViewModel", "filterArgs=$filterArgs, columns=$columns")
            val dataSupplierFactory = DataSupplierFactory(serverRepository.currentServerVersion)
            val dataSupplier =
                dataSupplierFactory.create<Query.Data, StashData, Query.Data>(filterArgs)
            val pagingSource =
                StashPagingSource(
                    queryEngine.asUtilQueryEngine(),
                    dataSupplier,
                ) { _, _, item -> item }
            val pager =
                ComposePager(filterArgs, pagingSource, viewModelScope, pageSize = columns * 10)
            job =
                viewModelScope.launchIO {
                    pager.init()
                    withContext(Dispatchers.IO) {
                        this@FilterViewModel.pager.value = pager
                    }
                }
        }
    }

    suspend fun findLetterPosition(letter: Char): Int {
        val filter = this.pager.value!!.filter

        val dataSupplierFactory = DataSupplierFactory(serverRepository.currentServerVersion)
        val letterPosition =
            AlphabetSearchUtils.findPosition(
                letter,
                filter,
                queryEngine.asUtilQueryEngine(),
                dataSupplierFactory,
            )
        val jumpPosition =
            if (filter.sortAndDirection.direction == SortDirectionEnum.DESC) {
                // Reverse if sorting descending
                pager.value!!.size - letterPosition - 1
            } else {
                letterPosition
            }
        return jumpPosition
    }
}
