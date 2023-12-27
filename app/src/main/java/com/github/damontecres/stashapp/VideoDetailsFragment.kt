package com.github.damontecres.stashapp

import android.content.Context
import android.content.Intent
import android.graphics.Bitmap
import android.os.Bundle
import android.graphics.drawable.Drawable
import androidx.leanback.app.DetailsSupportFragment
import androidx.leanback.app.DetailsSupportFragmentBackgroundController
import androidx.leanback.widget.Action
import androidx.leanback.widget.ArrayObjectAdapter
import androidx.leanback.widget.ClassPresenterSelector
import androidx.leanback.widget.DetailsOverviewRow
import androidx.leanback.widget.FullWidthDetailsOverviewRowPresenter
import androidx.leanback.widget.FullWidthDetailsOverviewSharedElementHelper
import androidx.leanback.widget.HeaderItem
import androidx.leanback.widget.ImageCardView
import androidx.leanback.widget.ListRow
import androidx.leanback.widget.ListRowPresenter
import androidx.leanback.widget.OnActionClickedListener
import androidx.leanback.widget.OnItemViewClickedListener
import androidx.leanback.widget.Presenter
import androidx.leanback.widget.Row
import androidx.leanback.widget.RowPresenter
import androidx.core.app.ActivityOptionsCompat
import androidx.core.content.ContextCompat
import android.util.Log
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Toast
import androidx.lifecycle.lifecycleScope
import androidx.preference.PreferenceManager
import com.apollographql.apollo3.api.Optional

import com.bumptech.glide.Glide
import com.bumptech.glide.load.model.GlideUrl
import com.bumptech.glide.load.model.LazyHeaders
import com.bumptech.glide.request.target.SimpleTarget
import com.bumptech.glide.request.transition.Transition
import com.github.damontecres.stashapp.api.FindPerformersQuery
import com.github.damontecres.stashapp.api.FindScenesQuery
import com.github.damontecres.stashapp.api.fragment.SlimSceneData
import com.github.damontecres.stashapp.api.type.FindFilterType
import com.github.damontecres.stashapp.api.type.SortDirectionEnum
import com.github.damontecres.stashapp.data.Scene
import com.github.damontecres.stashapp.data.sceneFromSlimSceneData
import kotlinx.coroutines.launch

/**
 * A wrapper fragment for leanback details screens.
 * It shows a detailed view of video and its metadata plus related videos.
 */
class VideoDetailsFragment : DetailsSupportFragment() {

    private var mSelectedMovie: Scene? = null

    private var performersAdapter: ArrayObjectAdapter = ArrayObjectAdapter(PerformerPresenter())

    private lateinit var mDetailsBackground: DetailsSupportFragmentBackgroundController
    private lateinit var mPresenterSelector: ClassPresenterSelector
    private lateinit var mAdapter: ArrayObjectAdapter

    override fun onCreate(savedInstanceState: Bundle?) {
        Log.d(TAG, "onCreate DetailsFragment")
        super.onCreate(savedInstanceState)

        mDetailsBackground = DetailsSupportFragmentBackgroundController(this)

//        mSelectedMovie = activity!!.intent.getSerializableExtra(DetailsActivity.MOVIE) as Scene
        mSelectedMovie = activity!!.intent.getParcelableExtra(DetailsActivity.MOVIE)
        if (mSelectedMovie != null) {
            mPresenterSelector = ClassPresenterSelector()
            mAdapter = ArrayObjectAdapter(mPresenterSelector)
            setupDetailsOverviewRow()
            setupDetailsOverviewRowPresenter()
            setupRelatedMovieListRow()
            adapter = mAdapter
            initializeBackground(mSelectedMovie)
            onItemViewClickedListener = ItemViewClickedListener()



        } else {
            val intent = Intent(activity!!, MainActivity::class.java)
            startActivity(intent)
        }
    }

    override fun onStart() {
        super.onStart()
        if(performersAdapter.size()==0) {
            // TODO: this is not efficient, but it does prevent duplicates during navigation
            viewLifecycleOwner.lifecycleScope.launch {
                val apolloClient = createApolloClient(requireContext())
                if (apolloClient != null && mSelectedMovie != null) {
                    val results = apolloClient.query(
                        FindScenesQuery(scene_ids = Optional.present(listOf(mSelectedMovie!!.id.toInt())))
                    ).execute()

                    val performerIds = results.data?.findScenes?.scenes?.map {
                        it.slimSceneData.performers.map { it.id.toInt() }
                    }?.flatten()
                    val performers = apolloClient.query(
                        FindPerformersQuery(
                            performer_ids = Optional.present(performerIds)
                        )
                    ).execute()
                    val perfs = performers.data?.findPerformers?.performers?.map {
                        it.performerData
                    }
                    if (perfs != null) {
                        performersAdapter.addAll(0, perfs)
                    }
                }
            }
        }
    }

    private fun initializeBackground(movie: Scene?) {
        mDetailsBackground.enableParallax()

        val screenshotUrl = movie?.screenshotUrl

        if(!screenshotUrl.isNullOrBlank()) {
            val apiKey = PreferenceManager.getDefaultSharedPreferences(requireContext())
                .getString("stashApiKey", "")
            val url = createGlideUrl(screenshotUrl, apiKey)

            Glide.with(requireActivity())
                .asBitmap()
                .centerCrop()
                .error(R.drawable.default_background)
                .load(url)
                .into<SimpleTarget<Bitmap>>(object : SimpleTarget<Bitmap>() {
                    override fun onResourceReady(
                        bitmap: Bitmap,
                        transition: Transition<in Bitmap>?
                    ) {
                        mDetailsBackground.coverBitmap = bitmap
                        mAdapter.notifyArrayItemRangeChanged(0, mAdapter.size())
                    }
                })
        }
    }

    private fun setupDetailsOverviewRow() {
        Log.d(TAG, "doInBackground: " + mSelectedMovie?.toString())
        val row = DetailsOverviewRow(mSelectedMovie)
        row.imageDrawable = ContextCompat.getDrawable(activity!!, R.drawable.default_background)
        val width = convertDpToPixel(activity!!, DETAIL_THUMB_WIDTH)
        val height = convertDpToPixel(activity!!, DETAIL_THUMB_HEIGHT)

        val screenshotUrl = mSelectedMovie?.screenshotUrl
        if(!screenshotUrl.isNullOrBlank()) {
            val apiKey = PreferenceManager.getDefaultSharedPreferences(requireContext())
                .getString("stashApiKey", "")
            val url = createGlideUrl(screenshotUrl, apiKey)
            Glide.with(activity!!)
                .load(url)
                .centerCrop()
                .error(R.drawable.default_background)
                .into<SimpleTarget<Drawable>>(object : SimpleTarget<Drawable>(width, height) {
                    override fun onResourceReady(
                        drawable: Drawable,
                        transition: Transition<in Drawable>?
                    ) {
                        Log.d(TAG, "details overview card image url ready: " + drawable)
                        row.imageDrawable = drawable
                        mAdapter.notifyArrayItemRangeChanged(0, mAdapter.size())
                    }
                })
        }

        val actionAdapter = ArrayObjectAdapter()

        actionAdapter.add(
                Action(
                        ACTION_PLAY_SCENE,
                        resources.getString(R.string.play_scene)))
        row.actionsAdapter = actionAdapter

        mAdapter.add(row)
    }

    private fun setupDetailsOverviewRowPresenter() {
        // Set detail background.
        val detailsPresenter = FullWidthDetailsOverviewRowPresenter(DetailsDescriptionPresenter())
        detailsPresenter.backgroundColor =
                ContextCompat.getColor(activity!!, R.color.selected_background)

        // Hook up transition element.
        val sharedElementHelper = FullWidthDetailsOverviewSharedElementHelper()
        sharedElementHelper.setSharedElementEnterTransition(
                activity, DetailsActivity.SHARED_ELEMENT_NAME)
        detailsPresenter.setListener(sharedElementHelper)
        detailsPresenter.isParticipatingEntranceTransition = true

        detailsPresenter.onActionClickedListener = OnActionClickedListener { action ->
            if (action.id == ACTION_PLAY_SCENE && mSelectedMovie!=null) {
                val intent = Intent(activity!!, PlaybackActivity::class.java)
                intent.putExtra(DetailsActivity.MOVIE, mSelectedMovie)
                startActivity(intent)
            } else {
                Toast.makeText(activity!!, action.toString(), Toast.LENGTH_SHORT).show()
            }
        }
        mPresenterSelector.addClassPresenter(DetailsOverviewRow::class.java, detailsPresenter)
    }

    private fun setupRelatedMovieListRow() {
        val subcategories = arrayOf(getString(R.string.related_movies))
        val listRowAdapter = ArrayObjectAdapter(ScenePresenter())
        // TODO related scenes

        mAdapter.add(ListRow(HeaderItem(0, "Performers"), performersAdapter))
        mAdapter.add(ListRow(HeaderItem(1, subcategories[0]), listRowAdapter))
        mPresenterSelector.addClassPresenter(ListRow::class.java, ListRowPresenter())
    }

    private fun convertDpToPixel(context: Context, dp: Int): Int {
        val density = context.applicationContext.resources.displayMetrics.density
        return Math.round(dp.toFloat() * density)
    }

    private inner class ItemViewClickedListener : OnItemViewClickedListener {
        override fun onItemClicked(
                itemViewHolder: Presenter.ViewHolder?,
                item: Any?,
                rowViewHolder: RowPresenter.ViewHolder,
                row: Row) {
            if (item is SlimSceneData) {
                Log.d(TAG, "Item: " + item.toString())
                val intent = Intent(activity!!, DetailsActivity::class.java)
                intent.putExtra(resources.getString(R.string.movie), sceneFromSlimSceneData(item))

                val bundle =
                        ActivityOptionsCompat.makeSceneTransitionAnimation(
                                activity!!,
                                (itemViewHolder?.view as ImageCardView).mainImageView,
                                DetailsActivity.SHARED_ELEMENT_NAME)
                                .toBundle()
                startActivity(intent, bundle)
            }
        }
    }

    companion object {
        private val TAG = "VideoDetailsFragment"

        private val ACTION_PLAY_SCENE = 1L
        private val ACTION_RENT = 2L
        private val ACTION_BUY = 3L

        private val DETAIL_THUMB_WIDTH = 274
        private val DETAIL_THUMB_HEIGHT = 274

        private val NUM_COLS = 10
    }
}