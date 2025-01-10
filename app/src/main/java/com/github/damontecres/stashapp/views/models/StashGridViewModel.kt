package com.github.damontecres.stashapp.views.models

import android.util.Log
import androidx.leanback.widget.ObjectAdapter
import androidx.leanback.widget.PresenterSelector
import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.apollographql.apollo.api.Query
import com.github.damontecres.stashapp.api.fragment.StashData
import com.github.damontecres.stashapp.data.SortAndDirection
import com.github.damontecres.stashapp.presenters.NullPresenter
import com.github.damontecres.stashapp.presenters.NullPresenterSelector
import com.github.damontecres.stashapp.suppliers.DataSupplierFactory
import com.github.damontecres.stashapp.suppliers.FilterArgs
import com.github.damontecres.stashapp.suppliers.StashPagingSource
import com.github.damontecres.stashapp.util.PagingObjectAdapter
import com.github.damontecres.stashapp.util.QueryEngine
import com.github.damontecres.stashapp.util.StashCoroutineExceptionHandler
import com.github.damontecres.stashapp.util.StashServer
import kotlinx.coroutines.launch

class StashGridViewModel : ViewModel() {
    private val _filterArgs = MutableLiveData<FilterArgs>()
    val filterArgs: LiveData<FilterArgs> = _filterArgs

    private val _loadingStatus = MutableLiveData<LoadingStatus>()
    val loadingStatus: LiveData<LoadingStatus> = _loadingStatus

    private val _currentPosition = EqualityMutableLiveData<Int>(0)
    val currentPosition: LiveData<Int> = _currentPosition
    var position: Int
        get() = currentPosition.value!!
        set(value) {
            _currentPosition.value = value
        }

    sealed interface LoadingStatus {
        data object NoOp : LoadingStatus

        data object Start : LoadingStatus

        data object FirstPageLoaded : LoadingStatus

        data class AdapterReady(
            val pagingAdapter: PagingObjectAdapter,
        ) : LoadingStatus
    }

    private var numberOfColumns = -1
    private lateinit var presenterSelector: PresenterSelector
    private lateinit var pagingAdapter: PagingObjectAdapter

    fun init(presenterSelector: PresenterSelector) {
        this.presenterSelector = presenterSelector
    }

    fun setFilter(sortAndDirection: SortAndDirection) {
        setFilter(filterArgs.value!!.with(sortAndDirection))
    }

    fun setFilter(filterArgs: FilterArgs) {
        if (_filterArgs.value == filterArgs) {
            Log.v(TAG, "Filter not changed, no-op")
            _loadingStatus.value = LoadingStatus.NoOp
            return
        }
        _loadingStatus.value = LoadingStatus.Start

        val dataType = filterArgs.dataType

        val server = StashServer.requireCurrentServer()
        val factory = DataSupplierFactory(server.serverPreferences.serverVersion)
        val dataSupplier =
            factory.create<Query.Data, StashData, Query.Data>(filterArgs)
        val pagingSource =
            StashPagingSource<Query.Data, StashData, StashData, Query.Data>(
                QueryEngine(server),
                dataSupplier = dataSupplier,
            )
        val pagingAdapter =
            PagingObjectAdapter(
                pagingSource,
                // TODO pageSize = numberOfColumns * 10,
                100,
                viewModelScope,
                NullPresenterSelector(presenterSelector, NullPresenter(dataType)),
            )
        pagingAdapter.registerObserver(
            object : ObjectAdapter.DataObserver() {
                override fun onChanged() {
                    _loadingStatus.value = LoadingStatus.FirstPageLoaded
                    pagingAdapter.unregisterObserver(this)
                }
            },
        )

        viewModelScope.launch(StashCoroutineExceptionHandler(true)) {
            pagingAdapter.init()
            this@StashGridViewModel.pagingAdapter = pagingAdapter
            _filterArgs.value = filterArgs
            _loadingStatus.value = LoadingStatus.AdapterReady(pagingAdapter)
        }
    }

    companion object {
        private const val TAG = "StashGridViewModel"
    }
}
