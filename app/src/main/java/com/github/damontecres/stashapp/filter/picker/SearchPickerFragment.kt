package com.github.damontecres.stashapp.filter.picker

import android.os.Bundle
import android.text.TextUtils
import android.util.Log
import android.widget.Toast
import androidx.fragment.app.activityViewModels
import androidx.leanback.app.SearchSupportFragment
import androidx.leanback.widget.ArrayObjectAdapter
import androidx.leanback.widget.HeaderItem
import androidx.leanback.widget.ListRow
import androidx.leanback.widget.ListRowPresenter
import androidx.leanback.widget.ObjectAdapter
import androidx.leanback.widget.Presenter
import androidx.leanback.widget.Row
import androidx.leanback.widget.RowPresenter
import androidx.leanback.widget.SparseArrayObjectAdapter
import androidx.lifecycle.lifecycleScope
import androidx.preference.PreferenceManager
import com.apollographql.apollo.api.Optional
import com.github.damontecres.stashapp.R
import com.github.damontecres.stashapp.StashApplication
import com.github.damontecres.stashapp.api.fragment.StashData
import com.github.damontecres.stashapp.api.type.CriterionModifier
import com.github.damontecres.stashapp.api.type.FindFilterType
import com.github.damontecres.stashapp.api.type.GalleryFilterType
import com.github.damontecres.stashapp.api.type.SortDirectionEnum
import com.github.damontecres.stashapp.api.type.StringCriterionInput
import com.github.damontecres.stashapp.data.DataType
import com.github.damontecres.stashapp.data.SortOption
import com.github.damontecres.stashapp.data.room.RecentSearchItem
import com.github.damontecres.stashapp.presenters.StashPresenter
import com.github.damontecres.stashapp.util.QueryEngine
import com.github.damontecres.stashapp.util.StashCoroutineExceptionHandler
import com.github.damontecres.stashapp.views.models.ServerViewModel
import kotlinx.coroutines.CoroutineExceptionHandler
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import kotlin.coroutines.CoroutineContext
import kotlin.properties.Delegates

/**
 * Similar to [com.github.damontecres.stashapp.SearchForFragment], search for an item of a specific [DataType]
 */
class SearchPickerFragment(
    private val dataType: DataType,
    private val addItem: (StashData) -> Unit,
) : SearchSupportFragment(),
    SearchSupportFragment.SearchResultProvider {
    private val serverViewModel: ServerViewModel by activityViewModels()
    private var taskJob: Job? = null

    private val adapter = SparseArrayObjectAdapter(ListRowPresenter())
    private val searchResultsAdapter = ArrayObjectAdapter()
    private var perPage by Delegates.notNull<Int>()

    private val exceptionHandler =
        CoroutineExceptionHandler { _: CoroutineContext, ex: Throwable ->
            Log.e(TAG, "Exception in search", ex)
            Toast
                .makeText(requireContext(), "Search failed: ${ex.message}", Toast.LENGTH_LONG)
                .show()
        }

    private lateinit var queryEngine: QueryEngine

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        queryEngine = QueryEngine(serverViewModel.requireServer())
        perPage =
            PreferenceManager
                .getDefaultSharedPreferences(requireContext())
                .getInt("maxSearchResults", 25)
        title = getString(dataType.pluralStringId)
        searchResultsAdapter.presenterSelector = StashPresenter.defaultClassPresenterSelector()
        adapter.set(
            RESULTS_POS,
            ListRow(HeaderItem(getString(R.string.waiting_for_query)), ArrayObjectAdapter()),
        )

        setSearchResultProvider(this)
        setOnItemViewClickedListener {
            itemViewHolder: Presenter.ViewHolder,
            item: Any,
            rowViewHolder: RowPresenter.ViewHolder?,
            row: Row?,
            ->
            if (item is StashData) {
                returnId(item)
            } else {
                throw IllegalStateException("Unknown item: $item")
            }
        }
    }

    private fun returnId(item: StashData) {
        val currentServer = serverViewModel.requireServer()
        if (dataType in DATA_TYPE_SUGGESTIONS) {
            viewLifecycleOwner.lifecycleScope.launch(Dispatchers.IO + StashCoroutineExceptionHandler()) {
                StashApplication
                    .getDatabase()
                    .recentSearchItemsDao()
                    .insert(RecentSearchItem(currentServer.url, item.id, dataType))
            }
        }
        addItem(item)
        parentFragmentManager.popBackStackImmediate()
    }

    override fun onResume() {
        super.onResume()
        if (dataType in DATA_TYPE_SUGGESTIONS) {
            viewLifecycleOwner.lifecycleScope.launch(
                StashCoroutineExceptionHandler {
                    Toast.makeText(
                        requireContext(),
                        "Error loading suggestions: ${it.message}",
                        Toast.LENGTH_LONG,
                    )
                },
            ) {
                val resultsAdapter =
                    ArrayObjectAdapter(StashPresenter.defaultClassPresenterSelector())
                val sortBy =
                    when (dataType) {
                        DataType.GALLERY -> SortOption.ImagesCount
                        else -> SortOption.ScenesCount
                    }
                val filter =
                    FindFilterType(
                        direction = Optional.present(SortDirectionEnum.DESC),
                        per_page = Optional.present(perPage),
                        sort = Optional.present(sortBy.key),
                    )
                val results =
                    when (dataType) {
                        DataType.GALLERY ->
                            // Cannot add an image to a zip/folder gallery, so exclude them
                            queryEngine.findGalleries(
                                filter,
                                GalleryFilterType(
                                    path =
                                        Optional.present(
                                            StringCriterionInput(
                                                value = "",
                                                modifier = CriterionModifier.IS_NULL,
                                            ),
                                        ),
                                ),
                            )

                        else -> queryEngine.find(dataType, filter)
                    }
                resultsAdapter.addAll(0, results)
                adapter.set(
                    SUGGESTIONS_POS,
                    ListRow(HeaderItem(getString(R.string.suggestions)), resultsAdapter),
                )
            }
            viewLifecycleOwner.lifecycleScope.launch(Dispatchers.IO + StashCoroutineExceptionHandler()) {
                val currentServer = serverViewModel.requireServer()
                val mostRecentIds =
                    StashApplication
                        .getDatabase()
                        .recentSearchItemsDao()
                        .getMostRecent(perPage, currentServer.url, dataType)
                        .map { it.id }
                Log.v(TAG, "Got ${mostRecentIds.size} recent items")
                if (mostRecentIds.isNotEmpty()) {
                    val items = queryEngine.getByIds(dataType, mostRecentIds)
                    val results =
                        ArrayObjectAdapter(StashPresenter.defaultClassPresenterSelector())
                    if (items.isNotEmpty()) {
                        Log.v(
                            TAG,
                            "${mostRecentIds.size} recent items resolved to ${results.size()} items",
                        )
                        results.addAll(0, items)
                        withContext(Dispatchers.Main) {
                            val headerName =
                                getString(
                                    R.string.format_recently_used,
                                    getString(dataType.pluralStringId).lowercase(),
                                )
                            adapter.set(RECENT_POS, ListRow(HeaderItem(headerName), results))
                        }
                    }
                }
            }
        }
    }

    override fun getResultsAdapter(): ObjectAdapter = adapter

    override fun onQueryTextChange(newQuery: String): Boolean {
        taskJob?.cancel()
        taskJob =
            viewLifecycleOwner.lifecycleScope.launch(exceptionHandler) {
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
            viewLifecycleOwner.lifecycleScope.launch(exceptionHandler) {
                search(query)
            }
        return true
    }

    private fun search(query: String) {
        searchResultsAdapter.clear()
        if (!TextUtils.isEmpty(query)) {
            adapter.set(
                RESULTS_POS,
                ListRow(
                    HeaderItem(getString(R.string.stashapp_loading_generic)),
                    ArrayObjectAdapter(),
                ),
            )
            val filter =
                FindFilterType(
                    q = Optional.present(query),
                    per_page = Optional.present(perPage),
                )
            viewLifecycleOwner.lifecycleScope.launch(exceptionHandler) {
                val results =
                    when (dataType) {
                        DataType.GALLERY ->
                            // Cannot add an image to a zip gallery, so exclude them
                            queryEngine.findGalleries(
                                filter,
                                GalleryFilterType(
                                    path =
                                        Optional.present(
                                            StringCriterionInput(
                                                value = "",
                                                modifier = CriterionModifier.IS_NULL,
                                            ),
                                        ),
                                ),
                            )

                        else -> queryEngine.find(dataType, filter)
                    }
                if (results.isNotEmpty()) {
                    searchResultsAdapter.addAll(0, results)
                    adapter.set(
                        RESULTS_POS,
                        ListRow(HeaderItem(getString(R.string.results)), searchResultsAdapter),
                    )
                } else {
                    adapter.set(
                        RESULTS_POS,
                        ListRow(
                            HeaderItem(getString(R.string.stashapp_component_tagger_results_match_failed_no_result)),
                            ArrayObjectAdapter(),
                        ),
                    )
                }
            }
        } else {
            adapter.set(
                RESULTS_POS,
                ListRow(HeaderItem(getString(R.string.waiting_for_query)), ArrayObjectAdapter()),
            )
        }
    }

    companion object {
        const val TAG = "SearchForFragment"

        const val TITLE_KEY = "title"

        private const val RESULTS_POS = 0
        private const val SUGGESTIONS_POS = RESULTS_POS + 1
        private const val RECENT_POS = SUGGESTIONS_POS + 1

        // List of data types that support querying for suggestions
        val DATA_TYPE_SUGGESTIONS =
            setOf(
                DataType.TAG,
                DataType.PERFORMER,
                DataType.STUDIO,
                DataType.GALLERY,
                DataType.GROUP,
            )
    }
}
