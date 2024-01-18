package com.github.damontecres.stashapp

import android.content.Intent
import android.graphics.Color
import android.graphics.drawable.Drawable
import android.os.Bundle
import android.os.Handler
import android.os.Looper
import android.util.DisplayMetrics
import android.util.Log
import android.view.Gravity
import android.view.View
import android.view.ViewGroup
import android.widget.TextView
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
import com.apollographql.apollo3.api.Optional
import com.bumptech.glide.Glide
import com.bumptech.glide.request.target.SimpleTarget
import com.bumptech.glide.request.transition.Transition
import com.github.damontecres.stashapp.api.ConfigurationQuery
import com.github.damontecres.stashapp.api.type.FilterMode
import com.github.damontecres.stashapp.api.type.FindFilterType
import com.github.damontecres.stashapp.api.type.SortDirectionEnum
import com.github.damontecres.stashapp.data.StashCustomFilter
import com.github.damontecres.stashapp.data.StashSavedFilter
import com.github.damontecres.stashapp.presenters.StashPresenter
import kotlinx.coroutines.CoroutineExceptionHandler
import kotlinx.coroutines.launch
import java.util.Timer
import java.util.TimerTask

/**
 * Loads a grid of cards with movies to browse.
 */
class MainFragment : BrowseSupportFragment() {
    private val rowsAdapter = ArrayObjectAdapter(ListRowPresenter())
    private val adapters = ArrayList<ArrayObjectAdapter>()

    //    private var performerAdapter: ArrayObjectAdapter = ArrayObjectAdapter(PerformerPresenter())
//    private var studioAdapter: ArrayObjectAdapter = ArrayObjectAdapter(StudioPresenter())
//    private var sceneAdapter: ArrayObjectAdapter = ArrayObjectAdapter(ScenePresenter())
    private val mHandler = Handler(Looper.myLooper()!!)
    private lateinit var mBackgroundManager: BackgroundManager
    private var mDefaultBackground: Drawable? = null
    private lateinit var mMetrics: DisplayMetrics
    private var mBackgroundTimer: Timer? = null
    private var mBackgroundUri: String? = null

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

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
                    requireActivity().findViewById<View>(androidx.leanback.R.id.search_orb)
                } else {
                    null
                }
            }
        adapter = rowsAdapter
    }

    override fun onResume() {
        super.onResume()

        viewLifecycleOwner.lifecycleScope.launch {
            if (testStashConnection(requireContext(), false) != null) {
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

    override fun onDestroy() {
        super.onDestroy()
        Log.d(TAG, "onDestroy: " + mBackgroundTimer?.toString())
        mBackgroundTimer?.cancel()
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

    private fun updateBackground(uri: String?) {
        val width = mMetrics.widthPixels
        val height = mMetrics.heightPixels
        Glide.with(requireActivity())
            .load(uri)
            .centerCrop()
            .error(R.drawable.baseline_camera_indoor_48)
            .into<SimpleTarget<Drawable>>(
                object : SimpleTarget<Drawable>(width, height) {
                    override fun onResourceReady(
                        drawable: Drawable,
                        transition: Transition<in Drawable>?,
                    ) {
                        mBackgroundManager.drawable = drawable
                    }
                },
            )
        mBackgroundTimer?.cancel()
    }

    private fun startBackgroundTimer() {
        mBackgroundTimer?.cancel()
        mBackgroundTimer = Timer()
        mBackgroundTimer?.schedule(UpdateBackgroundTask(), BACKGROUND_UPDATE_DELAY.toLong())
    }

    private inner class UpdateBackgroundTask : TimerTask() {
        override fun run() {
            mHandler.post { updateBackground(mBackgroundUri) }
        }
    }

    private inner class GridItemPresenter : Presenter() {
        override fun onCreateViewHolder(parent: ViewGroup): ViewHolder {
            val view = TextView(parent.context)
            view.layoutParams = ViewGroup.LayoutParams(GRID_ITEM_WIDTH, GRID_ITEM_HEIGHT)
            view.isFocusable = true
            view.isFocusableInTouchMode = true
            view.setBackgroundColor(ContextCompat.getColor(activity!!, R.color.default_background))
            view.setTextColor(Color.WHITE)
            view.gravity = Gravity.CENTER
            return ViewHolder(view)
        }

        override fun onBindViewHolder(
            viewHolder: ViewHolder,
            item: Any,
        ) {
            (viewHolder.view as TextView).text = item as String
        }

        override fun onUnbindViewHolder(viewHolder: ViewHolder) {}
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
            if (serverInfo?.version?.version != null && !isStashVersionSupported(Version(serverInfo.version.version))) {
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
                        val ui = queryEngine.executeQuery(query).data?.configuration?.ui
                        if (ui != null) {
                            val frontPageContent =
                                (ui as Map<String, *>)["frontPageContent"] as List<Map<String, *>>
                            for (frontPageFilter: Map<String, *> in frontPageContent) {
                                val adapter = ArrayObjectAdapter(StashPresenter.SELECTOR)
                                adapters.add(adapter)

                                val filterType = frontPageFilter["__typename"] as String
                                if (filterType == "CustomFilter") {
                                    addCustomFilterRow(frontPageFilter, adapter, queryEngine)
                                } else if (filterType == "SavedFilter") {
                                    addSavedFilterRow(frontPageFilter, adapter, queryEngine)
                                } else {
                                    Log.w(TAG, "Unknown frontPageFilter typename: $filterType")
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
                (frontPageFilter["sortBy"] as String?)
                    ?: // Some servers may return sortBy in lowercase
                    (frontPageFilter["sortby"] as String?) ?: when (msg["id"].toString()) {
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

                val filter =
                    FindFilterType(
                        direction = Optional.presentIfNotNull(directionEnum),
                        sort = Optional.presentIfNotNull(sortBy),
                        per_page = Optional.present(25),
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
                rowsAdapter.add(
                    index,
                    ListRow(HeaderItem(result?.name ?: ""), adapter),
                )

                val filter = convertFilter(result?.find_filter)
                val objectFilter =
                    result?.object_filter as Map<String, Map<String, *>>?

                when (result?.mode) {
                    FilterMode.SCENES -> {
                        val sceneFilter =
                            convertSceneObjectFilter(objectFilter)
                        adapter.addAll(
                            0,
                            queryEngine.findScenes(filter, sceneFilter),
                        )
                    }

                    FilterMode.STUDIOS -> {
                        val studioFilter =
                            convertStudioObjectFilter(objectFilter)
                        adapter.addAll(
                            0,
                            queryEngine.findStudios(
                                filter,
                                studioFilter,
                            ),
                        )
                    }

                    FilterMode.PERFORMERS -> {
                        val performerFilter =
                            convertPerformerObjectFilter(objectFilter)
                        adapter.addAll(
                            0,
                            queryEngine.findPerformers(
                                filter,
                                performerFilter,
                            ),
                        )
                    }

                    FilterMode.TAGS -> {
                        val tagFilter =
                            convertTagObjectFilter(objectFilter)
                        adapter.addAll(
                            0,
                            queryEngine.findTags(filter, tagFilter),
                        )
                    }

                    else -> {
                        Log.w(
                            TAG,
                            "Unsupported mode in frontpage: ${result?.mode}",
                        )
                    }
                }
                adapter.add(StashSavedFilter(filterId.toString(), result!!.mode))
            } else {
                Log.w(TAG, "SavedFilter mode is ${result?.mode} which is not supported yet")
            }
        }
    }

    companion object {
        private val TAG = "MainFragment"

        private val BACKGROUND_UPDATE_DELAY = 300
        private val GRID_ITEM_WIDTH = 200
        private val GRID_ITEM_HEIGHT = 200
        private val NUM_ROWS = 6
        private val NUM_COLS = 15
    }

    private fun loadData() {
    }
}
