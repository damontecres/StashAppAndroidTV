package com.github.damontecres.stashapp

import android.content.Intent
import android.os.Bundle
import android.util.DisplayMetrics
import android.util.Log
import android.view.View
import android.widget.Toast
import androidx.core.content.ContextCompat
import androidx.leanback.app.BackgroundManager
import androidx.leanback.app.BrowseSupportFragment
import androidx.leanback.widget.ArrayObjectAdapter
import androidx.leanback.widget.BrowseFrameLayout
import androidx.leanback.widget.BrowseFrameLayout.OnFocusSearchListener
import androidx.leanback.widget.HeaderItem
import androidx.leanback.widget.ListRow
import androidx.leanback.widget.ListRowPresenter
import androidx.leanback.widget.OnItemViewSelectedListener
import androidx.leanback.widget.Presenter
import androidx.leanback.widget.Row
import androidx.leanback.widget.RowPresenter
import androidx.lifecycle.lifecycleScope
import androidx.preference.PreferenceManager
import com.apollographql.apollo3.api.Optional
import com.github.damontecres.stashapp.api.ConfigurationQuery
import com.github.damontecres.stashapp.api.fragment.SlimSceneData
import com.github.damontecres.stashapp.api.type.FilterMode
import com.github.damontecres.stashapp.api.type.FindFilterType
import com.github.damontecres.stashapp.api.type.SortDirectionEnum
import com.github.damontecres.stashapp.data.StashCustomFilter
import com.github.damontecres.stashapp.data.StashSavedFilter
import com.github.damontecres.stashapp.presenters.StashPresenter
import com.github.damontecres.stashapp.util.FilterParser
import com.github.damontecres.stashapp.util.QueryEngine
import com.github.damontecres.stashapp.util.ServerPreferences
import com.github.damontecres.stashapp.util.StashCoroutineExceptionHandler
import com.github.damontecres.stashapp.util.Version
import com.github.damontecres.stashapp.util.convertFilter
import com.github.damontecres.stashapp.util.getCaseInsensitive
import com.github.damontecres.stashapp.util.supportedFilterModes
import com.github.damontecres.stashapp.util.testStashConnection
import kotlinx.coroutines.CoroutineExceptionHandler
import kotlinx.coroutines.launch
import java.util.Objects

/**
 * Loads a grid of cards with movies to browse.
 */
class MainFragment : BrowseSupportFragment() {
    private val rowsAdapter = ArrayObjectAdapter(ListRowPresenter())
    private val adapters = ArrayList<ArrayObjectAdapter>()
    private lateinit var mBackgroundManager: BackgroundManager
    private lateinit var mMetrics: DisplayMetrics
    private var serverHash: Int? = null

    /**
     * This just hashes a few preferences that affect what this fragment shows
     */
    private fun computeServerHash(): Int {
        val manager = PreferenceManager.getDefaultSharedPreferences(requireContext())
        val url = manager.getString("stashUrl", null)
        val apiKey = manager.getString("stashApiKey", null)
        val maxSearchResults = manager.getInt("maxSearchResults", 0)
        val playVideoPreviews = manager.getBoolean("playVideoPreviews", true)
        val columns = manager.getInt("numberOfColumns", 5)
        return Objects.hash(url, apiKey, maxSearchResults, playVideoPreviews, columns)
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        serverHash = computeServerHash()

        headersState = HEADERS_DISABLED
    }

    override fun onActivityCreated(savedInstanceState: Bundle?) {
        Log.i(TAG, "onCreate")
        super.onActivityCreated(savedInstanceState)

        prepareBackgroundManager()

        setupUIElements()

        setupEventListeners()

        // Override the focus search so that pressing up from the rows will move to search first
        val browseFrameLayout =
            requireView().findViewById<BrowseFrameLayout>(androidx.leanback.R.id.browse_frame)
        val originalOnFocusSearchListener = browseFrameLayout.onFocusSearchListener
        browseFrameLayout.onFocusSearchListener =
            OnFocusSearchListener { focused: View?, direction: Int ->
                if (direction == View.FOCUS_UP) {
                    requireActivity().findViewById(androidx.leanback.R.id.search_orb)
                } else {
                    null
                }
            }
        adapter = rowsAdapter
    }

    override fun onResume() {
        super.onResume()

        try {
            val position = getCurrentPosition()
            if (position != null) {
                val adapter = adapters[position.row]
                val item = adapter.get(position.column)
                if (item is SlimSceneData) {
                    viewLifecycleOwner.lifecycleScope.launch(StashCoroutineExceptionHandler()) {
                        val queryEngine = QueryEngine(requireContext())
                        queryEngine.getScene(item.id.toInt())?.let {
                            adapter.replace(position.column, it)
                        }
                    }
                }
            }
        } catch (ex: Exception) {
            Log.e(TAG, "Exception", ex)
        }

        val newServerHash = computeServerHash()
        if (serverHash != newServerHash) {
            clearData()
            rowsAdapter.clear()
        }
        serverHash = newServerHash

        viewLifecycleOwner.lifecycleScope.launch {
            if (testStashConnection(requireContext(), false) != null) {
                ServerPreferences(requireContext()).updatePreferences()
                val mainTitleView =
                    requireActivity().findViewById<MainTitleView>(R.id.browse_title_group)
                mainTitleView.refreshMenuItems()
                if (rowsAdapter.size() == 0) {
                    fetchData()
                }
            } else {
                clearData()
                rowsAdapter.clear()
                Toast.makeText(requireContext(), "Connection to Stash failed.", Toast.LENGTH_LONG)
                    .show()
            }
        }
    }

    private fun prepareBackgroundManager() {
        mBackgroundManager = BackgroundManager.getInstance(activity)
        mBackgroundManager.attach(requireActivity().window)
    }

    private fun setupUIElements() {
        title = getString(R.string.browse_title)
        // over title
        isHeadersTransitionOnBackEnabled = true

        // set fastLane (or headers) background color
        brandColor = ContextCompat.getColor(requireActivity(), R.color.fastlane_background)
        // set search icon color
        searchAffordanceColor = ContextCompat.getColor(requireActivity(), R.color.search_opaque)
    }

    private fun setupEventListeners() {
        setOnSearchClickedListener {
//            Toast.makeText(activity!!, "Implement your own in-app search", Toast.LENGTH_LONG)
//                .show()
            requireActivity().startActivity(Intent(requireContext(), SearchActivity::class.java))
        }

        onItemViewClickedListener = StashItemViewClickListener(requireActivity())
        onItemViewSelectedListener = ItemViewSelectedListener()
    }

    private inner class ItemViewSelectedListener : OnItemViewSelectedListener {
        override fun onItemSelected(
            itemViewHolder: Presenter.ViewHolder?,
            item: Any?,
            rowViewHolder: RowPresenter.ViewHolder,
            row: Row,
        ) {
            // TODO background
//            if (item is SlimSceneData) {
//                mBackgroundUri = item.backgroundImageUrl
//                startBackgroundTimer()
//            }
        }
    }

    private fun clearData() {
        adapters.forEach { it.clear() }
    }

    private fun fetchData() {
        clearData()
        viewLifecycleOwner.lifecycleScope.launch(
            CoroutineExceptionHandler { _, ex ->
                Log.e(TAG, "Exception in fetchData coroutine", ex)
                Toast.makeText(
                    requireContext(),
                    "Error fetching data: ${ex.message}",
                    Toast.LENGTH_LONG,
                ).show()
            },
        ) {
            val serverInfo = testStashConnection(requireContext(), false)
            if (serverInfo?.version?.version == null) {
                Log.w(TAG, "Version returned by server is null")
                Toast.makeText(
                    requireContext(),
                    "Could not determine the server version. Things may not work!",
                    Toast.LENGTH_LONG,
                ).show()
            }
            FilterParser.initialize(requireContext(), serverInfo)

            if (serverInfo?.version?.version != null &&
                !Version.isStashVersionSupported(
                    Version.fromString(
                        serverInfo.version.version,
                    ),
                )
            ) {
                val msg =
                    "Stash server version ${serverInfo.version.version} is not supported!"
                Log.e(TAG, msg)
                Toast.makeText(requireContext(), msg, Toast.LENGTH_LONG).show()
                rowsAdapter.clear()
            } else if (serverInfo != null) {
                try {
                    val queryEngine = QueryEngine(requireContext(), showToasts = true)

                    val exHandler =
                        CoroutineExceptionHandler { _, ex ->
                            Log.e(TAG, "Exception in coroutine", ex)
                        }

                    viewLifecycleOwner.lifecycleScope.launch(exHandler) {
                        val query = ConfigurationQuery()
                        val config = queryEngine.executeQuery(query).data?.configuration
                        val serverPreferences = ServerPreferences(requireContext())
                        serverPreferences.updatePreferences(config)

                        if (config?.ui != null) {
                            val ui = config.ui
                            val frontPageContent =
                                (ui as Map<String, *>)["frontPageContent"] as List<Map<String, *>>
                            for (frontPageFilter: Map<String, *> in frontPageContent) {
                                val adapter = ArrayObjectAdapter(StashPresenter.SELECTOR)
                                adapters.add(adapter)

                                when (val filterType = frontPageFilter["__typename"] as String) {
                                    "CustomFilter" -> {
                                        addCustomFilterRow(frontPageFilter, adapter, queryEngine)
                                    }

                                    "SavedFilter" -> {
                                        addSavedFilterRow(frontPageFilter, adapter, queryEngine)
                                    }

                                    else -> {
                                        Log.w(TAG, "Unknown frontPageFilter typename: $filterType")
                                    }
                                }
                            }
                        }
                    }
                } catch (ex: QueryEngine.StashNotConfiguredException) {
                    Toast.makeText(
                        requireContext(),
                        "Stash not configured. Please enter the URL in settings!",
                        Toast.LENGTH_LONG,
                    ).show()
                }
            } else {
                rowsAdapter.clear()
            }
        }
    }

    private fun addCustomFilterRow(
        frontPageFilter: Map<String, *>,
        adapter: ArrayObjectAdapter,
        queryEngine: QueryEngine,
    ) {
        val exHandler =
            CoroutineExceptionHandler { _, ex ->
                Log.e(TAG, "Exception in addCustomFilterRow", ex)
            }
        try {
            val msg = frontPageFilter["message"] as Map<String, *>
            val objType =
                (msg["values"] as Map<String, String>)["objects"] as String
            val description =
                when (msg["id"].toString()) {
                    "recently_added_objects" -> "Recently Added $objType"
                    "recently_released_objects" -> "Recently Released $objType"
                    else -> objType
                }

            val sortBy =
                (frontPageFilter.getCaseInsensitive("sortBy") as String?)
                    ?: when (msg["id"].toString()) {
                        // Just in case, fall back to a reasonable default
                        "recently_added_objects" -> "created_at"
                        "recently_released_objects" -> "date"
                        else -> null
                    }
            val mode = FilterMode.safeValueOf(frontPageFilter["mode"] as String)
            if (mode !in supportedFilterModes) {
                Log.w(TAG, "CustomFilter mode is $mode which is not supported yet")
                return
            }
            rowsAdapter.add(
                ListRow(
                    HeaderItem(description),
                    adapter,
                ),
            )
            viewLifecycleOwner.lifecycleScope.launch(exHandler) {
                val direction = frontPageFilter["direction"] as String?
                val directionEnum =
                    if (direction != null) {
                        val enum = SortDirectionEnum.safeValueOf(direction.uppercase())
                        if (enum == SortDirectionEnum.UNKNOWN__) {
                            SortDirectionEnum.DESC
                        }
                        enum
                    } else {
                        SortDirectionEnum.DESC
                    }
                val pageSize =
                    PreferenceManager.getDefaultSharedPreferences(requireContext())
                        .getInt("maxSearchResults", 25)
                val filter =
                    FindFilterType(
                        direction = Optional.presentIfNotNull(directionEnum),
                        sort = Optional.presentIfNotNull(sortBy),
                        per_page = Optional.present(pageSize),
                    )

                when (mode) {
                    FilterMode.SCENES -> {
                        adapter.addAll(0, queryEngine.findScenes(filter))
                    }

                    FilterMode.STUDIOS -> {
                        adapter.addAll(0, queryEngine.findStudios(filter))
                    }

                    FilterMode.PERFORMERS -> {
                        adapter.addAll(0, queryEngine.findPerformers(filter))
                    }

                    FilterMode.MOVIES -> {
                        adapter.addAll(0, queryEngine.findMovies(filter))
                    }

                    else -> {
                        Log.w(TAG, "Unsupported mode in frontpage: $mode")
                    }
                }
                adapter.add(StashCustomFilter(mode, direction, sortBy, description))
            }
        } catch (ex: Exception) {
            Log.e(TAG, "Exception during addCustomFilterRow", ex)
            Toast.makeText(
                requireContext(),
                "CustomFilter parse error: ${ex.message}",
                Toast.LENGTH_LONG,
            ).show()
        }
    }

    private fun addSavedFilterRow(
        frontPageFilter: Map<String, *>,
        adapter: ArrayObjectAdapter,
        queryEngine: QueryEngine,
    ) {
        val exHandler =
            CoroutineExceptionHandler { _, ex ->
                Log.e(TAG, "Exception in addSavedFilterRow", ex)
                Toast.makeText(
                    requireContext(),
                    "Error fetching saved filter. This is probably a bug!",
                    Toast.LENGTH_LONG,
                ).show()
            }
        val header = HeaderItem("")
        val listRow = ListRow(header, adapter)
        rowsAdapter.add(listRow)
        viewLifecycleOwner.lifecycleScope.launch(exHandler) {
            val filterId = frontPageFilter["savedFilterId"]
            val result = queryEngine.getSavedFilter(filterId.toString())

            val index = rowsAdapter.indexOf(listRow)
            rowsAdapter.removeItems(index, 1)

            if (result?.mode in supportedFilterModes) {
                // TODO doing it this way will result it adding an unsupported row then removing it which looks weird, in practice though it happens pretty fast
                rowsAdapter.add(index, ListRow(HeaderItem(result?.name ?: ""), adapter))

                val pageSize =
                    PreferenceManager.getDefaultSharedPreferences(requireContext())
                        .getInt("maxSearchResults", 25)

                val filter =
                    queryEngine.updateFilter(convertFilter(result?.find_filter), useRandom = true)
                        ?.copy(per_page = Optional.present(pageSize))
                val objectFilter =
                    result?.object_filter as Map<String, Map<String, *>>?

                when (result?.mode) {
                    FilterMode.SCENES -> {
                        val sceneFilter =
                            FilterParser.instance.convertSceneObjectFilter(objectFilter)
                        adapter.addAll(
                            0,
                            queryEngine.findScenes(filter, sceneFilter, useRandom = false),
                        )
                    }

                    FilterMode.STUDIOS -> {
                        val studioFilter =
                            FilterParser.instance.convertStudioObjectFilter(objectFilter)
                        adapter.addAll(
                            0,
                            queryEngine.findStudios(
                                filter,
                                studioFilter,
                                useRandom = false,
                            ),
                        )
                    }

                    FilterMode.PERFORMERS -> {
                        val performerFilter =
                            FilterParser.instance.convertPerformerObjectFilter(objectFilter)
                        adapter.addAll(
                            0,
                            queryEngine.findPerformers(
                                filter,
                                performerFilter,
                                useRandom = false,
                            ),
                        )
                    }

                    FilterMode.TAGS -> {
                        val tagFilter =
                            FilterParser.instance.convertTagObjectFilter(objectFilter)
                        adapter.addAll(
                            0,
                            queryEngine.findTags(filter, tagFilter, useRandom = false),
                        )
                    }

                    else -> {
                        Log.w(
                            TAG,
                            "Unsupported mode in frontpage: ${result?.mode}",
                        )
                    }
                }
                adapter.add(
                    StashSavedFilter(
                        filterId.toString(),
                        result!!.mode,
                        filter?.sort?.getOrNull(),
                    ),
                )
            } else {
                Log.w(TAG, "SavedFilter mode is ${result?.mode} which is not supported yet")
            }
        }
    }

    private fun getCurrentPosition(): Position? {
        val rowPos = selectedPosition
        if (rowPos >= 0 && selectedRowViewHolder != null) {
            val columnPos =
                (selectedRowViewHolder as ListRowPresenter.ViewHolder).gridView.selectedPosition
            if (columnPos >= 0) {
                Log.v(TAG, "row=$rowPos, column=$columnPos")
                return Position(rowPos, columnPos)
            }
        }
        return null
    }

    /**
     * Return true if back was handled
     */
    fun onBackPressed(): Boolean {
        val pos = getCurrentPosition()
        if (pos != null) {
            if (pos.column > 0) {
                selectedPosition = pos.row
                (selectedRowViewHolder as ListRowPresenter.ViewHolder).gridView.selectedPosition = 0
                return true
            } else if (pos.row > 0) {
                selectedPosition = 0
                return true
            }
        }
        return false
    }

    data class Position(val row: Int, val column: Int)

    companion object {
        private const val TAG = "MainFragment"
    }
}
