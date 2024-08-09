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
import androidx.room.Room
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
import com.github.damontecres.stashapp.api.type.FindFilterType
import com.github.damontecres.stashapp.api.type.PerformerCreateInput
import com.github.damontecres.stashapp.api.type.SortDirectionEnum
import com.github.damontecres.stashapp.api.type.TagCreateInput
import com.github.damontecres.stashapp.data.DataType
import com.github.damontecres.stashapp.data.room.AppDatabase
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

class SearchForFragment(
    private val dataType: DataType,
) :
    SearchSupportFragment(),
        SearchSupportFragment.SearchResultProvider {
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

    private val db =
        Room.databaseBuilder(
            StashApplication.getApplication(),
            AppDatabase::class.java,
            "search_for",
        ).build()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        perPage =
            PreferenceManager.getDefaultSharedPreferences(requireContext())
                .getInt("maxSearchResults", 25)
        title =
            requireActivity().intent.getStringExtra(TITLE_KEY) ?: getString(dataType.pluralStringId)
        adapter.set(0, ListRow(HeaderItem("Results"), searchResultsAdapter))

        searchResultsAdapter.presenterSelector = StashPresenter.SELECTOR

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
            if (currentServer != null) {
                viewLifecycleOwner.lifecycleScope.launch(Dispatchers.IO + StashCoroutineExceptionHandler()) {
                    db.recentSearchItemsDao()
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
                val results = ArrayObjectAdapter(StashPresenter.SELECTOR)
                val queryEngine = QueryEngine(requireContext(), false)
                val filter =
                    FindFilterType(
                        direction = Optional.present(SortDirectionEnum.DESC),
                        per_page = Optional.present(perPage),
                        sort = Optional.present("scenes_count"),
                    )
                results.addAll(0, queryEngine.find(dataType, filter))
                adapter.set(1, ListRow(HeaderItem("Suggestions"), results))
            }
            viewLifecycleOwner.lifecycleScope.launch(Dispatchers.IO + StashCoroutineExceptionHandler()) {
                val currentServer = StashServer.getCurrentStashServer(requireContext())
                val queryEngine = QueryEngine(requireContext(), false)
                if (currentServer != null) {
                    val mostRecentIds =
                        db.recentSearchItemsDao()
                            .getMostRecent(perPage, currentServer.url, dataType).map { it.id }
                    Log.v(TAG, "Got ${mostRecentIds.size} recent items")
                    if (mostRecentIds.isNotEmpty()) {
                        val items =
                            when (dataType) {
                                DataType.PERFORMER -> queryEngine.findPerformers(performerIds = mostRecentIds)
                                DataType.TAG -> queryEngine.getTags(mostRecentIds)
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
                                adapter.set(2, ListRow(HeaderItem("Recently used"), results))
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
            val filter =
                FindFilterType(
                    q = Optional.present(query),
                    per_page = Optional.present(perPage),
                )
            val queryEngine = QueryEngine(requireContext(), true)
            viewLifecycleOwner.lifecycleScope.launch(exceptionHandler) {
                val results = queryEngine.find(dataType, filter)
                searchResultsAdapter.addAll(0, results)
                val itemExists =
                    results.map {
                        when (dataType) {
                            DataType.TAG -> (it as TagData).name
                            DataType.MOVIE -> (it as MovieData).name
                            DataType.STUDIO -> (it as StudioData).name
                            DataType.PERFORMER -> (it as PerformerData).name
                            else -> throw IllegalArgumentException("Unsupported datatype $dataType")
//                        DataType.SCENE -> (it as SlimSceneData).id
//                        DataType.MARKER -> (it as MarkerData).id
//                        DataType.IMAGE -> (it as ImageData).id
//                        DataType.GALLERY -> (it as GalleryData).id
                        }.lowercase()
                    }.contains(query.lowercase())
                if (dataType in DATA_TYPE_ALLOW_CREATE && !itemExists) {
                    if (adapter.lookup(2) == null) {
                        adapter.set(
                            2,
                            ListRow(
                                HeaderItem("Create " + getString(dataType.stringId)),
                                createNewAdapter,
                            ),
                        )
                    }
                    createNewAdapter.notifyItemRangeChanged(0, 1)
                } else {
                    adapter.clear(2)
                }
            }
        } else {
            adapter.clear(2)
        }
    }

    private inner class CreateNewPresenter : StashPresenter<StashAction>() {
        override fun doOnBindViewHolder(
            cardView: StashImageCardView,
            item: StashAction,
        ) {
            cardView.setMainImageDimensions(CARD_WIDTH, CARD_HEIGHT)
            cardView.titleText = query?.replaceFirstChar(Char::titlecase)
            cardView.contentText = "Create new ${getString(dataType.stringId)}"
        }
    }

    companion object {
        const val TAG = "SearchForFragment"

        const val ID_KEY = "id"
        const val RESULT_ID_KEY = "resultId"
        const val TITLE_KEY = "title"

        // List of data types that support querying for suggestions
        val DATA_TYPE_SUGGESTIONS = setOf(DataType.TAG, DataType.PERFORMER, DataType.STUDIO)

        // List of data types that support creating a new one
        val DATA_TYPE_ALLOW_CREATE = setOf(DataType.TAG, DataType.PERFORMER)
    }
}
