package com.github.damontecres.stashapp

import android.content.Intent
import java.util.Timer
import java.util.TimerTask

import android.graphics.Color
import android.graphics.drawable.Drawable
import android.os.Bundle
import android.os.Handler
import android.os.Looper
import androidx.leanback.app.BackgroundManager
import androidx.leanback.app.BrowseSupportFragment
import androidx.leanback.widget.ArrayObjectAdapter
import androidx.leanback.widget.HeaderItem
import androidx.leanback.widget.ListRow
import androidx.leanback.widget.ListRowPresenter
import androidx.leanback.widget.OnItemViewSelectedListener
import androidx.leanback.widget.Presenter
import androidx.leanback.widget.Row
import androidx.leanback.widget.RowPresenter
import androidx.core.content.ContextCompat
import android.util.DisplayMetrics
import android.util.Log
import android.view.Gravity
import android.view.ViewGroup
import android.widget.TextView
import android.widget.Toast
import androidx.lifecycle.lifecycleScope
import com.apollographql.apollo3.api.Optional
import com.apollographql.apollo3.exception.ApolloException

import com.bumptech.glide.Glide
import com.bumptech.glide.request.target.SimpleTarget
import com.bumptech.glide.request.transition.Transition
import com.github.damontecres.stashapp.api.FindPerformersQuery
import com.github.damontecres.stashapp.api.FindScenesQuery
import com.github.damontecres.stashapp.api.FindStudiosQuery
import com.github.damontecres.stashapp.api.SystemStatusQuery
import com.github.damontecres.stashapp.api.type.FindFilterType
import com.github.damontecres.stashapp.api.type.SortDirectionEnum
import com.github.damontecres.stashapp.presenters.PerformerPresenter
import com.github.damontecres.stashapp.presenters.ScenePresenter
import com.github.damontecres.stashapp.presenters.StudioPresenter
import kotlinx.coroutines.launch

/**
 * Loads a grid of cards with movies to browse.
 */
class MainFragment : BrowseSupportFragment() {

    private var performerAdapter: ArrayObjectAdapter =ArrayObjectAdapter(PerformerPresenter())
    private var studioAdapter: ArrayObjectAdapter= ArrayObjectAdapter(StudioPresenter())
    private var sceneAdapter: ArrayObjectAdapter= ArrayObjectAdapter(ScenePresenter())
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

        val rowsAdapter = ArrayObjectAdapter(ListRowPresenter())
        rowsAdapter.add(ListRow(HeaderItem("RECENTLY RELEASED SCENES"), sceneAdapter))
        rowsAdapter.add(ListRow(HeaderItem("RECENTLY ADDED STUDIOS"), studioAdapter))
        rowsAdapter.add(ListRow(HeaderItem("RECENTLY ADDED PERFORMERS"), performerAdapter))
        adapter = rowsAdapter

    }

    override fun onResume() {
        super.onResume()

        // Only query if there are no scenes
        if(sceneAdapter.size()==0) {
            val apolloClient = createApolloClient(requireContext())
            if (apolloClient != null) {
                viewLifecycleOwner.lifecycleScope.launch {
                    try {
                        apolloClient.query(SystemStatusQuery()).execute()

                        viewLifecycleOwner.lifecycleScope.launch {
                            val results = apolloClient.query(
                                FindScenesQuery(
                                    filter = Optional.present(
                                        FindFilterType(
                                            sort = Optional.present("date"),
                                            direction = Optional.present(SortDirectionEnum.DESC),
                                            per_page = Optional.present(25)
                                        )
                                    )
                                )
                            ).execute()
                            val scenes = results.data?.findScenes?.scenes?.map {
                                it.slimSceneData
                            }
                            if (scenes != null) {
                                sceneAdapter.addAll(0, scenes)
                            }
                        }

                        viewLifecycleOwner.lifecycleScope.launch {
                            val results = apolloClient.query(
                                FindPerformersQuery(
                                    filter = Optional.present(
                                        FindFilterType(
                                            sort = Optional.present("created_at"),
                                            direction = Optional.present(SortDirectionEnum.DESC),
                                            per_page = Optional.present(25)
                                        )
                                    )
                                )
                            ).execute()
                            val performers = results.data?.findPerformers?.performers?.map {
                                it.performerData
                            }
                            if (performers != null) {
                                performerAdapter.addAll(0, performers)
                            }
                        }

                        viewLifecycleOwner.lifecycleScope.launch {
                            val results = apolloClient.query(
                                FindStudiosQuery(
                                    filter = Optional.present(
                                        FindFilterType(
                                            sort = Optional.present("created_at"),
                                            direction = Optional.present(SortDirectionEnum.DESC),
                                            per_page = Optional.present(25)
                                        )
                                    )
                                )
                            ).execute()
                            val studios = results.data?.findStudios?.studios?.map {
                                it.studioData
                            }
                            if (studios != null) {
                                studioAdapter.addAll(0, studios)
                            }
                        }

                    } catch (exception: ApolloException) {
                        Toast.makeText(context, exception.toString(), Toast.LENGTH_LONG).show()
                    }
                }
            } else {
                Toast.makeText(context, "Stash URL not set!", Toast.LENGTH_LONG).show()
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
        mBackgroundManager.attach(activity!!.window)
        mDefaultBackground = ContextCompat.getDrawable(activity!!, R.drawable.default_background)
        mMetrics = DisplayMetrics()
        activity!!.windowManager.defaultDisplay.getMetrics(mMetrics)
    }

    private fun setupUIElements() {
        title = getString(R.string.browse_title)
        // over title
        isHeadersTransitionOnBackEnabled = true

        // set fastLane (or headers) background color
        brandColor = ContextCompat.getColor(activity!!, R.color.fastlane_background)
        // set search icon color
        searchAffordanceColor = ContextCompat.getColor(activity!!, R.color.search_opaque)
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
        Glide.with(activity!!)
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