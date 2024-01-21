package com.github.damontecres.stashapp

import android.os.Bundle
import android.text.TextUtils
import android.util.Log
import android.widget.Toast
import androidx.leanback.app.SearchSupportFragment
import androidx.leanback.widget.ArrayObjectAdapter
import androidx.leanback.widget.HeaderItem
import androidx.leanback.widget.ListRow
import androidx.leanback.widget.ListRowPresenter
import androidx.leanback.widget.ObjectAdapter
import androidx.lifecycle.lifecycleScope
import androidx.preference.PreferenceManager
import com.apollographql.apollo3.api.Optional
import com.github.damontecres.stashapp.api.type.FindFilterType
import com.github.damontecres.stashapp.data.DataType
import com.github.damontecres.stashapp.presenters.StashPresenter
import com.github.damontecres.stashapp.util.QueryEngine
import kotlinx.coroutines.CoroutineExceptionHandler
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import java.util.EnumMap

class StashSearchFragment : SearchSupportFragment(), SearchSupportFragment.SearchResultProvider {
    private var taskJob: Job? = null

    private val rowsAdapter = ArrayObjectAdapter(ListRowPresenter())
    private val adapters = EnumMap<DataType, ArrayObjectAdapter>(DataType::class.java)

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setSearchResultProvider(this)
        setOnItemViewClickedListener(StashItemViewClickListener(requireActivity()))

        DataType.entries.forEach {
            val adapter = ArrayObjectAdapter(StashPresenter.SELECTOR)
            adapters[it] = adapter
            rowsAdapter.add(ListRow(HeaderItem(getString(it.pluralStringId)), adapter))
        }
    }

    override fun getResultsAdapter(): ObjectAdapter {
        return rowsAdapter
    }

    override fun onQueryTextChange(newQuery: String): Boolean {
        taskJob?.cancel()
        taskJob =
            viewLifecycleOwner.lifecycleScope.launch {
                val searchDelay =
                    PreferenceManager.getDefaultSharedPreferences(requireContext())
                        .getInt("searchDelay", 500)
                delay(searchDelay.toLong())
                search(newQuery)
            }
        return true
    }

    override fun onQueryTextSubmit(query: String): Boolean {
        taskJob?.cancel()
        taskJob =
            viewLifecycleOwner.lifecycleScope.launch {
                search(query)
            }
        return true
    }

    private suspend fun search(query: String) {
        if (!TextUtils.isEmpty(query)) {
            adapters.values.forEach { it.clear() }
            val perPage =
                PreferenceManager.getDefaultSharedPreferences(requireContext())
                    .getInt("maxSearchResults", 25)
            val filter =
                FindFilterType(
                    q = Optional.present(query),
                    per_page = Optional.present(perPage),
                )
            val queryEngine = QueryEngine(requireContext(), true)
            DataType.entries.forEach {
                viewLifecycleOwner.lifecycleScope.launch(
                    CoroutineExceptionHandler { _, ex ->
                        Log.e(TAG, "Exception in search for $it", ex)
                        Toast.makeText(
                            requireContext(),
                            "Search for ${getString(it.pluralStringId)} failed: ${ex.message}",
                            Toast.LENGTH_LONG,
                        ).show()
                    },
                ) {
                    adapters[it]!!.addAll(0, queryEngine.find(it, filter))
                }
            }
        }
    }

    companion object {
        const val TAG = "StashSearchFragment"
    }
}
