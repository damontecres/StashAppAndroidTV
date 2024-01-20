package com.github.damontecres.stashapp

import android.app.Activity
import android.content.Intent
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
import androidx.leanback.widget.Presenter
import androidx.leanback.widget.Row
import androidx.leanback.widget.RowPresenter
import androidx.lifecycle.lifecycleScope
import androidx.preference.PreferenceManager
import com.apollographql.apollo3.api.Optional
import com.github.damontecres.stashapp.api.fragment.MovieData
import com.github.damontecres.stashapp.api.fragment.PerformerData
import com.github.damontecres.stashapp.api.fragment.SlimSceneData
import com.github.damontecres.stashapp.api.fragment.StudioData
import com.github.damontecres.stashapp.api.type.FindFilterType
import com.github.damontecres.stashapp.data.DataType
import com.github.damontecres.stashapp.data.Tag
import com.github.damontecres.stashapp.presenters.StashPresenter
import kotlinx.coroutines.CoroutineExceptionHandler
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import kotlin.coroutines.CoroutineContext

class SearchForFragment(
    private val dataType: DataType,
) :
    SearchSupportFragment(),
        SearchSupportFragment.SearchResultProvider {
    private var taskJob: Job? = null

    private val adapter = ArrayObjectAdapter(ListRowPresenter())
    private val resultsAdapter = ArrayObjectAdapter(StashPresenter.SELECTOR)

    private val exceptionHandler =
        CoroutineExceptionHandler { _: CoroutineContext, ex: Throwable ->
            Log.e(TAG, "Exception in search", ex)
            Toast.makeText(requireContext(), "Search failed: ${ex.message}", Toast.LENGTH_LONG).show()
        }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setSearchResultProvider(this)
        setOnItemViewClickedListener {
                itemViewHolder: Presenter.ViewHolder,
                item: Any,
                rowViewHolder: RowPresenter.ViewHolder?,
                row: Row?,
            ->
            val result = Intent()
            val resultId =
                when (dataType) {
                    DataType.TAG -> (item as Tag).id.toString()
                    DataType.SCENE -> (item as SlimSceneData).id
                    DataType.MOVIE -> (item as MovieData).id
                    DataType.STUDIO -> (item as StudioData).id
                    DataType.PERFORMER -> (item as PerformerData).id
                }
            result.putExtra(RESULT_ID_KEY, resultId)
            result.putExtra(ID_KEY, requireActivity().intent.getLongExtra("id", -1))
            requireActivity().setResult(Activity.RESULT_OK, result)
            requireActivity().finish()
        }
        adapter.add(ListRow(HeaderItem("Results"), resultsAdapter))
    }

    override fun getResultsAdapter(): ObjectAdapter {
        return adapter
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
        resultsAdapter.clear()

        if (!TextUtils.isEmpty(query)) {
            val perPage =
                PreferenceManager.getDefaultSharedPreferences(requireContext())
                    .getInt("maxSearchResults", 25)
            val filter =
                FindFilterType(
                    q = Optional.present(query),
                    per_page = Optional.present(perPage),
                )
            val queryEngine = QueryEngine(requireContext(), true)
            viewLifecycleOwner.lifecycleScope.launch(exceptionHandler) {
                resultsAdapter.addAll(0, queryEngine.find(dataType, filter))
            }
        }
    }

    companion object {
        const val TAG = "SearchForFragment"

        const val ID_KEY = "id"
        const val RESULT_ID_KEY = "resultId"
    }
}
