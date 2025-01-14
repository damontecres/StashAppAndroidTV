package com.github.damontecres.stashapp.views.models

import android.util.Log
import android.view.inputmethod.InputMethodManager
import android.widget.SearchView
import androidx.leanback.widget.ObjectAdapter
import androidx.leanback.widget.PresenterSelector
import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import androidx.preference.PreferenceManager
import com.apollographql.apollo.api.Query
import com.github.damontecres.stashapp.StashApplication
import com.github.damontecres.stashapp.api.fragment.StashData
import com.github.damontecres.stashapp.data.SortAndDirection
import com.github.damontecres.stashapp.data.StashFindFilter
import com.github.damontecres.stashapp.presenters.NullPresenter
import com.github.damontecres.stashapp.presenters.NullPresenterSelector
import com.github.damontecres.stashapp.suppliers.DataSupplierFactory
import com.github.damontecres.stashapp.suppliers.FilterArgs
import com.github.damontecres.stashapp.suppliers.StashPagingSource
import com.github.damontecres.stashapp.util.PagingObjectAdapter
import com.github.damontecres.stashapp.util.QueryEngine
import com.github.damontecres.stashapp.util.StashCoroutineExceptionHandler
import com.github.damontecres.stashapp.util.StashServer
import com.github.damontecres.stashapp.util.isNotNullOrBlank
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
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

    val searchBarFocus = EqualityMutableLiveData(false)

    sealed interface LoadingStatus {
        data object NoOp : LoadingStatus

        data object Start : LoadingStatus

        data object FirstPageLoaded : LoadingStatus

        data class AdapterReady(
            val pagingAdapter: PagingObjectAdapter,
            val filterArgs: FilterArgs,
        ) : LoadingStatus
    }

    var searchJob: Job? = null
    val searchDelay =
        PreferenceManager
            .getDefaultSharedPreferences(StashApplication.getApplication())
            .getInt("searchDelay", 500)
            .toLong()

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
        _filterArgs.value = filterArgs
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
            _loadingStatus.value = LoadingStatus.AdapterReady(pagingAdapter, filterArgs)
        }
    }

    fun setupSearchButton(searchButton: SearchView) {
        if (filterArgs.isInitialized) {
            val query = filterArgs.value!!.findFilter?.q
            if (query.isNotNullOrBlank()) {
                searchButton.setQuery(query, false)
            }
        }
        searchButton.setOnQueryTextFocusChangeListener { v, hasFocus ->
            if (hasFocus) {
                // Show keyboard
                val imm: InputMethodManager =
                    v.context.getSystemService<InputMethodManager>(
                        InputMethodManager::class.java,
                    )
                imm.showSoftInput(v, 0)
            }
            searchBarFocus.value = hasFocus
        }
        searchButton.setOnQueryTextListener(
            object : SearchView.OnQueryTextListener {
                override fun onQueryTextSubmit(query: String?): Boolean = onQueryTextChange(query)

                override fun onQueryTextChange(newText: String?): Boolean {
                    searchJob?.cancel()
                    val newQuery = newText?.ifBlank { null }

                    val currentFilter = filterArgs.value!!
                    val findFilter =
                        currentFilter.findFilter
                            ?: StashFindFilter(currentFilter.dataType.defaultSort)
                    if (findFilter.q != newQuery) {
                        searchJob =
                            viewModelScope.launch(StashCoroutineExceptionHandler()) {
                                delay(searchDelay)
                                Log.v(TAG, "New query")
                                setFilter(currentFilter.copy(findFilter = findFilter.copy(q = newQuery)))
                            }
                    }

                    return true
                }
            },
        )
    }

    companion object {
        private const val TAG = "StashGridViewModel"
    }
}
