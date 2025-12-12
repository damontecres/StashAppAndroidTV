package com.github.damontecres.stashapp

import android.annotation.SuppressLint
import android.graphics.Bitmap
import android.graphics.drawable.Drawable
import android.os.Bundle
import android.util.Log
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Toast
import androidx.core.content.ContextCompat
import androidx.fragment.app.activityViewModels
import androidx.fragment.app.setFragmentResultListener
import androidx.fragment.app.viewModels
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
import androidx.preference.PreferenceManager
import com.bumptech.glide.request.target.CustomTarget
import com.bumptech.glide.request.transition.Transition
import com.github.damontecres.stashapp.actions.StashAction
import com.github.damontecres.stashapp.actions.StashActionClickedListener
import com.github.damontecres.stashapp.api.fragment.FullMarkerData
import com.github.damontecres.stashapp.api.fragment.TagData
import com.github.damontecres.stashapp.data.DataType
import com.github.damontecres.stashapp.data.OCounter
import com.github.damontecres.stashapp.navigation.Destination
import com.github.damontecres.stashapp.navigation.NavigationOnItemViewClickedListener
import com.github.damontecres.stashapp.playback.PlaybackMode
import com.github.damontecres.stashapp.presenters.ActionPresenter
import com.github.damontecres.stashapp.presenters.ScenePresenter
import com.github.damontecres.stashapp.presenters.StashPresenter
import com.github.damontecres.stashapp.presenters.TagPresenter
import com.github.damontecres.stashapp.util.ListRowManager
import com.github.damontecres.stashapp.util.MutationEngine
import com.github.damontecres.stashapp.util.StashCoroutineExceptionHandler
import com.github.damontecres.stashapp.util.StashGlide
import com.github.damontecres.stashapp.util.StashServer
import com.github.damontecres.stashapp.util.convertDpToPixel
import com.github.damontecres.stashapp.util.getDataType
import com.github.damontecres.stashapp.util.getDestination
import com.github.damontecres.stashapp.util.isNotNullOrBlank
import com.github.damontecres.stashapp.util.readOnlyModeDisabled
import com.github.damontecres.stashapp.views.ClassOnItemViewClickedListener
import com.github.damontecres.stashapp.views.StashRatingBar
import com.github.damontecres.stashapp.views.durationToString
import com.github.damontecres.stashapp.views.models.MarkerDetailsViewModel
import com.github.damontecres.stashapp.views.models.ServerViewModel
import com.github.damontecres.stashapp.views.parseTimeToString
import kotlinx.coroutines.launch
import kotlin.time.DurationUnit
import kotlin.time.toDuration

class MarkerDetailsFragment : DetailsSupportFragment() {
    companion object {
        private const val TAG = "MarkerDetailsFragment"

        private const val REPLACE_PRIMARY_ID = 10012L

        private const val DETAILS_POS = 1
        private const val PRIMARY_TAG_POS = DETAILS_POS + 1
        private const val TAG_POS = PRIMARY_TAG_POS + 1
        private const val ACTIONS_POS = TAG_POS + 1
    }

    private val serverViewModel by activityViewModels<ServerViewModel>()
    private val viewModel by viewModels<MarkerDetailsViewModel>()

    private val mDetailsBackground = DetailsSupportFragmentBackgroundController(this)
    private val mAdapter = SparseArrayObjectAdapter()

    private val primaryTagPresenter =
        TagPresenter()
            .addLongCLickAction(
                StashPresenter.PopUpItem(
                    REPLACE_PRIMARY_ID,
                    R.string.replace,
                ),
                { readOnlyModeDisabled() },
            ) { _, _ ->
                serverViewModel.navigationManager.navigate(
                    Destination.SearchFor(
                        MarkerDetailsFragment::class.simpleName!!,
                        REPLACE_PRIMARY_ID,
                        DataType.TAG,
                    ),
                )
            }
    private val primaryTagRowManager =
        ListRowManager<TagData>(
            DataType.TAG,
            ListRowManager.SparseArrayRowModifier(mAdapter, PRIMARY_TAG_POS),
            ArrayObjectAdapter(primaryTagPresenter),
        ) { tagIds ->
            // Kind of hacky
            val marker = viewModel.item.value!!
            val result =
                mutationEngine.setTagsOnMarker(
                    marker.id,
                    tagIds[1],
                    marker.tags.map { it.tagData.id },
                )
            viewModel.setMarker(result!!)
            listOfNotNull(result.primary_tag.tagData)
        }

    private val tagsRowManager =
        ListRowManager<TagData>(
            DataType.TAG,
            ListRowManager.SparseArrayRowModifier(mAdapter, TAG_POS),
            ArrayObjectAdapter(TagPresenter()),
        ) { tagIds ->
            val marker = viewModel.item.value!!
            val result =
                mutationEngine.setTagsOnMarker(marker.id, marker.primary_tag.tagData.id, tagIds)
            viewModel.setMarker(result!!)
            result.tags.map { it.tagData }
        }

    private val sceneActionsAdapter =
        SparseArrayObjectAdapter(
            ClassPresenterSelector().addClassPresenter(
                StashAction::class.java,
                ActionPresenter(),
            ),
        )

    private lateinit var mutationEngine: MutationEngine

    private lateinit var detailsPresenter: FullWidthDetailsOverviewRowPresenter

    private val actionClickListener =
        object : StashActionClickedListener {
            override fun onClicked(action: StashAction) {
                if (action == StashAction.ADD_TAG) {
                    serverViewModel.navigationManager.navigate(
                        Destination.SearchFor(
                            MarkerDetailsFragment::class.simpleName!!,
                            StashAction.ADD_TAG.id,
                            DataType.TAG,
                        ),
                    )
                } else if (action == StashAction.SHIFT_MARKERS) {
                    serverViewModel.navigationManager.navigate(
                        Destination.UpdateMarker(viewModel.item.value!!.id),
                    )
                }
            }

            override fun incrementOCounter(counter: OCounter): Unit = throw IllegalStateException()
        }

    @Deprecated("Deprecated in Java")
    override fun inflateTitle(
        inflater: LayoutInflater?,
        parent: ViewGroup?,
        savedInstanceState: Bundle?,
    ): View? = null

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        primaryTagRowManager.name = getString(R.string.stashapp_primary_tag)
        mutationEngine = MutationEngine(StashServer.requireCurrentServer())

        setFragmentResultListener(MarkerDetailsFragment::class.simpleName!!) { _, bundle ->
            val sourceId = bundle.getLong(SearchForFragment.RESULT_ID_KEY)
            val dataType = bundle.getDataType()
            val newId = bundle.getString(SearchForFragment.RESULT_ITEM_ID_KEY)
            Log.v(TAG, "Adding $dataType: $newId")
            if (newId != null) {
                viewLifecycleOwner.lifecycleScope.launch(
                    StashCoroutineExceptionHandler { ex ->
                        Toast.makeText(
                            requireContext(),
                            "Failed to update: ${ex.message}",
                            Toast.LENGTH_LONG,
                        )
                    },
                ) {
                    when (sourceId) {
                        REPLACE_PRIMARY_ID -> {
                            val newPrimaryTag = primaryTagRowManager.add(newId)
                            if (newPrimaryTag != null) {
                                Toast
                                    .makeText(
                                        requireContext(),
                                        "Changed primary tag to '${newPrimaryTag.name}'",
                                        Toast.LENGTH_LONG,
                                    ).show()
                            }
                        }

                        StashAction.ADD_TAG.id -> {
                            val newTagData = tagsRowManager.add(newId)
                            if (newTagData != null) {
                                Toast
                                    .makeText(
                                        requireContext(),
                                        "Added tag '${newTagData.name}' to marker",
                                        Toast.LENGTH_SHORT,
                                    ).show()
                            }
                        }

                        else -> {
                            throw IllegalArgumentException("Unknown action id $sourceId")
                        }
                    }
                }
            }
        }
    }

    override fun onResume() {
        super.onResume()
        val markerDetailsDest = requireArguments().getDestination<Destination.MarkerDetails>()
        viewModel.init(serverViewModel.requireServer(), markerDetailsDest.markerId)
    }

    override fun onViewCreated(
        view: View,
        savedInstanceState: Bundle?,
    ) {
        super.onViewCreated(view, savedInstanceState)
        setupDetailsOverviewRowPresenter()
        adapter = mAdapter

        onItemViewClickedListener =
            ClassOnItemViewClickedListener(NavigationOnItemViewClickedListener(serverViewModel.navigationManager))
                .addListenerForClass(StashAction::class.java) { item ->
                    actionClickListener.onClicked(item)
                }.addListenerForClass(Action::class.java) { _ ->
                    // no-op, detailsPresenter.onActionClickedListener will handle
                }

        viewModel.item.observe(viewLifecycleOwner) { marker ->
            if (marker == null) {
                Toast.makeText(requireContext(), "Marker not found", Toast.LENGTH_LONG).show()
                serverViewModel.navigationManager.goBack()
                return@observe
            }
            val sceneId = marker.scene.videoSceneData.id

            initializeBackground(marker.screenshot)
            setupDetailsOverviewRow(marker)

            primaryTagRowManager.setItems(listOf(marker.primary_tag.tagData))
            tagsRowManager.setItems(marker.tags.map { it.tagData })

            detailsPresenter.onActionClickedListener =
                OnActionClickedListener { _ ->
                    serverViewModel.navigationManager.navigate(
                        Destination.Playback(
                            sceneId,
                            (viewModel.seconds.value!! * 1000).toLong(),
                            PlaybackMode.Choose,
                        ),
                    )
                }

            sceneActionsAdapter.set(1, StashAction.ADD_TAG)
            sceneActionsAdapter.set(2, StashAction.SHIFT_MARKERS)

            if (readOnlyModeDisabled()) {
                mAdapter.set(
                    ACTIONS_POS,
                    ListRow(
                        HeaderItem(getString(R.string.stashapp_actions_name)),
                        sceneActionsAdapter,
                    ),
                )
            }
        }
    }

    private fun setupDetailsOverviewRowPresenter() {
        detailsPresenter =
            FullWidthDetailsOverviewRowPresenter(
                object : AbstractDetailsDescriptionPresenter() {
                    @SuppressLint("SetTextI18n")
                    override fun onBindDescription(
                        vh: ViewHolder,
                        item: Any,
                    ) {
                        item as FullMarkerData
                        val ratingBar = vh.view.findViewById<StashRatingBar>(R.id.rating_bar)
                        ratingBar.visibility = View.GONE

                        vh.title.text =
                            if (item.title.isNotNullOrBlank()) {
                                item.title
                            } else {
                                item.primary_tag.tagData.name
                            }
                        val body = mutableListOf<String>()
                        if (item.end_seconds != null) {
                            val endTime =
                                item.end_seconds
                                    .toDuration(
                                        DurationUnit.SECONDS,
                                    )
                            body.add(
                                "${getString(R.string.stashapp_time_end)}: $endTime",
                            )
                            val duration = endTime - item.seconds.toDuration(DurationUnit.SECONDS)
                            body.add(
                                "${getString(R.string.stashapp_duration)}: $duration",
                            )
                        }
                        if (PreferenceManager
                                .getDefaultSharedPreferences(requireContext())
                                .getBoolean(
                                    getString(R.string.pref_key_show_playback_debug_info),
                                    false,
                                )
                        ) {
                            if (body.isNotEmpty()) {
                                body.add("\n")
                            }
                            body.add("${getString(R.string.id)}: ${item.id}")
                        }
                        val createdAt =
                            getString(R.string.stashapp_created_at) + ": " +
                                parseTimeToString(
                                    item.created_at,
                                )
                        val updatedAt =
                            getString(R.string.stashapp_updated_at) + ": " +
                                parseTimeToString(
                                    item.updated_at,
                                )
                        body.add(createdAt)
                        body.add(updatedAt)
                        vh.body.text = body.joinToString("\n")
                    }
                },
            )
        // Set detail background.
        detailsPresenter.backgroundColor =
            ContextCompat.getColor(requireActivity(), R.color.default_card_background)

        val presenterSelector =
            ClassPresenterSelector()
                .addClassPresenter(ListRow::class.java, ListRowPresenter())
                .addClassPresenter(DetailsOverviewRow::class.java, detailsPresenter)
        mAdapter.presenterSelector = presenterSelector
    }

    private fun setupDetailsOverviewRow(marker: FullMarkerData) {
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
            StashGlide
                .with(requireActivity(), screenshotUrl)
                .optionalCenterCrop()
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

        viewModel.seconds.observe(viewLifecycleOwner) { newSeconds ->
            Log.v(TAG, "Marker newSeconds=$newSeconds")
            val playActionsAdapter = ArrayObjectAdapter()
            playActionsAdapter.add(
                Action(
                    1L,
                    resources.getString(R.string.play_scene),
                    durationToString(newSeconds),
                ),
            )
            row.actionsAdapter = playActionsAdapter
        }
        if (!viewModel.seconds.isInitialized) {
            viewModel.seconds.value = marker.seconds
        }

        mAdapter.set(1, row)
    }

    private fun initializeBackground(screenshotUrl: String?) {
        if (mDetailsBackground.coverBitmap == null) {
            mDetailsBackground.enableParallax()

            if (screenshotUrl.isNotNullOrBlank()) {
                StashGlide
                    .withBitmap(requireActivity(), screenshotUrl)
                    .optionalCenterCrop()
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
