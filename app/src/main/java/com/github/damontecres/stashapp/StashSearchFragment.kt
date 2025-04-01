package com.github.damontecres.stashapp

import android.os.Bundle
import android.text.TextUtils
import android.widget.Toast
import androidx.fragment.app.activityViewModels
import androidx.leanback.app.SearchSupportFragment
import androidx.leanback.widget.ArrayObjectAdapter
import androidx.leanback.widget.HeaderItem
import androidx.leanback.widget.ListRow
import androidx.leanback.widget.ListRowPresenter
import androidx.leanback.widget.ObjectAdapter
import androidx.leanback.widget.SparseArrayObjectAdapter
import androidx.lifecycle.lifecycleScope
import androidx.preference.PreferenceManager
import com.apollographql.apollo.api.Optional
import com.github.damontecres.stashapp.MainFragment.Position
import com.github.damontecres.stashapp.api.type.FindFilterType
import com.github.damontecres.stashapp.data.DataType
import com.github.damontecres.stashapp.data.StashFindFilter
import com.github.damontecres.stashapp.data.toStashFindFilter
import com.github.damontecres.stashapp.navigation.FilterAndPosition
import com.github.damontecres.stashapp.navigation.NavigationOnItemViewClickedListener
import com.github.damontecres.stashapp.presenters.StashPresenter
import com.github.damontecres.stashapp.suppliers.FilterArgs
import com.github.damontecres.stashapp.util.QueryEngine
import com.github.damontecres.stashapp.util.StashCoroutineExceptionHandler
import com.github.damontecres.stashapp.views.models.ServerViewModel
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch

/**
 * Search for items regardless of data type
 */
class StashSearchFragment :
    SearchSupportFragment(),
    SearchSupportFragment.SearchResultProvider {
    private val serverViewModel: ServerViewModel by activityViewModels()

    private var taskJob: Job? = null

    private val rowsAdapter = SparseArrayObjectAdapter(ListRowPresenter())
    private var currentPosition: Position = Position(0, 0)
    private var currentQuery: String? = null

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setSearchResultProvider(this)
        setOnItemViewSelectedListener { itemViewHolder, item, rowViewHolder, row ->
            val rowNum = rowsAdapter.indexOf(row)
            val col =
                (rowViewHolder as ListRowPresenter.ViewHolder).gridView.selectedPosition
            val pos = Position(rowNum, col)
            currentPosition = pos
        }
        setOnItemViewClickedListener(
            NavigationOnItemViewClickedListener(serverViewModel.navigationManager) {
                val filter =
                    FilterArgs(
                        dataType = DataType.IMAGE,
                        findFilter = StashFindFilter(q = currentQuery),
                    )
                FilterAndPosition(filter, currentPosition.column)
            },
        )
    }

    override fun getResultsAdapter(): ObjectAdapter = rowsAdapter

    override fun onQueryTextChange(newQuery: String): Boolean {
        taskJob?.cancel()
        taskJob =
            viewLifecycleOwner.lifecycleScope.launch(StashCoroutineExceptionHandler()) {
                val searchDelay =
                    PreferenceManager
                        .getDefaultSharedPreferences(requireContext())
                        .getInt("searchDelay", 500)
                delay(searchDelay.toLong())
                search(newQuery)
            }
        return true
    }

    override fun onQueryTextSubmit(query: String): Boolean {
        taskJob?.cancel()
        taskJob =
            viewLifecycleOwner.lifecycleScope.launch(StashCoroutineExceptionHandler()) {
                search(query)
            }
        return true
    }

    private fun search(query: String) {
        if (!TextUtils.isEmpty(query) && query != currentQuery) {
            rowsAdapter.clear()
            currentQuery = query
            val perPage =
                PreferenceManager
                    .getDefaultSharedPreferences(requireContext())
                    .getInt("maxSearchResults", 25)
            val filter =
                FindFilterType(
                    q = Optional.present(query),
                    per_page = Optional.present(perPage),
                    page = Optional.present(1),
                )
            val queryEngine = QueryEngine(serverViewModel.requireServer())
            DataType.entries.forEach {
                val adapter =
                    ArrayObjectAdapter(StashPresenter.defaultClassPresenterSelector(serverViewModel.requireServer()))
                rowsAdapter.set(
                    it.ordinal,
                    ListRow(HeaderItem(getString(it.pluralStringId)), adapter),
                )

                viewLifecycleOwner.lifecycleScope.launch(
                    StashCoroutineExceptionHandler { ex ->
                        Toast.makeText(
                            requireContext(),
                            "Search for ${getString(it.pluralStringId)} failed: ${ex.message}",
                            Toast.LENGTH_LONG,
                        )
                    },
                ) {
                    val results = queryEngine.find(it, filter)
                    if (results.isNotEmpty()) {
                        adapter.addAll(0, results)
                        val viewAll =
                            FilterArgs(
                                dataType = it,
                                findFilter = filter.toStashFindFilter(),
                            )
                        adapter.add(viewAll)
                    } else {
                        rowsAdapter.clear(it.ordinal)
                    }
                }
            }
        }
    }

    companion object {
        const val TAG = "StashSearchFragment"
    }
}
