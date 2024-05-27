package com.github.damontecres.stashapp

import android.app.Activity
import android.content.Context
import android.content.Intent
import android.graphics.Bitmap
import android.graphics.drawable.Drawable
import android.os.Bundle
import android.util.Log
import android.view.View
import android.widget.Toast
import androidx.activity.result.ActivityResult
import androidx.activity.result.ActivityResultCallback
import androidx.activity.result.ActivityResultLauncher
import androidx.activity.result.contract.ActivityResultContracts
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
import com.github.damontecres.stashapp.actions.StashAction
import com.github.damontecres.stashapp.actions.StashActionClickedListener
import com.github.damontecres.stashapp.api.fragment.TagData
import com.github.damontecres.stashapp.data.DataType
import com.github.damontecres.stashapp.data.Marker
import com.github.damontecres.stashapp.data.OCounter
import com.github.damontecres.stashapp.presenters.ActionPresenter
import com.github.damontecres.stashapp.presenters.ScenePresenter
import com.github.damontecres.stashapp.presenters.StashPresenter
import com.github.damontecres.stashapp.presenters.TagPresenter
import com.github.damontecres.stashapp.util.MutationEngine
import com.github.damontecres.stashapp.util.QueryEngine
import com.github.damontecres.stashapp.util.StashCoroutineExceptionHandler
import com.github.damontecres.stashapp.util.StashGlide
import com.github.damontecres.stashapp.util.TagDiffCallback
import com.github.damontecres.stashapp.util.convertDpToPixel
import com.github.damontecres.stashapp.util.isNotEmpty
import com.github.damontecres.stashapp.util.isNotNullOrBlank
import com.github.damontecres.stashapp.views.StashItemViewClickListener
import com.github.damontecres.stashapp.views.StashRatingBar
import com.github.damontecres.stashapp.views.durationToString
import kotlinx.coroutines.CoroutineExceptionHandler
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
        companion object {
            private const val TAG = "MarkerDetailsFragment"

            private const val DETAILS_POS = 1
            private const val PRIMARY_TAG_POS = DETAILS_POS + 1
            private const val TAG_POS = PRIMARY_TAG_POS + 1
            private const val ACTIONS_POS = TAG_POS + 1
        }

        private val mPresenterSelector = ClassPresenterSelector().addClassPresenter(ListRow::class.java, ListRowPresenter())
        private val mAdapter = SparseArrayObjectAdapter(mPresenterSelector)
        private val primaryTagAdapter = ArrayObjectAdapter(TagPresenter())
        private val tagsAdapter = ArrayObjectAdapter(TagPresenter(TagLongClickListener()))
        private val sceneActionsAdapter =
            SparseArrayObjectAdapter(
                ClassPresenterSelector().addClassPresenter(
                    StashAction::class.java,
                    ActionPresenter(),
                ),
            )

        private lateinit var queryEngine: QueryEngine
        private lateinit var mutationEngine: MutationEngine
        private lateinit var resultLauncher: ActivityResultLauncher<Intent>

        private lateinit var marker: Marker

        private lateinit var detailsPresenter: FullWidthDetailsOverviewRowPresenter

        private val actionClickListener =
            object : StashActionClickedListener {
                override fun onClicked(action: StashAction) {
                    if (action == StashAction.ADD_TAG) {
                        val intent = Intent(requireActivity(), SearchForActivity::class.java)
                        val dataType = DataType.TAG
                        intent.putExtra("dataType", dataType.name)
                        intent.putExtra(SearchForFragment.ID_KEY, action.id)
                        resultLauncher.launch(intent)
                    }
                }

                override fun incrementOCounter(counter: OCounter) {
                    throw IllegalStateException()
                }
            }

        override fun onCreate(savedInstanceState: Bundle?) {
            super.onCreate(savedInstanceState)

            val lock = ReentrantReadWriteLock()
            queryEngine = QueryEngine(requireContext(), lock = lock)
            mutationEngine = MutationEngine(requireContext(), lock = lock)

            marker = requireActivity().intent.getParcelableExtra("marker")!!

            resultLauncher =
                registerForActivityResult(
                    ActivityResultContracts.StartActivityForResult(),
                    ResultCallback(),
                )

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

            onItemViewClickedListener =
                StashItemViewClickListener(requireContext(), actionClickListener)

            mAdapter.set(
                PRIMARY_TAG_POS,
                ListRow(HeaderItem(getString(R.string.stashapp_primary_tag)), primaryTagAdapter),
            )

            sceneActionsAdapter.set(1, StashAction.ADD_TAG)
            mAdapter.set(ACTIONS_POS, ListRow(HeaderItem("Actions"), sceneActionsAdapter))

            viewLifecycleOwner.lifecycleScope.launch(StashCoroutineExceptionHandler()) {
                val tags =
                    queryEngine.getTags(listOf(marker.primaryTagId, *marker.tagIds.toTypedArray()))
                primaryTagAdapter.add(tags.first { it.id == marker.primaryTagId })
                tagsAdapter.addAll(0, tags.filterNot { it.id == marker.primaryTagId })
                if (tagsAdapter.isNotEmpty()) {
                    mAdapter.set(
                        TAG_POS,
                        ListRow(HeaderItem(getString(R.string.stashapp_tags)), tagsAdapter),
                    )
                }
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

        private inner class ResultCallback : ActivityResultCallback<ActivityResult> {
            override fun onActivityResult(result: ActivityResult) {
                if (result.resultCode == Activity.RESULT_OK) {
                    val data: Intent? = result.data
                    val id = data!!.getLongExtra(SearchForFragment.ID_KEY, -1)
                    if (id == StashAction.ADD_TAG.id) {
                        val tagId = data.getStringExtra(SearchForFragment.RESULT_ID_KEY)
                        Log.d(TAG, "Adding tag $tagId to scene marker ${marker.id}")
                        viewLifecycleOwner.lifecycleScope.launch(
                            StashCoroutineExceptionHandler { ex ->
                                Toast.makeText(
                                    requireContext(),
                                    "Failed to add tag: ${ex.message}",
                                    Toast.LENGTH_LONG,
                                )
                            },
                        ) {
                            val tagIds =
                                tagsAdapter.unmodifiableList<TagData>().map { it.id }
                                    .toMutableSet()
                            tagIds.add(tagId!!)
                            if (tagsAdapter.size() == tagIds.size || tagId == marker.primaryTagId) {
                                // Tag is already on the marker
                                Toast.makeText(
                                    requireContext(),
                                    "Tag is already set on this marker",
                                    Toast.LENGTH_SHORT,
                                ).show()
                                return@launch
                            }
                            val mutResult =
                                MutationEngine(requireContext()).setTagsOnMarker(
                                    marker.id,
                                    tagIds.toList(),
                                )
                            val newTags = mutResult?.tags?.map { it.tagData }
                            val newTagName =
                                newTags?.firstOrNull { it.id == tagId }?.name
                            tagsAdapter.setItems(newTags, TagDiffCallback)
                            if (mAdapter.lookup(TAG_POS) == null) {
                                mAdapter.set(
                                    TAG_POS,
                                    ListRow(
                                        HeaderItem(getString(R.string.stashapp_tags)),
                                        tagsAdapter,
                                    ),
                                )
                            }

                            Toast.makeText(
                                requireContext(),
                                "Added tag '$newTagName' to marker",
                                Toast.LENGTH_SHORT,
                            ).show()
                        }
                    }
                }
            }
        }

        private inner class TagLongClickListener : TagPresenter.DefaultTagLongClickCallBack() {
            override fun getPopUpItems(
                context: Context,
                item: TagData,
            ): List<StashPresenter.PopUpItem> {
                val items = super.getPopUpItems(context, item).toMutableList()
                items.add(StashPresenter.PopUpItem(1000L, "Remove"))
                return items
            }

            override fun onItemLongClick(
                context: Context,
                item: TagData,
                popUpItem: StashPresenter.PopUpItem,
            ) {
                when (popUpItem.id) {
                    1000L -> {
                        viewLifecycleOwner.lifecycleScope.launch(
                            CoroutineExceptionHandler { _, ex ->
                                Log.e(TAG, "Exception setting tags", ex)
                                Toast.makeText(
                                    requireContext(),
                                    "Failed to remove tag: ${ex.message}",
                                    Toast.LENGTH_LONG,
                                ).show()
                            },
                        ) {
                            val tagIds =
                                tagsAdapter.unmodifiableList<TagData>()
                                    .map { it.id }
                                    .toMutableList()
                            tagIds.remove(item.id)
                            val mutResult =
                                MutationEngine(requireContext()).setTagsOnMarker(
                                    marker.id,
                                    tagIds,
                                )
                            val newTags = mutResult?.tags?.map { it.tagData }.orEmpty()
                            if (newTags.isEmpty()) {
                                mAdapter.clear(TAG_POS)
                            } else {
                                tagsAdapter.setItems(newTags, TagDiffCallback)
                            }
                            Toast.makeText(
                                requireContext(),
                                "Removed tag '${item.name}' from scene",
                                Toast.LENGTH_SHORT,
                            ).show()
                        }
                    }

                    else -> {
                        super.onItemLongClick(context, item, popUpItem)
                    }
                }
            }
        }
    }
}
