package com.github.damontecres.stashapp.views.models

import android.util.Log
import android.view.inputmethod.EditorInfo
import android.view.inputmethod.InputMethodManager
import androidx.core.content.ContextCompat.getSystemService
import androidx.core.widget.doOnTextChanged
import androidx.leanback.widget.ObjectAdapter
import androidx.leanback.widget.PresenterSelector
import androidx.leanback.widget.SearchEditText
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
import com.github.damontecres.stashapp.util.toReadableString
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch

class StashGridViewModel : ViewModel() {
    private lateinit var server: StashServer
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

    val scrollToNextPage = MutableLiveData<Boolean?>()

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

    private var pageSize: Int = 25
    private lateinit var presenterSelector: PresenterSelector
    private lateinit var pagingAdapter: PagingObjectAdapter

    fun init(
        server: StashServer,
        presenterSelector: PresenterSelector,
        pageSize: Int,
    ) {
        this.server = server
        this.presenterSelector = presenterSelector
        this.pageSize = pageSize
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
        Log.d(TAG, "Setting new filter, pageSize=$pageSize")
        Log.v(
            TAG,
            "filterArgs: dataType=${filterArgs.dataType}\n" +
                "findFilter=${filterArgs.findFilter}\n" +
                "objectFilter=${filterArgs.objectFilter?.toReadableString(true)}",
        )
        _filterArgs.value = filterArgs
        _loadingStatus.value = LoadingStatus.Start

        val dataType = filterArgs.dataType

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
                pageSize,
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

    fun setupSearch(searchEditText: SearchEditText) {
        if (filterArgs.isInitialized) {
            val query = filterArgs.value!!.findFilter?.q
            if (query.isNotNullOrBlank()) {
                searchEditText.setText(query)
            }
        }
        searchEditText.setOnFocusChangeListener { v, hasFocus ->
            searchBarFocus.value = hasFocus
        }
        searchEditText.setOnEditorActionListener { v, actionId, event ->
            if (actionId == EditorInfo.IME_ACTION_SEARCH ||
                actionId == EditorInfo.IME_ACTION_DONE ||
                actionId == EditorInfo.IME_ACTION_GO
            ) {
                val imm = getSystemService(v.context, InputMethodManager::class.java)
                imm!!.hideSoftInputFromWindow(v.windowToken, 0)
                true
            } else {
                false
            }
        }
        searchEditText.doOnTextChanged { text, start, before, count ->
            val newQuery = searchEditText.text.toString().ifBlank { null }
            searchJob?.cancel()

            val currentFilter = filterArgs.value
            if (currentFilter != null) {
                val findFilter =
                    currentFilter.findFilter
                        ?: StashFindFilter(currentFilter.dataType.defaultSort)
                val oldQuery = findFilter.q?.ifBlank { null }
                if (oldQuery != newQuery) {
                    searchJob =
                        viewModelScope.launch(StashCoroutineExceptionHandler()) {
                            delay(searchDelay)
                            Log.v(TAG, "New query")
                            setFilter(currentFilter.copy(findFilter = findFilter.copy(q = newQuery)))
                        }
                }
            }
        }
    }

    fun clearCache() {
        val status = loadingStatus.value
        if (status is LoadingStatus.AdapterReady) {
            status.pagingAdapter.clearCache()
        }
    }

    companion object {
        private const val TAG = "StashGridViewModel"
    }
}
