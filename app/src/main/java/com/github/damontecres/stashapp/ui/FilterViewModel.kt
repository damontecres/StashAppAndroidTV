package com.github.damontecres.stashapp.ui

import android.util.Log
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.apollographql.apollo.api.Query
import com.github.damontecres.stashapp.api.fragment.StashData
import com.github.damontecres.stashapp.api.type.SortDirectionEnum
import com.github.damontecres.stashapp.data.DataType
import com.github.damontecres.stashapp.suppliers.DataSupplierFactory
import com.github.damontecres.stashapp.suppliers.FilterArgs
import com.github.damontecres.stashapp.suppliers.StashPagingSource
import com.github.damontecres.stashapp.util.AlphabetSearchUtils
import com.github.damontecres.stashapp.util.ComposePager
import com.github.damontecres.stashapp.util.QueryEngine
import com.github.damontecres.stashapp.util.StashCoroutineExceptionHandler
import com.github.damontecres.stashapp.util.StashServer
import kotlinx.coroutines.Job
import kotlinx.coroutines.launch

class FilterViewModel : ViewModel() {
    private var server: StashServer? = null
    val pager = MutableLiveData<ComposePager<StashData>>()

    val currentFilter: FilterArgs? get() = pager.value?.filter
    val dataType: DataType? get() = currentFilter?.dataType

    private var job: Job? = null

    fun setFilter(
        server: StashServer,
        filterArgs: FilterArgs,
    ) {
        if (pager.value?.filter != filterArgs || server != this.server) {
            job?.cancel()
            Log.d("FilterPageViewModel", "filterArgs=$filterArgs")
            this.server = server
            val dataSupplierFactory = DataSupplierFactory(server.version)
            val dataSupplier =
                dataSupplierFactory.create<Query.Data, StashData, Query.Data>(filterArgs)
            val pagingSource =
                StashPagingSource(QueryEngine(server), dataSupplier) { _, _, item -> item }
            val pager = ComposePager(filterArgs, pagingSource, viewModelScope)
            job =
                viewModelScope.launch(StashCoroutineExceptionHandler(autoToast = true)) {
                    pager.init()
                    this@FilterViewModel.pager.value = pager
                }
        }
    }

    suspend fun findLetterPosition(letter: Char): Int {
        val server = this.server!!
        val filter = this.pager.value!!.filter

        val dataSupplierFactory = DataSupplierFactory(server.version)
        val letterPosition =
            AlphabetSearchUtils.findPosition(
                letter,
                filter,
                QueryEngine(server),
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
