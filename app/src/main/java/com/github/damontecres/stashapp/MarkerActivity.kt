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
import com.github.damontecres.stashapp.data.Scene
import com.github.damontecres.stashapp.presenters.ActionPresenter
import com.github.damontecres.stashapp.presenters.ScenePresenter
import com.github.damontecres.stashapp.presenters.StashPresenter
import com.github.damontecres.stashapp.presenters.TagPresenter
import com.github.damontecres.stashapp.util.ListRowManager
import com.github.damontecres.stashapp.util.MutationEngine
import com.github.damontecres.stashapp.util.QueryEngine
import com.github.damontecres.stashapp.util.StashCoroutineExceptionHandler
import com.github.damontecres.stashapp.util.StashGlide
import com.github.damontecres.stashapp.util.convertDpToPixel
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
            val marker = intent.getParcelableExtra<Marker>("marker")!!
            supportFragmentManager.beginTransaction()
                .replace(R.id.details_fragment, MarkerDetailsFragment(marker))
                .commitNow()
        }
    }

    class MarkerDetailsFragment(private var marker: Marker) : DetailsSupportFragment() {
        companion object {
            private const val TAG = "MarkerDetailsFragment"

            private const val REPLACE_PRIMARY_ID = 10012L

            private const val DETAILS_POS = 1
            private const val PRIMARY_TAG_POS = DETAILS_POS + 1
            private const val TAG_POS = PRIMARY_TAG_POS + 1
            private const val ACTIONS_POS = TAG_POS + 1
        }

        private val mPresenterSelector = ClassPresenterSelector().addClassPresenter(ListRow::class.java, ListRowPresenter())
        private val mAdapter = SparseArrayObjectAdapter(mPresenterSelector)

        private val primaryTagRowManager =
            ListRowManager<TagData>(
                DataType.TAG,
                ListRowManager.SparseArrayRowModifier(mAdapter, PRIMARY_TAG_POS),
                ArrayObjectAdapter(TagPresenter(PrimaryTagLongClickListener())),
            ) { tagIds ->
                // Kind of hacky
                val result = mutationEngine.setTagsOnMarker(marker.id, tagIds[1], marker.tagIds)
                marker = marker.copy(primaryTagId = tagIds[1])
                listOfNotNull(result?.primary_tag?.tagData)
            }

        private val tagsRowManager =
            ListRowManager<TagData>(
                DataType.TAG,
                ListRowManager.SparseArrayRowModifier(mAdapter, TAG_POS),
                ArrayObjectAdapter(TagPresenter(TagLongClickListener())),
            ) { tagIds ->
                val result = mutationEngine.setTagsOnMarker(marker.id, marker.primaryTagId, tagIds)
                marker = marker.copy(tagIds = tagIds)
                result?.tags?.map { it.tagData }.orEmpty()
            }

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

            primaryTagRowManager.name = getString(R.string.stashapp_primary_tag)

            val lock = ReentrantReadWriteLock()
            queryEngine = QueryEngine(requireContext(), lock = lock)
            mutationEngine = MutationEngine(requireContext(), lock = lock)

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

            sceneActionsAdapter.set(1, StashAction.ADD_TAG)
            mAdapter.set(ACTIONS_POS, ListRow(HeaderItem("Actions"), sceneActionsAdapter))

            viewLifecycleOwner.lifecycleScope.launch(StashCoroutineExceptionHandler()) {
                val sceneData = queryEngine.getScene(marker.sceneId)!!
                val markerData = sceneData.scene_markers.first { it.id == marker.id }

                primaryTagRowManager.setItems(listOf(markerData.primary_tag.tagData))
                tagsRowManager.setItems(markerData.tags.map { it.tagData })

                detailsPresenter.onActionClickedListener =
                    OnActionClickedListener { _ ->
                        val intent = Intent(requireActivity(), PlaybackActivity::class.java)
                        intent.putExtra(
                            VideoDetailsActivity.MOVIE,
                            Scene.fromFullSceneData(sceneData),
                        )
                        intent.putExtra(
                            VideoDetailsFragment.POSITION_ARG,
                            (marker.seconds * 1000).toLong(),
                        )

                        requireContext().startActivity(intent)
                    }
            }
        }

        private fun setupDetailsOverviewRowPresenter() {
            // Set detail background.
            detailsPresenter.backgroundColor =
                ContextCompat.getColor(requireActivity(), R.color.default_card_background)

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
                        val tagId = data.getStringExtra(SearchForFragment.RESULT_ID_KEY)!!
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
                            val newTagData = tagsRowManager.add(tagId)
                            if (newTagData != null) {
                                Toast.makeText(
                                    requireContext(),
                                    "Added tag '${newTagData.name}' to marker",
                                    Toast.LENGTH_SHORT,
                                ).show()
                            }
                        }
                    } else if (id == REPLACE_PRIMARY_ID) {
                        val tagId = data.getStringExtra(SearchForFragment.RESULT_ID_KEY)
                        Log.d(TAG, "Setting primary tag to $tagId to scene marker ${marker.id}")
                        if (tagId != null) {
                            viewLifecycleOwner.lifecycleScope.launch(
                                StashCoroutineExceptionHandler { ex ->
                                    Toast.makeText(
                                        requireContext(),
                                        "Failed to set primary tag: ${ex.message}",
                                        Toast.LENGTH_LONG,
                                    )
                                },
                            ) {
                                val newPrimaryTag = primaryTagRowManager.add(tagId)
                                if (newPrimaryTag != null) {
                                    Toast.makeText(
                                        requireContext(),
                                        "Changed primary tag to '${newPrimaryTag.name}'",
                                        Toast.LENGTH_LONG,
                                    ).show()
                                }
                            }
                        }
                    }
                }
            }
        }

        private inner class PrimaryTagLongClickListener :
            TagPresenter.DefaultTagLongClickCallBack() {
            override fun getPopUpItems(
                context: Context,
                item: TagData,
            ): List<StashPresenter.PopUpItem> {
                val items = super.getPopUpItems(context, item).toMutableList()
                items.add(StashPresenter.PopUpItem(REPLACE_PRIMARY_ID, "Replace"))
                return items
            }

            override fun onItemLongClick(
                context: Context,
                item: TagData,
                popUpItem: StashPresenter.PopUpItem,
            ) {
                when (popUpItem.id) {
                    REPLACE_PRIMARY_ID -> {
                        // Replace
                        val intent = Intent(requireActivity(), SearchForActivity::class.java)
                        val dataType = DataType.TAG
                        intent.putExtra("dataType", dataType.name)
                        intent.putExtra(SearchForFragment.ID_KEY, REPLACE_PRIMARY_ID)
                        resultLauncher.launch(intent)
                    }

                    else -> {
                        super.onItemLongClick(context, item, popUpItem)
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
                            if (tagsRowManager.remove(item)) {
                                Toast.makeText(
                                    requireContext(),
                                    "Removed tag '${item.name}' from scene",
                                    Toast.LENGTH_SHORT,
                                ).show()
                            }
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
