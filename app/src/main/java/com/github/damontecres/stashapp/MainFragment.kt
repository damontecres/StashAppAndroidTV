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
import com.github.damontecres.stashapp.presenters.PerformerPresenter
import com.github.damontecres.stashapp.presenters.ScenePresenter
import com.github.damontecres.stashapp.presenters.StudioPresenter
import com.github.damontecres.stashapp.presenters.stashPresenterSelector
import kotlinx.coroutines.CoroutineExceptionHandler
import kotlinx.coroutines.async
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
            if (testStashConnection(requireContext(), false)) {
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
        mDefaultBackground =
            ContextCompat.getDrawable(requireActivity(), R.drawable.default_background)
        mMetrics = DisplayMetrics()
        requireActivity().windowManager.defaultDisplay.getMetrics(mMetrics)
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
            itemViewHolder: Presenter.ViewHolder?, item: Any?,
            rowViewHolder: RowPresenter.ViewHolder, row: Row
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
            .error(mDefaultBackground)
            .into<SimpleTarget<Drawable>>(
                object : SimpleTarget<Drawable>(width, height) {
                    override fun onResourceReady(
                        drawable: Drawable,
                        transition: Transition<in Drawable>?
                    ) {
                        mBackgroundManager.drawable = drawable
                    }
                })
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
        override fun onCreateViewHolder(parent: ViewGroup): Presenter.ViewHolder {
            val view = TextView(parent.context)
            view.layoutParams = ViewGroup.LayoutParams(GRID_ITEM_WIDTH, GRID_ITEM_HEIGHT)
            view.isFocusable = true
            view.isFocusableInTouchMode = true
            view.setBackgroundColor(ContextCompat.getColor(activity!!, R.color.default_background))
            view.setTextColor(Color.WHITE)
            view.gravity = Gravity.CENTER
            return Presenter.ViewHolder(view)
        }

        override fun onBindViewHolder(viewHolder: Presenter.ViewHolder, item: Any) {
            (viewHolder.view as TextView).text = item as String
        }

        override fun onUnbindViewHolder(viewHolder: Presenter.ViewHolder) {}
    }

    private fun clearData() {
        adapters.forEach { it.clear() }
    }

    private fun fetchData() {
        clearData()
        viewLifecycleOwner.lifecycleScope.launch {
            if (testStashConnection(requireContext(), false)) {
                try {
                    val queryEngine = QueryEngine(requireContext(), showToasts = true)

                    val exHandler = CoroutineExceptionHandler { _, ex ->
                        Log.e(TAG, "Exception in coroutine", ex)
                    }

                    viewLifecycleOwner.lifecycleScope.launch(exHandler) {
                        val query = ConfigurationQuery()
                        val ui = queryEngine.executeQuery(query).data?.configuration?.ui
                        if (ui != null) {
                            val frontPageContent =
                                (ui as Map<String, *>)["frontPageContent"] as List<Map<String, *>>
                            for (frontPageFilter: Map<String, *> in frontPageContent) {
                                val adapter = ArrayObjectAdapter(stashPresenterSelector)
                                adapters.add(adapter)

                                val filterType = frontPageFilter["__typename"] as String
                                if (filterType == "CustomFilter") {
                                    val mode = FilterMode.valueOf(frontPageFilter["mode"] as String)
                                    val direction = frontPageFilter["direction"] as String
                                    val sortBy = frontPageFilter["sortBy"] as String
                                    val filter = FindFilterType(
                                        direction = Optional.present(
                                            SortDirectionEnum.safeValueOf(
                                                direction
                                            )
                                        ),
                                        sort = Optional.present(sortBy),
                                        per_page = Optional.present(25)
                                    )

                                    val msg = frontPageFilter["message"] as Map<String, *>
                                    val objType =
                                        (msg["values"] as Map<String, String>)["objects"] as String
                                    val description = when (msg["id"] as String) {
                                        "recently_added_objects" -> "Recently Added $objType"
                                        "recently_released_objects" -> "Recently Released $objType"
                                        else -> ""
                                    }

                                    when (mode) {
                                        FilterMode.SCENES -> {
                                            rowsAdapter.add(
                                                ListRow(
                                                    HeaderItem(description),
                                                    adapter
                                                )
                                            )
                                            adapter.addAll(0, queryEngine.findScenes(filter))
                                        }

                                        FilterMode.STUDIOS -> {
                                            rowsAdapter.add(
                                                ListRow(
                                                    HeaderItem(description),
                                                    adapter
                                                )
                                            )
                                            adapter.addAll(0, queryEngine.findStudios(filter))
                                        }

                                        FilterMode.PERFORMERS -> {
                                            rowsAdapter.add(
                                                ListRow(
                                                    HeaderItem(description),
                                                    adapter
                                                )
                                            )
                                            adapter.addAll(0, queryEngine.findPerformers(filter))
                                        }

                                        else -> {
                                            Log.i(TAG, "Unsupported mode in frontpage: $mode")
                                        }
                                    }
                                } else if (filterType == "SavedFilter") {
                                    val filterId = frontPageFilter["savedFilterId"]
                                    val header = HeaderItem("")
                                    val listRow = ListRow(header, adapter)
                                    rowsAdapter.add(listRow) // TODO check if supported
                                    viewLifecycleOwner.lifecycleScope.launch(exHandler) {
                                        val result = queryEngine.getSavedFilter(filterId.toString())

                                        val index = rowsAdapter.indexOf(listRow)
                                        rowsAdapter.removeItems(index, 1)
                                        rowsAdapter.add(
                                            index,
                                            ListRow(HeaderItem(result?.name ?: ""), adapter)
                                        )

                                        val filter = convertFilter(result?.find_filter)
                                        val objectFilter = result?.object_filter
                                        // TODO convert object filters

                                        when (result?.mode) {
                                            FilterMode.SCENES -> {
                                                adapter.addAll(0, queryEngine.findScenes(filter))
                                            }

                                            FilterMode.STUDIOS -> {
                                                adapter.addAll(0, queryEngine.findStudios(filter))
                                            }

                                            FilterMode.PERFORMERS -> {
                                                adapter.addAll(
                                                    0,
                                                    queryEngine.findPerformers(filter)
                                                )
                                            }

                                            FilterMode.TAGS -> {
                                                adapter.addAll(0, queryEngine.findTags(filter))
                                            }

                                            else -> {
                                                Log.i(
                                                    TAG,
                                                    "Unsupported mode in frontpage: ${result?.mode}"
                                                )
                                            }
                                        }
                                    }
                                }
                            }
                        }
                    }
                } catch (ex: QueryEngine.StashNotConfiguredException) {
                    Toast.makeText(
                        requireContext(),
                        "Stash not configured. Please enter the URL in settings!",
                        Toast.LENGTH_LONG
                    ).show()
                }
            } else {
                rowsAdapter.clear()
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