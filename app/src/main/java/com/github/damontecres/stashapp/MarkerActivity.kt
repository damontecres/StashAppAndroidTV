package com.github.damontecres.stashapp

import android.content.Intent
import android.graphics.Bitmap
import android.graphics.drawable.Drawable
import android.os.Bundle
import android.view.View
import androidx.core.content.ContextCompat
import androidx.fragment.app.FragmentActivity
import androidx.leanback.app.DetailsSupportFragment
import androidx.leanback.app.DetailsSupportFragmentBackgroundController
import androidx.leanback.widget.AbstractDetailsDescriptionPresenter
import androidx.leanback.widget.Action
import androidx.leanback.widget.ArrayObjectAdapter
import androidx.leanback.widget.ClassPresenterSelector
import androidx.leanback.widget.DetailsOverviewRow
import androidx.leanback.widget.FullWidthDetailsOverviewRowPresenter
import androidx.leanback.widget.HeaderItem
import androidx.leanback.widget.ListRow
import androidx.leanback.widget.ListRowPresenter
import androidx.leanback.widget.OnActionClickedListener
import androidx.leanback.widget.SparseArrayObjectAdapter
import androidx.lifecycle.lifecycleScope
import com.bumptech.glide.request.target.CustomTarget
import com.bumptech.glide.request.transition.Transition
import com.github.damontecres.stashapp.data.Marker
import com.github.damontecres.stashapp.presenters.ScenePresenter
import com.github.damontecres.stashapp.presenters.StashPresenter
import com.github.damontecres.stashapp.presenters.TagPresenter
import com.github.damontecres.stashapp.util.MutationEngine
import com.github.damontecres.stashapp.util.QueryEngine
import com.github.damontecres.stashapp.util.StashCoroutineExceptionHandler
import com.github.damontecres.stashapp.util.StashGlide
import com.github.damontecres.stashapp.util.convertDpToPixel
import com.github.damontecres.stashapp.util.isNotNullOrBlank
import com.github.damontecres.stashapp.views.StashRatingBar
import com.github.damontecres.stashapp.views.durationToString
import kotlinx.coroutines.launch
import java.util.concurrent.locks.ReentrantReadWriteLock

class MarkerActivity : FragmentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_details)
        if (savedInstanceState == null) {
            supportFragmentManager.beginTransaction()
                .replace(R.id.details_fragment, MarkerDetailsFragment())
                .commitNow()
        }
    }

    class MarkerDetailsFragment : DetailsSupportFragment() {
        private val mPresenterSelector = ClassPresenterSelector().addClassPresenter(ListRow::class.java, ListRowPresenter())
        private val mAdapter = SparseArrayObjectAdapter(mPresenterSelector)
        private val tagsAdapter = ArrayObjectAdapter(TagPresenter())

        private lateinit var queryEngine: QueryEngine
        private lateinit var mutationEngine: MutationEngine

        private lateinit var marker: Marker

        private lateinit var detailsPresenter: FullWidthDetailsOverviewRowPresenter

        override fun onCreate(savedInstanceState: Bundle?) {
            super.onCreate(savedInstanceState)

            val lock = ReentrantReadWriteLock()
            queryEngine = QueryEngine(requireContext(), lock = lock)
            mutationEngine = MutationEngine(requireContext(), lock = lock)

            marker = requireActivity().intent.getParcelableExtra("marker")!!

            detailsPresenter =
                FullWidthDetailsOverviewRowPresenter(
                    object : AbstractDetailsDescriptionPresenter() {
                        override fun onBindDescription(
                            vh: ViewHolder,
                            item: Any,
                        ) {
                            val ratingBar = vh.view.findViewById<StashRatingBar>(R.id.rating_bar)
                            ratingBar.visibility = View.GONE

                            vh.title.text =
                                if (marker.title.isNotNullOrBlank()) {
                                    marker.title
                                } else {
                                    marker.primaryTagName
                                }
                        }
                    },
                )
        }

        override fun onViewCreated(
            view: View,
            savedInstanceState: Bundle?,
        ) {
            super.onViewCreated(view, savedInstanceState)
            adapter = mAdapter
            initializeBackground()
            setupDetailsOverviewRowPresenter()
            setupDetailsOverviewRow()

            mAdapter.set(2, ListRow(HeaderItem(getString(R.string.stashapp_tags)), tagsAdapter))

            viewLifecycleOwner.lifecycleScope.launch(StashCoroutineExceptionHandler()) {
                val tags = queryEngine.getTags(marker.tagIds)
                tagsAdapter.addAll(0, tags)
            }
        }

        private fun setupDetailsOverviewRowPresenter() {
            // Set detail background.
            detailsPresenter.backgroundColor =
                ContextCompat.getColor(requireActivity(), R.color.default_card_background)

            detailsPresenter.onActionClickedListener =
                OnActionClickedListener { action ->
                    val intent = Intent(requireActivity(), PlaybackActivity::class.java)
                    intent.putExtra(VideoDetailsActivity.MOVIE, marker.scene)
                    intent.putExtra(VideoDetailsFragment.POSITION_ARG, (marker.seconds * 1000).toLong())

                    requireContext().startActivity(intent)
                }

            mPresenterSelector.addClassPresenter(DetailsOverviewRow::class.java, detailsPresenter)
        }

        private fun setupDetailsOverviewRow() {
            val row = DetailsOverviewRow(marker)

            val screenshotUrl = marker.screenshot
            if (screenshotUrl.isNotNullOrBlank()) {
                row.item = marker
                val width =
                    convertDpToPixel(
                        requireActivity(),
                        ScenePresenter.CARD_WIDTH,
                    )
                val height =
                    convertDpToPixel(
                        requireActivity(),
                        ScenePresenter.CARD_HEIGHT,
                    )
                StashGlide.with(requireActivity(), screenshotUrl)
                    .centerCrop()
                    .error(StashPresenter.glideError(requireContext()))
                    .into<CustomTarget<Drawable>>(
                        object : CustomTarget<Drawable>(width, height) {
                            override fun onResourceReady(
                                resource: Drawable,
                                transition: Transition<in Drawable>?,
                            ) {
                                row.imageDrawable = resource
                                mAdapter.notifyArrayItemRangeChanged(0, mAdapter.size())
                            }

                            override fun onLoadCleared(placeholder: Drawable?) {
                                row.imageDrawable = null
                            }
                        },
                    )
            }

            val playActionsAdapter = ArrayObjectAdapter()
            playActionsAdapter.add(Action(1L, resources.getString(R.string.play_scene), durationToString(marker.seconds)))
            row.actionsAdapter = playActionsAdapter

            mAdapter.set(1, row)
        }

        private fun initializeBackground() {
            val mDetailsBackground = DetailsSupportFragmentBackgroundController(this)
            if (mDetailsBackground.coverBitmap == null) {
                mDetailsBackground.enableParallax()

                val screenshotUrl = marker.screenshot

                if (screenshotUrl.isNotNullOrBlank()) {
                    StashGlide.withBitmap(requireActivity(), screenshotUrl)
                        .centerCrop()
                        .error(R.drawable.baseline_camera_indoor_48)
                        .into<CustomTarget<Bitmap>>(
                            object : CustomTarget<Bitmap>() {
                                override fun onResourceReady(
                                    bitmap: Bitmap,
                                    transition: Transition<in Bitmap>?,
                                ) {
                                    mDetailsBackground.coverBitmap = bitmap
                                    mAdapter.notifyArrayItemRangeChanged(0, mAdapter.size())
                                }

                                override fun onLoadCleared(placeholder: Drawable?) {
                                    mDetailsBackground.coverBitmap = null
                                }
                            },
                        )
                }
            }
        }
    }
}
