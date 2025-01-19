package com.github.damontecres.stashapp

import android.content.Context
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
import androidx.leanback.widget.Action
import androidx.leanback.widget.ArrayObjectAdapter
import androidx.leanback.widget.ClassPresenterSelector
import androidx.leanback.widget.DetailsOverviewRow
import androidx.leanback.widget.FullWidthDetailsOverviewRowPresenter
import androidx.leanback.widget.HeaderItem
import androidx.leanback.widget.ListRow
import androidx.leanback.widget.ListRowPresenter
import androidx.leanback.widget.OnActionClickedListener
import androidx.leanback.widget.SinglePresenterSelector
import androidx.leanback.widget.SparseArrayObjectAdapter
import androidx.lifecycle.lifecycleScope
import androidx.preference.PreferenceManager
import com.bumptech.glide.request.target.CustomTarget
import com.bumptech.glide.request.transition.Transition
import com.github.damontecres.stashapp.actions.CreateMarkerAction
import com.github.damontecres.stashapp.actions.StashAction
import com.github.damontecres.stashapp.actions.StashActionClickedListener
import com.github.damontecres.stashapp.api.fragment.FullSceneData
import com.github.damontecres.stashapp.api.fragment.GroupData
import com.github.damontecres.stashapp.api.fragment.MarkerData
import com.github.damontecres.stashapp.api.fragment.PerformerData
import com.github.damontecres.stashapp.api.fragment.StudioData
import com.github.damontecres.stashapp.api.fragment.TagData
import com.github.damontecres.stashapp.data.DataType
import com.github.damontecres.stashapp.data.OCounter
import com.github.damontecres.stashapp.navigation.Destination
import com.github.damontecres.stashapp.navigation.NavigationOnItemViewClickedListener
import com.github.damontecres.stashapp.playback.PlaybackMode
import com.github.damontecres.stashapp.presenters.ActionPresenter
import com.github.damontecres.stashapp.presenters.CreateMarkerActionPresenter
import com.github.damontecres.stashapp.presenters.GalleryPresenter
import com.github.damontecres.stashapp.presenters.GroupPresenter
import com.github.damontecres.stashapp.presenters.MarkerPresenter
import com.github.damontecres.stashapp.presenters.OCounterPresenter
import com.github.damontecres.stashapp.presenters.PerformerInScenePresenter
import com.github.damontecres.stashapp.presenters.SceneDetailsPresenter
import com.github.damontecres.stashapp.presenters.ScenePresenter
import com.github.damontecres.stashapp.presenters.StashPresenter
import com.github.damontecres.stashapp.presenters.StudioPresenter
import com.github.damontecres.stashapp.presenters.TagPresenter
import com.github.damontecres.stashapp.util.Constants
import com.github.damontecres.stashapp.util.ListRowManager
import com.github.damontecres.stashapp.util.MutationEngine
import com.github.damontecres.stashapp.util.QueryEngine
import com.github.damontecres.stashapp.util.StashCoroutineExceptionHandler
import com.github.damontecres.stashapp.util.StashDiffCallback
import com.github.damontecres.stashapp.util.StashGlide
import com.github.damontecres.stashapp.util.configRowManager
import com.github.damontecres.stashapp.util.createOCounterLongClickCallBack
import com.github.damontecres.stashapp.util.createRemoveLongClickListener
import com.github.damontecres.stashapp.util.getDataType
import com.github.damontecres.stashapp.util.getDestination
import com.github.damontecres.stashapp.util.isNotNullOrBlank
import com.github.damontecres.stashapp.util.readOnlyModeDisabled
import com.github.damontecres.stashapp.util.showSetRatingToast
import com.github.damontecres.stashapp.views.ClassOnItemViewClickedListener
import com.github.damontecres.stashapp.views.models.SceneViewModel
import com.github.damontecres.stashapp.views.models.ServerViewModel
import kotlinx.coroutines.launch
import kotlin.math.roundToInt

/**
 * Fragment to show a scene's details and actions
 */
class SceneDetailsFragment : DetailsSupportFragment() {
    private val serverViewModel: ServerViewModel by activityViewModels<ServerViewModel>()
    private val viewModel by viewModels<SceneViewModel>()

    private lateinit var sceneId: String
    private var sceneData: FullSceneData? = null

    private lateinit var queryEngine: QueryEngine
    private lateinit var mutationEngine: MutationEngine

    private val mPresenterSelector = ClassPresenterSelector()
    private val mAdapter = SparseArrayObjectAdapter(mPresenterSelector)

    private val studioRowManager =
        ListRowManager<StudioData>(
            DataType.STUDIO,
            ListRowManager.SparseArrayRowModifier(mAdapter, STUDIO_POS),
            ArrayObjectAdapter(),
            StashApplication.getApplication().getString(DataType.STUDIO.stringId),
        ) { studioIds ->
            val newStudioId =
                if (studioIds.isEmpty()) {
                    null
                } else if (studioIds.size == 1) {
                    studioIds.first()
                } else {
                    studioIds.last()
                }

            val result = mutationEngine.setStudioOnScene(sceneId, newStudioId)
            val newStudio = result?.studio?.studioData
            if (newStudio != null) {
                listOf(newStudio)
            } else {
                listOf()
            }
        }

    private val performersRowManager =
        ListRowManager<PerformerData>(
            DataType.PERFORMER,
            ListRowManager.SparseArrayRowModifier(mAdapter, PERFORMER_POS),
            ArrayObjectAdapter(),
        ) { performerIds ->
            val result = mutationEngine.setPerformersOnScene(sceneId, performerIds)
            result?.performers?.map { it.performerData }.orEmpty()
        }

    private val tagsRowManager =
        ListRowManager<TagData>(
            DataType.TAG,
            ListRowManager.SparseArrayRowModifier(mAdapter, TAG_POS),
            ArrayObjectAdapter(),
        ) { tagIds ->
            val result = mutationEngine.setTagsOnScene(sceneId, tagIds)
            result?.tags?.map { it.tagData }.orEmpty()
        }

    private val groupsRowManager =
        ListRowManager<GroupData>(
            DataType.GROUP,
            ListRowManager.SparseArrayRowModifier(mAdapter, GROUP_POS),
            ArrayObjectAdapter(),
        ) { groupIds ->
            val result = mutationEngine.setGroupsOnScene(sceneData!!.id, groupIds)
            result?.groups?.map { it.group.groupData }.orEmpty()
        }

    private val markersRowManager =
        ListRowManager<MarkerData>(
            DataType.MARKER,
            ListRowManager.SparseArrayRowModifier(mAdapter, MARKER_POS),
            ArrayObjectAdapter(),
        ) { markerIds ->
            val removed = sceneData!!.scene_markers.map { it.id }.toSet() - markerIds.toSet()
            removed.forEach { mutationEngine.deleteMarker(it) }
            sceneData =
                sceneData!!.copy(scene_markers = sceneData!!.scene_markers.filter { it.id !in removed })
            queryEngine.findMarkersInScene(sceneData!!.id)
        }

    private val galleriesAdapter = ArrayObjectAdapter(GalleryPresenter())

    private val sceneActionsAdapter: SparseArrayObjectAdapter by lazy {
        SparseArrayObjectAdapter(
            ClassPresenterSelector()
                .addClassPresenter(
                    StashAction::class.java,
                    ActionPresenter(),
                ).addClassPresenter(
                    OCounter::class.java,
                    OCounterPresenter(
                        createOCounterLongClickCallBack(
                            DataType.SCENE,
                            sceneId,
                            mutationEngine,
                            viewLifecycleOwner.lifecycleScope,
                        ) { newCount ->
                            sceneActionsAdapter.set(O_COUNTER_POS, newCount)
                            sceneData = sceneData!!.copy(o_counter = newCount.count)
                        },
                    ),
                ).addClassPresenter(
                    CreateMarkerAction::class.java,
                    CreateMarkerActionPresenter(),
                ),
        )
    }

    private var detailsOverviewRow: DetailsOverviewRow? = null
    private lateinit var mDetailsBackground: DetailsSupportFragmentBackgroundController

    private val playActionsAdapter = SparseArrayObjectAdapter()

    private val detailsPresenter =
        FullWidthDetailsOverviewRowPresenter(
            SceneDetailsPresenter { rating100: Int ->
                viewLifecycleOwner.lifecycleScope.launch(
                    StashCoroutineExceptionHandler(
                        Toast.makeText(
                            requireContext(),
                            "Failed to set rating",
                            Toast.LENGTH_SHORT,
                        ),
                    ),
                ) {
                    mutationEngine.setRating(
                        sceneId,
                        rating100,
                    )
                    showSetRatingToast(requireContext(), rating100)
                }
            },
        )

    override fun onInflateTitleView(
        inflater: LayoutInflater?,
        parent: ViewGroup?,
        savedInstanceState: Bundle?,
    ): View? = null

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        sceneId = requireArguments().getDestination<Destination.Item>().id
        Log.d(TAG, "onCreate: sceneId=$sceneId")

        queryEngine = QueryEngine(serverViewModel.requireServer())
        mutationEngine = MutationEngine(serverViewModel.requireServer())

        mDetailsBackground = DetailsSupportFragmentBackgroundController(this)
        mDetailsBackground.enableParallax()

        setFragmentResultListener(Constants.POSITION_REQUEST_KEY) { _, bundle ->
            val position = bundle.getLong(Constants.POSITION_REQUEST_KEY)
            Log.v(TAG, "setFragmentResultListener: position=$position")
            viewModel.currentPosition.value = position
            if (serverViewModel.requireServer().serverPreferences.trackActivity &&
                PreferenceManager
                    .getDefaultSharedPreferences(requireContext())
                    .getBoolean(getString(R.string.pref_key_playback_track_activity), true)
            ) {
                viewModel.saveCurrentPosition()
            }
        }

        setFragmentResultListener(SceneDetailsFragment::class.simpleName!!) { _, bundle ->
            val sourceId = bundle.getLong(SearchForFragment.RESULT_ID_KEY)
            val dataType = bundle.getDataType()
            val newId = bundle.getString(SearchForFragment.RESULT_ITEM_ID_KEY)
            if (newId != null) {
                viewLifecycleOwner.lifecycleScope.launch(StashCoroutineExceptionHandler()) {
                    when (dataType) {
                        DataType.GROUP -> {
                            val newGroup = groupsRowManager.add(newId)
                            if (newGroup != null) {
                                Toast
                                    .makeText(
                                        requireContext(),
                                        "Added to '${newGroup.name}'",
                                        Toast.LENGTH_SHORT,
                                    ).show()
                            }
                        }

                        DataType.PERFORMER -> {
                            val newPerformer = performersRowManager.add(newId)
                            if (newPerformer != null) {
                                Toast
                                    .makeText(
                                        requireContext(),
                                        "Added performer '${newPerformer.name}' to scene",
                                        Toast.LENGTH_SHORT,
                                    ).show()
                            }
                        }

                        DataType.STUDIO -> {
                            val newStudio = studioRowManager.add(newId)
                            if (newStudio != null) {
                                Toast
                                    .makeText(
                                        requireContext(),
                                        "Set studio to '${newStudio.name}'",
                                        Toast.LENGTH_SHORT,
                                    ).show()
                            }
                        }

                        DataType.TAG -> {
                            if (sourceId == StashAction.ADD_TAG.id) {
                                val newTag = tagsRowManager.add(newId)
                                if (newTag != null) {
                                    Toast
                                        .makeText(
                                            requireContext(),
                                            "Added tag '${newTag.name}' to scene",
                                            Toast.LENGTH_SHORT,
                                        ).show()
                                }
                            } else if (sourceId == StashAction.CREATE_MARKER.id) {
                                val position = viewModel.currentPosition.value ?: 0L
                                Log.d(
                                    TAG,
                                    "Adding marker at $position with tagId=$newId to scene ${sceneData?.id}",
                                )
                                val newMarker =
                                    mutationEngine.createMarker(
                                        sceneId,
                                        position,
                                        newId,
                                    )!!
                                markersRowManager.add(newMarker.id)
                                Toast
                                    .makeText(
                                        requireContext(),
                                        "Created a new marker with primary tag '${newMarker.primary_tag.tagData.name}'",
                                        Toast.LENGTH_SHORT,
                                    ).show()
                            }
                        }

                        DataType.SCENE, DataType.MARKER, DataType.IMAGE, DataType.GALLERY -> throw IllegalArgumentException()
                    }
                }
            }
        }
    }

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?,
    ): View? {
        val actionListener = SceneActionListener()
        onItemViewClickedListener =
            ClassOnItemViewClickedListener(NavigationOnItemViewClickedListener(serverViewModel.navigationManager))
                .addListenerForClass(StashAction::class.java) { item ->
                    actionListener.onClicked(item)
                }.addListenerForClass(CreateMarkerAction::class.java) { _ ->
                    actionListener.onClicked(StashAction.CREATE_MARKER)
                }.addListenerForClass(Action::class.java) { _ ->
                    // no-op, detailsPresenter.onActionClickedListener will handle
                }

        setupDetailsOverviewRowPresenter()
        mPresenterSelector.addClassPresenter(ListRow::class.java, ListRowPresenter())
        return super.onCreateView(inflater, container, savedInstanceState)
    }

    override fun onViewCreated(
        view: View,
        savedInstanceState: Bundle?,
    ) {
        super.onViewCreated(view, savedInstanceState)

        if (mAdapter.lookup(ACTIONS_POS) == null) {
            if (readOnlyModeDisabled()) {
                sceneActionsAdapter.set(ADD_TAG_POS, StashAction.ADD_TAG)
                sceneActionsAdapter.set(ADD_PERFORMER_POS, StashAction.ADD_PERFORMER)
                sceneActionsAdapter.set(SET_STUDIO_POS, StashAction.SET_STUDIO)
                sceneActionsAdapter.set(ADD_GROUP_POS, StashAction.ADD_GROUP)
            }
            sceneActionsAdapter.set(FORCE_TRANSCODE_POS, StashAction.FORCE_TRANSCODE)
            sceneActionsAdapter.set(FORCE_DIRECT_PLAY_POS, StashAction.FORCE_DIRECT_PLAY)
            mAdapter.set(
                ACTIONS_POS,
                ListRow(HeaderItem(getString(R.string.stashapp_actions_name)), sceneActionsAdapter),
            )
        }

        configRowManager({ viewLifecycleOwner.lifecycleScope }, tagsRowManager, ::TagPresenter)
        configRowManager({ viewLifecycleOwner.lifecycleScope }, studioRowManager, ::StudioPresenter)
        configRowManager({ viewLifecycleOwner.lifecycleScope }, groupsRowManager, ::GroupPresenter)

        markersRowManager.adapter.presenterSelector =
            SinglePresenterSelector(
                MarkerPresenter(
                    createRemoveLongClickListener(
                        { viewLifecycleOwner.lifecycleScope },
                        markersRowManager,
                    ).addAction(MARKER_DETAILS_POPUP) { _, item ->
                        serverViewModel.navigationManager.navigate(
                            Destination.MarkerDetails(
                                item.id,
                                item.scene.videoSceneData.id,
                            ),
                        )
                    },
                ),
            )

        setupObservers()
    }

    override fun onResume() {
        super.onResume()
        viewModel.init(sceneId, true)
    }

    private fun setupObservers() {
        viewModel.scene.observe(viewLifecycleOwner) { sceneData ->
            if (sceneData == null) {
                Toast
                    .makeText(requireContext(), "Scene $sceneId not found", Toast.LENGTH_LONG)
                    .show()
                serverViewModel.navigationManager.goBack()
                return@observe
            }
            Log.v(TAG, "sceneData.id=${sceneData.id}")

            configRowManager(
                { viewLifecycleOwner.lifecycleScope },
                performersRowManager,
            ) { callback ->
                PerformerInScenePresenter(sceneData.date, callback)
            }

            setupDetailsOverviewRow(sceneData)

            if (adapter == null) {
                adapter = mAdapter
            }

            initializeBackground(sceneData)

            if (readOnlyModeDisabled()) {
                sceneActionsAdapter.set(
                    O_COUNTER_POS,
                    OCounter(
                        sceneId,
                        sceneData.o_counter ?: 0,
                    ),
                )
            }

            if (sceneData.studio?.studioData != null) {
                studioRowManager.setItems(listOf(sceneData.studio.studioData))
            }

            this@SceneDetailsFragment.sceneData = sceneData
        }

        viewModel.tags.observe(viewLifecycleOwner, tagsRowManager::setItems)

        viewModel.markers.observe(viewLifecycleOwner, markersRowManager::setItems)

        viewModel.groups.observe(viewLifecycleOwner, groupsRowManager::setItems)

        viewModel.performers.observe(viewLifecycleOwner, performersRowManager::setItems)

        viewModel.galleries.observe(viewLifecycleOwner) { galleries ->
            if (galleries.isNotEmpty()) {
                if (mAdapter.lookup(GALLERY_POS) == null) {
                    mAdapter.set(
                        GALLERY_POS,
                        ListRow(
                            HeaderItem(getString(R.string.stashapp_galleries)),
                            galleriesAdapter,
                        ),
                    )
                }
                galleriesAdapter.setItems(galleries, StashDiffCallback)
            } else {
                mAdapter.clear(GALLERY_POS)
            }
        }

        viewModel.suggestedScenes.observe(viewLifecycleOwner) { items ->
            if (items.isEmpty()) {
                mAdapter.clear(SIMILAR_POS)
            } else {
                val adapter =
                    ArrayObjectAdapter(StashPresenter.defaultClassPresenterSelector())
                adapter.addAll(0, items)
                mAdapter.set(
                    SIMILAR_POS,
                    ListRow(
                        HeaderItem(getString(R.string.more_like_this_scene)),
                        adapter,
                    ),
                )
            }
        }

        viewModel.currentPosition.observe(viewLifecycleOwner) { localPosition ->
            if (localPosition >= 0 && readOnlyModeDisabled()) {
                sceneActionsAdapter.set(
                    CREATE_MARKER_POS,
                    CreateMarkerAction(localPosition),
                )
            }

            setupPlayActionsAdapter(localPosition)
        }
    }

    private fun initializeBackground(sceneData: FullSceneData) {
        val screenshotUrl = sceneData.paths.screenshot
        if (mDetailsBackground.coverBitmap == null && screenshotUrl.isNotNullOrBlank()) {
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

    private fun setupDetailsOverviewRow(sceneData: FullSceneData) {
        val row =
            if (detailsOverviewRow != null) {
                detailsOverviewRow!!
            } else {
                val newRow = DetailsOverviewRow(sceneData)
                newRow.actionsAdapter = playActionsAdapter
                mAdapter.set(DETAILS_POS, newRow)
                newRow
            }

        val screenshotUrl = sceneData.paths.screenshot
        if (detailsOverviewRow == null && !screenshotUrl.isNullOrBlank()) {
            row.item = sceneData
            val width = convertDpToPixel(requireActivity(), DETAIL_THUMB_WIDTH)
            val height = convertDpToPixel(requireActivity(), DETAIL_THUMB_HEIGHT)
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
        detailsOverviewRow = row
    }

    private fun setupDetailsOverviewRowPresenter() {
        // Set detail background.
        detailsPresenter.backgroundColor =
            ContextCompat.getColor(requireActivity(), R.color.default_card_background)

        // Hook up transition element.
//        val sharedElementHelper = FullWidthDetailsOverviewSharedElementHelper()
//        sharedElementHelper.setSharedElementEnterTransition(
//            activity,
//            SceneDetailsActivity.SHARED_ELEMENT_NAME,
//        )
//        detailsPresenter.setListener(sharedElementHelper)
        detailsPresenter.isParticipatingEntranceTransition = true

        detailsPresenter.onActionClickedListener =
            OnActionClickedListener { action ->
                if (this.sceneData != null) {
                    if (action.id in
                        longArrayOf(
                            ACTION_PLAY_SCENE,
                            ACTION_RESUME_SCENE,
                            ACTION_TRANSCODE_RESUME_SCENE,
                            ACTION_DIRECT_PLAY_RESUME_SCENE,
                        )
                    ) {
                        val position =
                            if (action.id == ACTION_RESUME_SCENE ||
                                action.id == ACTION_TRANSCODE_RESUME_SCENE ||
                                action.id == ACTION_DIRECT_PLAY_RESUME_SCENE
                            ) {
                                viewModel.currentPosition.value ?: 0L
                            } else {
                                0L
                            }
                        val mode =
                            when (action.id) {
                                ACTION_TRANSCODE_RESUME_SCENE -> PlaybackMode.FORCED_TRANSCODE
                                ACTION_DIRECT_PLAY_RESUME_SCENE -> PlaybackMode.FORCED_DIRECT_PLAY
                                else -> PlaybackMode.CHOOSE
                            }

                        val playbackDest = Destination.Playback(sceneId, position, mode)
                        serverViewModel.navigationManager.navigate(playbackDest)
                    } else {
                        throw IllegalArgumentException("Action $action (id=${action.id} is not supported!")
                    }
                }
            }
        mPresenterSelector.addClassPresenter(DetailsOverviewRow::class.java, detailsPresenter)
    }

    private fun convertDpToPixel(
        context: Context,
        dp: Int,
    ): Int {
        val density = context.applicationContext.resources.displayMetrics.density
        return (dp.toFloat() * density).roundToInt()
    }

    private inner class SceneActionListener : StashActionClickedListener {
        override fun onClicked(action: StashAction) {
            if (action in StashAction.SEARCH_FOR_ACTIONS) {
                val dataType =
                    when (action) {
                        StashAction.ADD_TAG -> DataType.TAG
                        StashAction.ADD_PERFORMER -> DataType.PERFORMER
                        StashAction.ADD_GROUP -> DataType.GROUP
                        StashAction.SET_STUDIO -> DataType.STUDIO
                        StashAction.CREATE_MARKER -> DataType.TAG

                        else -> throw RuntimeException("Unsupported search for type $action")
                    }
                val title =
                    if (action == StashAction.CREATE_MARKER) "for primary tag for scene marker" else null
                serverViewModel.navigationManager.navigate(
                    Destination.SearchFor(
                        SceneDetailsFragment::class.simpleName!!,
                        action.id,
                        dataType,
                        title,
                    ),
                )
            } else if (action == StashAction.FORCE_TRANSCODE) {
                detailsPresenter.onActionClickedListener.onActionClicked(
                    Action(
                        ACTION_TRANSCODE_RESUME_SCENE,
                    ),
                )
            } else if (action == StashAction.FORCE_DIRECT_PLAY) {
                detailsPresenter.onActionClickedListener.onActionClicked(
                    Action(
                        ACTION_DIRECT_PLAY_RESUME_SCENE,
                    ),
                )
            }
        }

        override fun incrementOCounter(counter: OCounter) {
            viewLifecycleOwner.lifecycleScope.launch(
                StashCoroutineExceptionHandler(
                    Toast.makeText(
                        requireContext(),
                        getString(R.string.failed_o_counter),
                        Toast.LENGTH_SHORT,
                    ),
                ),
            ) {
                val newCounter = mutationEngine.incrementOCounter(counter.id)
                sceneData = sceneData!!.copy(o_counter = newCounter.count)
                sceneActionsAdapter.set(O_COUNTER_POS, newCounter)
            }
        }
    }

    private fun setupPlayActionsAdapter(position: Long) {
        Log.v(TAG, "setupPlayActionsAdapter: position=$position")
        if (position >= 0L) {
            val serverPreferences = serverViewModel.requireServer().serverPreferences
            if (position <= 0L || serverPreferences.alwaysStartFromBeginning) {
                playActionsAdapter.set(
                    0,
                    Action(ACTION_PLAY_SCENE, resources.getString(R.string.play_scene)),
                )
                playActionsAdapter.clear(1)
            } else {
                playActionsAdapter.set(0, Action(ACTION_RESUME_SCENE, getString(R.string.resume)))
                // Force focus to move to Resume
                playActionsAdapter.clear(1)
                playActionsAdapter.set(1, Action(ACTION_PLAY_SCENE, getString(R.string.restart)))
            }
        } else {
            playActionsAdapter.clear()
        }
    }

    companion object {
        private const val TAG = "SceneDetailsFragment"

        private const val ACTION_PLAY_SCENE = 1L
        private const val ACTION_RESUME_SCENE = 2L
        private const val ACTION_TRANSCODE_RESUME_SCENE = 3L
        private const val ACTION_DIRECT_PLAY_RESUME_SCENE = 4L

        private const val DETAIL_THUMB_WIDTH = ScenePresenter.CARD_WIDTH
        private const val DETAIL_THUMB_HEIGHT = ScenePresenter.CARD_HEIGHT

        // Row order
        private const val DETAILS_POS = 1
        private const val MARKER_POS = DETAILS_POS + 1
        private const val GROUP_POS = MARKER_POS + 1
        private const val STUDIO_POS = GROUP_POS + 1
        private const val PERFORMER_POS = STUDIO_POS + 1
        private const val TAG_POS = PERFORMER_POS + 1
        private const val GALLERY_POS = TAG_POS + 1
        private const val ACTIONS_POS = GALLERY_POS + 1
        private const val SIMILAR_POS = ACTIONS_POS + 1

        // Actions row order
        private const val O_COUNTER_POS = 1
        private const val CREATE_MARKER_POS = O_COUNTER_POS + 1
        private const val ADD_TAG_POS = CREATE_MARKER_POS + 1
        private const val ADD_PERFORMER_POS = ADD_TAG_POS + 1
        private const val ADD_GROUP_POS = ADD_PERFORMER_POS + 1
        private const val SET_STUDIO_POS = ADD_GROUP_POS + 1
        private const val FORCE_TRANSCODE_POS = SET_STUDIO_POS + 1
        private const val FORCE_DIRECT_PLAY_POS = FORCE_TRANSCODE_POS + 1

        private val MARKER_DETAILS_POPUP =
            StashPresenter.PopUpItem(
                177,
                StashApplication.getApplication().getString(R.string.stashapp_details),
            )
    }
}
