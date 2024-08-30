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
import androidx.leanback.widget.SparseArrayObjectAdapter
import androidx.lifecycle.lifecycleScope
import androidx.preference.PreferenceManager
import com.apollographql.apollo3.api.Optional
import com.github.damontecres.stashapp.actions.StashAction
import com.github.damontecres.stashapp.api.fragment.GalleryData
import com.github.damontecres.stashapp.api.fragment.ImageData
import com.github.damontecres.stashapp.api.fragment.MarkerData
import com.github.damontecres.stashapp.api.fragment.MovieData
import com.github.damontecres.stashapp.api.fragment.PerformerData
import com.github.damontecres.stashapp.api.fragment.SlimSceneData
import com.github.damontecres.stashapp.api.fragment.StudioData
import com.github.damontecres.stashapp.api.fragment.TagData
import com.github.damontecres.stashapp.api.type.CriterionModifier
import com.github.damontecres.stashapp.api.type.FindFilterType
import com.github.damontecres.stashapp.api.type.GalleryFilterType
import com.github.damontecres.stashapp.api.type.PerformerCreateInput
import com.github.damontecres.stashapp.api.type.SortDirectionEnum
import com.github.damontecres.stashapp.api.type.StringCriterionInput
import com.github.damontecres.stashapp.api.type.TagCreateInput
import com.github.damontecres.stashapp.data.DataType
import com.github.damontecres.stashapp.data.room.RecentSearchItem
import com.github.damontecres.stashapp.presenters.ActionPresenter.Companion.CARD_HEIGHT
import com.github.damontecres.stashapp.presenters.ActionPresenter.Companion.CARD_WIDTH
import com.github.damontecres.stashapp.presenters.StashImageCardView
import com.github.damontecres.stashapp.presenters.StashPresenter
import com.github.damontecres.stashapp.util.MutationEngine
import com.github.damontecres.stashapp.util.QueryEngine
import com.github.damontecres.stashapp.util.SingleItemObjectAdapter
import com.github.damontecres.stashapp.util.StashCoroutineExceptionHandler
import com.github.damontecres.stashapp.util.StashServer
import com.github.damontecres.stashapp.util.isNotNullOrBlank
import kotlinx.coroutines.CoroutineExceptionHandler
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import kotlin.coroutines.CoroutineContext
import kotlin.properties.Delegates

class SearchForFragment : SearchSupportFragment(), SearchSupportFragment.SearchResultProvider {
    private var taskJob: Job? = null
    private var query: String? = null

    private val adapter = SparseArrayObjectAdapter(ListRowPresenter())
    private val searchResultsAdapter = ArrayObjectAdapter(StashPresenter.SELECTOR)
    private var perPage by Delegates.notNull<Int>()
    private var createNewAdapter =
        SingleItemObjectAdapter(CreateNewPresenter(), StashAction.CREATE_NEW)

    private val exceptionHandler =
        CoroutineExceptionHandler { _: CoroutineContext, ex: Throwable ->
            Log.e(TAG, "Exception in search", ex)
            Toast.makeText(requireContext(), "Search failed: ${ex.message}", Toast.LENGTH_LONG)
                .show()
        }

    private lateinit var dataType: DataType

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        dataType = DataType.valueOf(requireActivity().intent.getStringExtra("dataType")!!)
        perPage =
            PreferenceManager.getDefaultSharedPreferences(requireContext())
                .getInt("maxSearchResults", 25)
        title =
            requireActivity().intent.getStringExtra(TITLE_KEY) ?: getString(dataType.pluralStringId)
        searchResultsAdapter.presenterSelector = StashPresenter.SELECTOR
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
            if (item == StashAction.CREATE_NEW) {
                viewLifecycleOwner.lifecycleScope.launch(StashCoroutineExceptionHandler()) {
                    handleCreate(query)
                }
            } else {
                returnId(item)
            }
        }
    }

    private suspend fun handleCreate(query: String?) {
        if (query.isNotNullOrBlank()) {
            val name = query.replaceFirstChar(Char::titlecase)
            val mutationEngine = MutationEngine(requireContext())
            val item =
                when (dataType) {
                    DataType.TAG -> {
                        mutationEngine.createTag(TagCreateInput(name = name))
                    }

                    DataType.PERFORMER -> {
                        mutationEngine.createPerformer(PerformerCreateInput(name = name))
                    }

                    else -> throw IllegalArgumentException("Unsupported datatype $dataType")
                }
            if (item != null) {
                Toast.makeText(
                    requireContext(),
                    "Created new ${getString(dataType.stringId)}: $name",
                    Toast.LENGTH_LONG,
                ).show()
                returnId(item)
            }
        }
    }

    private fun returnId(item: Any?) {
        if (item != null) {
            val result = Intent()
            val resultId =
                when (dataType) {
                    DataType.TAG -> (item as TagData).id
                    DataType.SCENE -> (item as SlimSceneData).id
                    DataType.MOVIE -> (item as MovieData).id
                    DataType.STUDIO -> (item as StudioData).id
                    DataType.PERFORMER -> (item as PerformerData).id
                    DataType.MARKER -> (item as MarkerData).id
                    DataType.IMAGE -> (item as ImageData).id
                    DataType.GALLERY -> (item as GalleryData).id
                }
            val currentServer = StashServer.getCurrentStashServer(requireContext())
            if (dataType in DATA_TYPE_SUGGESTIONS && currentServer != null) {
                viewLifecycleOwner.lifecycleScope.launch(Dispatchers.IO + StashCoroutineExceptionHandler()) {
                    StashApplication.getDatabase().recentSearchItemsDao()
                        .insert(RecentSearchItem(currentServer.url, resultId, dataType))
                }
            }
            result.putExtra(RESULT_ID_KEY, resultId)
            result.putExtra(ID_KEY, requireActivity().intent.getLongExtra("id", -1))
            requireActivity().setResult(Activity.RESULT_OK, result)
            requireActivity().finish()
        }
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
                val resultsAdapter = ArrayObjectAdapter(StashPresenter.SELECTOR)
                val queryEngine = QueryEngine(requireContext(), false)
                val sortBy =
                    when (dataType) {
                        DataType.GALLERY -> "images_count"
                        else -> "scenes_count"
                    }
                val filter =
                    FindFilterType(
                        direction = Optional.present(SortDirectionEnum.DESC),
                        per_page = Optional.present(perPage),
                        sort = Optional.present(sortBy),
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
                val currentServer = StashServer.getCurrentStashServer(requireContext())
                val queryEngine = QueryEngine(requireContext(), false)
                if (currentServer != null) {
                    val mostRecentIds =
                        StashApplication.getDatabase().recentSearchItemsDao()
                            .getMostRecent(perPage, currentServer.url, dataType).map { it.id }
                    Log.v(TAG, "Got ${mostRecentIds.size} recent items")
                    if (mostRecentIds.isNotEmpty()) {
                        val items =
                            when (dataType) {
                                DataType.PERFORMER -> queryEngine.findPerformers(performerIds = mostRecentIds)
                                DataType.TAG -> queryEngine.getTags(mostRecentIds)
                                DataType.STUDIO -> queryEngine.findStudios(studioIds = mostRecentIds)
                                DataType.GALLERY -> queryEngine.findGalleries(galleryIds = mostRecentIds)
                                else -> {
                                    listOf()
                                }
                            }
                        val results = ArrayObjectAdapter(StashPresenter.SELECTOR)
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
    }

    override fun getResultsAdapter(): ObjectAdapter {
        return adapter
    }

    override fun onQueryTextChange(newQuery: String): Boolean {
        taskJob?.cancel()
        taskJob =
            viewLifecycleOwner.lifecycleScope.launch(exceptionHandler) {
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
            viewLifecycleOwner.lifecycleScope.launch(exceptionHandler) {
                search(query)
            }
        return true
    }

    private suspend fun search(query: String) {
        searchResultsAdapter.clear()

        this.query = query
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
            val queryEngine = QueryEngine(requireContext(), true)
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
                    if (dataType in DATA_TYPE_ALLOW_CREATE) {
                        adapter.set(
                            RESULTS_POS,
                            ListRow(
                                HeaderItem(getString(R.string.stashapp_component_tagger_results_match_failed_no_result)),
                                createNewAdapter,
                            ),
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
            }
        } else {
            adapter.set(
                RESULTS_POS,
                ListRow(HeaderItem(getString(R.string.waiting_for_query)), ArrayObjectAdapter()),
            )
        }
    }

    private inner class CreateNewPresenter : StashPresenter<StashAction>() {
        override fun doOnBindViewHolder(
            cardView: StashImageCardView,
            item: StashAction,
        ) {
            cardView.setMainImageDimensions(CARD_WIDTH, CARD_HEIGHT / 2)
            cardView.titleText = "Create new ${getString(dataType.stringId)}"
            cardView.contentText = query?.replaceFirstChar(Char::titlecase)
        }
    }

    companion object {
        const val TAG = "SearchForFragment"

        const val ID_KEY = "id"
        const val RESULT_ID_KEY = "resultId"
        const val TITLE_KEY = "title"

        private const val RESULTS_POS = 0
        private const val SUGGESTIONS_POS = RESULTS_POS + 1
        private const val RECENT_POS = SUGGESTIONS_POS + 1

        // List of data types that support querying for suggestions
        val DATA_TYPE_SUGGESTIONS =
            setOf(DataType.TAG, DataType.PERFORMER, DataType.STUDIO, DataType.GALLERY)

        // List of data types that support creating a new one
        val DATA_TYPE_ALLOW_CREATE = setOf(DataType.TAG, DataType.PERFORMER)
    }
}
