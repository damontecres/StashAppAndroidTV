package com.github.damontecres.stashapp

import android.os.Bundle
import android.text.TextUtils
import android.util.Log
import android.widget.Toast
import androidx.appcompat.content.res.AppCompatResources
import androidx.core.os.bundleOf
import androidx.fragment.app.activityViewModels
import androidx.fragment.app.setFragmentResult
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
import com.github.damontecres.stashapp.actions.StashAction
import com.github.damontecres.stashapp.api.fragment.GroupData
import com.github.damontecres.stashapp.api.fragment.PerformerData
import com.github.damontecres.stashapp.api.fragment.StashData
import com.github.damontecres.stashapp.api.fragment.TagData
import com.github.damontecres.stashapp.api.type.CriterionModifier
import com.github.damontecres.stashapp.api.type.FindFilterType
import com.github.damontecres.stashapp.api.type.GalleryFilterType
import com.github.damontecres.stashapp.api.type.GroupCreateInput
import com.github.damontecres.stashapp.api.type.PerformerCreateInput
import com.github.damontecres.stashapp.api.type.SortDirectionEnum
import com.github.damontecres.stashapp.api.type.StringCriterionInput
import com.github.damontecres.stashapp.api.type.TagCreateInput
import com.github.damontecres.stashapp.data.DataType
import com.github.damontecres.stashapp.data.room.RecentSearchItem
import com.github.damontecres.stashapp.navigation.Destination
import com.github.damontecres.stashapp.presenters.StashImageCardView
import com.github.damontecres.stashapp.presenters.StashPresenter
import com.github.damontecres.stashapp.util.MutationEngine
import com.github.damontecres.stashapp.util.QueryEngine
import com.github.damontecres.stashapp.util.StashCoroutineExceptionHandler
import com.github.damontecres.stashapp.util.StashServer
import com.github.damontecres.stashapp.util.defaultCardHeight
import com.github.damontecres.stashapp.util.defaultCardWidth
import com.github.damontecres.stashapp.util.getDestination
import com.github.damontecres.stashapp.util.isNotNullOrBlank
import com.github.damontecres.stashapp.util.putDataType
import com.github.damontecres.stashapp.util.readOnlyModeDisabled
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
 * Search for an item of a specific [DataType]
 *
 * Will load recently used items via [com.github.damontecres.stashapp.data.room.RecentSearchItemsDao]
 */
class SearchForFragment :
    SearchSupportFragment(),
    SearchSupportFragment.SearchResultProvider {
    private val serverViewModel by activityViewModels<ServerViewModel>()

    private var taskJob: Job? = null
    private var query: String? = null

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

    private val server = StashServer.requireCurrentServer()
    private val queryEngine = QueryEngine(server)

    private lateinit var searchFor: Destination.SearchFor
    private lateinit var dataType: DataType

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        searchFor = requireArguments().getDestination<Destination.SearchFor>()
        dataType = searchFor.dataType
        perPage =
            PreferenceManager
                .getDefaultSharedPreferences(requireContext())
                .getInt("maxSearchResults", 25)
        title = searchFor.title ?: getString(dataType.pluralStringId)

        searchResultsAdapter.presenterSelector =
            StashPresenter
                .defaultClassPresenterSelector()
                .addClassPresenter(StashAction::class.java, CreateNewPresenter(dataType))
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
            } else if (item is StashData) {
                returnId(item)
            } else {
                throw IllegalStateException("Unknown item: $item")
            }
        }
    }

    private suspend fun handleCreate(query: String?) {
        if (query.isNotNullOrBlank()) {
            val name = query.replaceFirstChar(Char::titlecase)
            val mutationEngine = MutationEngine(server)
            val item =
                when (dataType) {
                    DataType.TAG -> {
                        mutationEngine.createTag(TagCreateInput(name = name))
                    }

                    DataType.PERFORMER -> {
                        mutationEngine.createPerformer(PerformerCreateInput(name = name))
                    }

                    DataType.GROUP -> {
                        mutationEngine.createGroup(GroupCreateInput(name = name))
                    }

                    else -> throw IllegalArgumentException("Unsupported datatype $dataType")
                }
            if (item != null) {
                Toast
                    .makeText(
                        requireContext(),
                        "Created new ${getString(dataType.stringId)}: $name",
                        Toast.LENGTH_LONG,
                    ).show()
                returnId(item)
            }
        }
    }

    private fun returnId(item: StashData?) {
        if (item != null) {
            val currentServer = serverViewModel.currentServer.value
            if (dataType in DATA_TYPE_SUGGESTIONS && currentServer != null) {
                viewLifecycleOwner.lifecycleScope.launch(Dispatchers.IO + StashCoroutineExceptionHandler()) {
                    StashApplication
                        .getDatabase()
                        .recentSearchItemsDao()
                        .insert(RecentSearchItem(currentServer.url, item.id, dataType))
                }
            }
            setFragmentResult(
                searchFor.requestKey,
                bundleOf(
                    RESULT_ID_KEY to searchFor.sourceId,
                    RESULT_ITEM_ID_KEY to item.id,
                ).putDataType(dataType),
            )
            serverViewModel.navigationManager.goBack()
        }
    }

    override fun onResume() {
        super.onResume()
        if (dataType in DATA_TYPE_SUGGESTIONS) {
            val presenterSelector = StashPresenter.defaultClassPresenterSelector()

            viewLifecycleOwner.lifecycleScope.launch(
                StashCoroutineExceptionHandler {
                    Toast.makeText(
                        requireContext(),
                        "Error loading suggestions: ${it.message}",
                        Toast.LENGTH_LONG,
                    )
                },
            ) {
                val resultsAdapter = ArrayObjectAdapter(presenterSelector)
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
                val currentServer = serverViewModel.requireServer()
                val mostRecentIds =
                    StashApplication
                        .getDatabase()
                        .recentSearchItemsDao()
                        .getMostRecent(perPage, currentServer.url, dataType)
                        .map { it.id }
                Log.v(TAG, "Got ${mostRecentIds.size} recent items")
                if (mostRecentIds.isNotEmpty()) {
                    val items =
                        when (dataType) {
                            DataType.PERFORMER -> queryEngine.findPerformers(performerIds = mostRecentIds)
                            DataType.TAG -> queryEngine.getTags(mostRecentIds)
                            DataType.STUDIO -> queryEngine.findStudios(studioIds = mostRecentIds)
                            DataType.GALLERY -> queryEngine.findGalleries(galleryIds = mostRecentIds)
                            DataType.GROUP -> queryEngine.findGroups(groupIds = mostRecentIds)
                            else -> {
                                listOf()
                            }
                        }
                    val results = ArrayObjectAdapter(presenterSelector)
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
                val createAllowed = allowCreate(query, results)
                if (createAllowed && readOnlyModeDisabled()) {
                    searchResultsAdapter.add(StashAction.CREATE_NEW)
                }
                if (results.isNotEmpty()) {
                    searchResultsAdapter.addAll(searchResultsAdapter.size(), results)
                    adapter.set(
                        RESULTS_POS,
                        ListRow(HeaderItem(getString(R.string.results)), searchResultsAdapter),
                    )
                } else {
                    if (createAllowed && readOnlyModeDisabled()) {
                        adapter.set(
                            RESULTS_POS,
                            ListRow(
                                HeaderItem(getString(R.string.stashapp_component_tagger_results_match_failed_no_result)),
                                searchResultsAdapter,
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

    private fun allowCreate(
        query: String,
        items: List<StashData>,
    ): Boolean {
        val q = query.lowercase()
        return when (dataType) {
            DataType.GROUP -> {
                items as List<GroupData>
                items.none { it.name.lowercase() == q || it.aliases?.lowercase() == q }
            }

            DataType.PERFORMER -> {
                items as List<PerformerData>
                items.none { it.name.lowercase() == q || it.alias_list.any { it.lowercase() == q } }
            }

            DataType.TAG -> {
                items as List<TagData>
                items.none { it.name.lowercase() == q || it.aliases.any { it.lowercase() == q } }
            }

            DataType.SCENE -> false
            DataType.MARKER -> false
            DataType.STUDIO -> false
            DataType.IMAGE -> false
            DataType.GALLERY -> false
        }
    }

    private inner class CreateNewPresenter(
        val dataType: DataType,
    ) : StashPresenter<StashAction>() {
        override fun doOnBindViewHolder(
            cardView: StashImageCardView,
            item: StashAction,
        ) {
            cardView.setMainImageDimensions(dataType.defaultCardWidth, dataType.defaultCardHeight)
            cardView.titleText = "Create new ${getString(dataType.stringId)}"
            cardView.contentText = query?.replaceFirstChar(Char::titlecase)
            cardView.mainImage =
                AppCompatResources.getDrawable(requireContext(), R.drawable.baseline_add_box_24)
        }
    }

    companion object {
        const val TAG = "SearchForFragment"

        const val ID_KEY = "id"
        const val RESULT_ID_KEY = "$TAG.sourceId"
        const val RESULT_ITEM_ID_KEY = "$TAG.resultId"

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
