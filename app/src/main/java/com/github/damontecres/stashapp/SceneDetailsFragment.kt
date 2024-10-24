package com.github.damontecres.stashapp

import android.app.Activity
import android.content.Context
import android.content.Intent
import android.graphics.Bitmap
import android.graphics.drawable.Drawable
import android.os.Bundle
import android.util.Log
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Toast
import androidx.activity.result.ActivityResult
import androidx.activity.result.ActivityResultCallback
import androidx.activity.result.ActivityResultLauncher
import androidx.activity.result.contract.ActivityResultContracts
import androidx.core.content.ContextCompat
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
import com.github.damontecres.stashapp.data.Marker
import com.github.damontecres.stashapp.data.OCounter
import com.github.damontecres.stashapp.data.Scene
import com.github.damontecres.stashapp.playback.PlaybackActivity
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
import com.github.damontecres.stashapp.util.StashServer
import com.github.damontecres.stashapp.util.asSlimTagData
import com.github.damontecres.stashapp.util.asVideoSceneData
import com.github.damontecres.stashapp.util.isNotNullOrBlank
import com.github.damontecres.stashapp.util.putDataType
import com.github.damontecres.stashapp.util.showSetRatingToast
import com.github.damontecres.stashapp.views.ClassOnItemViewClickedListener
import com.github.damontecres.stashapp.views.StashItemViewClickListener
import kotlinx.coroutines.CoroutineExceptionHandler
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.launch
import kotlin.math.roundToInt

/**
 * Fragment to show a scene's details and actions
 */
class SceneDetailsFragment : DetailsSupportFragment() {
    private var pendingJob: Job? = null

    private lateinit var sceneId: String
    private var sceneData: FullSceneData? = null

    private lateinit var queryEngine: QueryEngine
    private lateinit var mutationEngine: MutationEngine

    private val mPresenterSelector = ClassPresenterSelector()
    private val mAdapter = SparseArrayObjectAdapter(mPresenterSelector)

    private val mStudioAdapter = ArrayObjectAdapter(StudioPresenter(StudioLongClickCallBack()))
    private val studioAdapter =
        ListRowManager<StudioData>(
            DataType.STUDIO,
            ListRowManager.SparseArrayRowModifier(mAdapter, STUDIO_POS),
            mStudioAdapter,
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

    // Presenter is set in fetchData because it requires mSelectedGroup
    private val mPerformersAdapter = ArrayObjectAdapter()
    private val performersRowManager =
        ListRowManager<PerformerData>(
            DataType.PERFORMER,
            ListRowManager.SparseArrayRowModifier(mAdapter, PERFORMER_POS),
            mPerformersAdapter,
        ) { performerIds ->
            val result = mutationEngine.setPerformersOnScene(sceneId, performerIds)
            result?.performers?.map { it.performerData }.orEmpty()
        }

    private val tagsRowManager =
        ListRowManager<TagData>(
            DataType.TAG,
            ListRowManager.SparseArrayRowModifier(mAdapter, TAG_POS),
            ArrayObjectAdapter(TagPresenter(TagLongClickCallBack())),
        ) { tagIds ->
            val result = mutationEngine.setTagsOnScene(sceneId, tagIds)
            result?.tags?.map { it.tagData }.orEmpty()
        }

    private val groupsRowManager =
        ListRowManager<GroupData>(
            DataType.GROUP,
            ListRowManager.SparseArrayRowModifier(mAdapter, GROUP_POS),
            ArrayObjectAdapter(GroupPresenter(GroupLongClickCallBack())),
        ) { groupIds ->
            val result = mutationEngine.setGroupsOnScene(sceneData!!.id, groupIds)
            result?.groups?.map { it.group.groupData }.orEmpty()
        }

    private val markersAdapter = ArrayObjectAdapter(MarkerPresenter(MarkerLongClickCallBack()))
    private val galleriesAdapter = ArrayObjectAdapter(GalleryPresenter())
    private val sceneActionsAdapter =
        SparseArrayObjectAdapter(
            ClassPresenterSelector().addClassPresenter(
                StashAction::class.java,
                ActionPresenter(),
            ).addClassPresenter(
                OCounter::class.java,
                OCounterPresenter(OCounterLongClickCallBack()),
            ).addClassPresenter(
                CreateMarkerAction::class.java,
                CreateMarkerActionPresenter(),
            ),
        )

    private var detailsOverviewRow: DetailsOverviewRow? = null
    private lateinit var mDetailsBackground: DetailsSupportFragmentBackgroundController

    private lateinit var resultLauncher: ActivityResultLauncher<Intent>

    private val playActionsAdapter = SparseArrayObjectAdapter()
    private var position = -1L // The position in the video
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
                    MutationEngine(server).setRating(
                        sceneId,
                        rating100,
                    )
                    showSetRatingToast(requireContext(), rating100)
                }
            },
        )

    private val server = StashServer.requireCurrentServer()
    private val serverPreferences = server.serverPreferences

    override fun onCreate(savedInstanceState: Bundle?) {
        Log.d(TAG, "onCreate DetailsFragment")
        super.onCreate(savedInstanceState)

        sceneId = requireActivity().intent.getStringExtra(Constants.SCENE_ID_ARG)!!

        queryEngine = QueryEngine(server)
        mutationEngine = MutationEngine(server)

        mDetailsBackground = DetailsSupportFragmentBackgroundController(this)
        resultLauncher =
            registerForActivityResult(
                ActivityResultContracts.StartActivityForResult(),
                ResultCallback(),
            )
    }

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?,
    ): View? {
        val actionListener = SceneActionListener()
        onItemViewClickedListener =
            ClassOnItemViewClickedListener(
                StashItemViewClickListener(
                    requireActivity(),
                    actionListener,
                ),
            ).addListenerForClass(CreateMarkerAction::class.java) { _ ->
                actionListener.onClicked(StashAction.CREATE_MARKER)
            }

        setupDetailsOverviewRowPresenter()
        mAdapter.set(
            ACTIONS_POS,
            ListRow(HeaderItem(getString(R.string.stashapp_actions_name)), sceneActionsAdapter),
        )
        mPresenterSelector.addClassPresenter(ListRow::class.java, ListRowPresenter())
        return super.onCreateView(inflater, container, savedInstanceState)
    }

    override fun onViewCreated(
        view: View,
        savedInstanceState: Bundle?,
    ) {
        super.onViewCreated(view, savedInstanceState)
        if (sceneData == null) {
            viewLifecycleOwner.lifecycleScope.launch(StashCoroutineExceptionHandler(true)) {
                fetchData(sceneId)
            }
        }
    }

    override fun onResume() {
        super.onResume()
        viewLifecycleOwner.lifecycleScope.launch(StashCoroutineExceptionHandler(true)) {
            fetchData(sceneId)
        }
    }

    private suspend fun fetchData(sceneId: String) {
        pendingJob?.join()
        pendingJob = null
        sceneData = queryEngine.getScene(sceneId)
        if (sceneData != null) {
            mPerformersAdapter.presenterSelector =
                SinglePresenterSelector(
                    PerformerInScenePresenter(
                        sceneData!!.date,
                        PerformerLongClickCallBack(),
                    ),
                )
        }

        // Need to check position because the activity result callback happens before onResume
        if (position <= 0 &&
            serverPreferences.trackActivity &&
            sceneData?.resume_time != null &&
            sceneData?.resume_time!! > 0
        ) {
            position = (sceneData?.resume_time!! * 1000).toLong()
        }
        setupPlayActionsAdapter()

        setupDetailsOverviewRow()

        adapter = mAdapter
        initializeBackground()

        sceneActionsAdapter.set(
            O_COUNTER_POS,
            OCounter(
                sceneId,
                sceneData?.o_counter ?: 0,
            ),
        )
        sceneActionsAdapter.set(ADD_TAG_POS, StashAction.ADD_TAG)
        sceneActionsAdapter.set(ADD_PERFORMER_POS, StashAction.ADD_PERFORMER)
        sceneActionsAdapter.set(CREATE_MARKER_POS, CreateMarkerAction(position))
        sceneActionsAdapter.set(SET_STUDIO_POS, StashAction.SET_STUDIO)
        sceneActionsAdapter.set(ADD_GROUP_POS, StashAction.ADD_GROUP)
        sceneActionsAdapter.set(FORCE_TRANSCODE_POS, StashAction.FORCE_TRANSCODE)
        sceneActionsAdapter.set(FORCE_DIRECT_PLAY_POS, StashAction.FORCE_DIRECT_PLAY)

        if (sceneData!!.studio?.studioData != null) {
            studioAdapter.setItems(listOf(sceneData!!.studio!!.studioData))
        }

        tagsRowManager.setItems(sceneData!!.tags.map { it.tagData })

        if (sceneData!!.scene_markers.isNotEmpty()) {
            if (mAdapter.lookup(MARKER_POS) == null) {
                mAdapter.set(
                    MARKER_POS,
                    ListRow(HeaderItem(getString(R.string.stashapp_markers)), markersAdapter),
                )
            }
            markersAdapter.setItems(
                sceneData!!.scene_markers.map(::convertMarker),
                StashDiffCallback,
            )
        } else {
            mAdapter.clear(MARKER_POS)
        }

        val performerIds = sceneData!!.performers.map { it.id }
        Log.v(TAG, "fetchData performerIds=$performerIds")
        if (performerIds.isNotEmpty()) {
            val perfs = queryEngine.findPerformers(performerIds = performerIds)
            performersRowManager.setItems(perfs)
        } else {
            performersRowManager.setItems(listOf())
        }

        groupsRowManager.setItems(sceneData!!.groups.map { it.group.groupData })

        if (sceneData!!.galleries.isNotEmpty()) {
            viewLifecycleOwner.lifecycleScope.launch(StashCoroutineExceptionHandler()) {
                if (mAdapter.lookup(GALLERY_POS) == null) {
                    mAdapter.set(
                        GALLERY_POS,
                        ListRow(
                            HeaderItem(getString(R.string.stashapp_galleries)),
                            galleriesAdapter,
                        ),
                    )
                }
                val galleries =
                    queryEngine.getGalleries(sceneData!!.galleries.map { it.id })
                galleriesAdapter.setItems(galleries, StashDiffCallback)
            }
        } else {
            mAdapter.clear(GALLERY_POS)
        }
    }

    private fun initializeBackground() {
        if (mDetailsBackground.coverBitmap == null) {
            mDetailsBackground.enableParallax()

            val screenshotUrl = sceneData!!.paths.screenshot

            if (screenshotUrl.isNotNullOrBlank()) {
                StashGlide.withBitmap(requireActivity(), screenshotUrl)
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

    private fun setupDetailsOverviewRow() {
        val row = detailsOverviewRow ?: DetailsOverviewRow(sceneData)

        val screenshotUrl = sceneData?.paths?.screenshot
        if ((detailsOverviewRow == null || sceneData != row.item) && !screenshotUrl.isNullOrBlank()) {
            row.item = sceneData
            val width = convertDpToPixel(requireActivity(), DETAIL_THUMB_WIDTH)
            val height = convertDpToPixel(requireActivity(), DETAIL_THUMB_HEIGHT)
            StashGlide.with(requireActivity(), screenshotUrl)
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

        row.actionsAdapter = playActionsAdapter

        mAdapter.set(DETAILS_POS, row)
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
                if (sceneData != null) {
                    if (action.id in
                        longArrayOf(
                            ACTION_PLAY_SCENE,
                            ACTION_RESUME_SCENE,
                            ACTION_TRANSCODE_RESUME_SCENE,
                            ACTION_DIRECT_PLAY_RESUME_SCENE,
                        )
                    ) {
                        val intent = Intent(requireActivity(), PlaybackActivity::class.java)
                        intent.putExtra(
                            Constants.SCENE_ARG,
                            Scene.fromFullSceneData(sceneData!!),
                        )
                        if (action.id == ACTION_RESUME_SCENE ||
                            action.id == ACTION_TRANSCODE_RESUME_SCENE ||
                            action.id == ACTION_DIRECT_PLAY_RESUME_SCENE
                        ) {
                            intent.putExtra(Constants.POSITION_ARG, position)
                        }
                        if (action.id == ACTION_TRANSCODE_RESUME_SCENE) {
                            intent.putExtra(FORCE_TRANSCODE, true)
                        } else if (action.id == ACTION_DIRECT_PLAY_RESUME_SCENE) {
                            intent.putExtra(FORCE_DIRECT_PLAY, true)
                        }
                        resultLauncher.launch(intent)
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
                val intent = Intent(requireActivity(), SearchForActivity::class.java)
                val dataType =
                    when (action) {
                        StashAction.ADD_TAG -> DataType.TAG
                        StashAction.ADD_PERFORMER -> DataType.PERFORMER
                        StashAction.ADD_GROUP -> DataType.GROUP
                        StashAction.SET_STUDIO -> DataType.STUDIO
                        StashAction.CREATE_MARKER -> {
                            intent.putExtra(
                                SearchForFragment.TITLE_KEY,
                                "for primary tag for scene marker",
                            )
                            DataType.TAG
                        }

                        else -> throw RuntimeException("Unsupported search for type $action")
                    }
                intent.putDataType(dataType)
                intent.putExtra(SearchForFragment.ID_KEY, action.id)
                resultLauncher.launch(intent)
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

    private fun setupPlayActionsAdapter() {
        if (sceneData!!.files.isNotEmpty()) {
            val serverPreferences = server.serverPreferences
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

    private inner class ResultCallback : ActivityResultCallback<ActivityResult> {
        override fun onActivityResult(result: ActivityResult) {
            if (result.resultCode == Activity.RESULT_OK) {
                val data: Intent? = result.data
                val id = data!!.getLongExtra(SearchForFragment.ID_KEY, -1)
                if (id in StashAction.SEARCH_FOR_ACTIONS.map { it.id }) {
                    pendingJob =
                        viewLifecycleOwner.lifecycleScope.launch(
                            CoroutineExceptionHandler { _, ex ->
                                Log.e(TAG, "Exception", ex)
                                Toast.makeText(
                                    requireContext(),
                                    "Failed to update scene: ${ex.message}",
                                    Toast.LENGTH_LONG,
                                ).show()
                            },
                        ) {
                            // This will be called before other lifecycle methods, so refresh data if needed
                            if (sceneData == null) {
                                fetchData(sceneId)
                            }
                            val newId = data.getStringExtra(SearchForFragment.RESULT_ID_KEY)!!
                            when (id) {
                                StashAction.ADD_TAG.id -> {
                                    val newTag = tagsRowManager.add(newId)
                                    if (newTag != null) {
                                        Toast.makeText(
                                            requireContext(),
                                            "Added tag '${newTag.name}' to scene",
                                            Toast.LENGTH_SHORT,
                                        ).show()
                                    }
                                }

                                StashAction.ADD_PERFORMER.id -> {
                                    val newPerformer = performersRowManager.add(newId)
                                    if (newPerformer != null) {
                                        Toast.makeText(
                                            requireContext(),
                                            "Added performer '${newPerformer.name}' to scene",
                                            Toast.LENGTH_SHORT,
                                        ).show()
                                    }
                                }

                                StashAction.SET_STUDIO.id -> {
                                    val newStudio = studioAdapter.add(newId)
                                    if (newStudio != null) {
                                        Toast.makeText(
                                            requireContext(),
                                            "Set studio to '${newStudio.name}'",
                                            Toast.LENGTH_SHORT,
                                        ).show()
                                    }
                                }

                                StashAction.ADD_GROUP.id -> {
                                    val newGroup = groupsRowManager.add(newId)
                                    if (newGroup != null) {
                                        Toast.makeText(
                                            requireContext(),
                                            "Added to '${newGroup.name}'",
                                            Toast.LENGTH_SHORT,
                                        ).show()
                                    }
                                }

                                StashAction.CREATE_MARKER.id -> {
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
                                    val index =
                                        markersAdapter.unmodifiableList<MarkerData>()
                                            .indexOfFirst {
                                                newMarker.seconds < it.seconds
                                            }.coerceAtLeast(0)
                                    markersAdapter.add(index, newMarker)
                                    if (mAdapter.lookup(MARKER_POS) == null) {
                                        mAdapter.set(
                                            MARKER_POS,
                                            ListRow(
                                                HeaderItem(getString(R.string.stashapp_markers)),
                                                markersAdapter,
                                            ),
                                        )
                                    }
                                    Toast.makeText(
                                        requireContext(),
                                        "Created a new marker with primary tag '${newMarker.primary_tag.tagData.name}'",
                                        Toast.LENGTH_SHORT,
                                    ).show()
                                }
                            }
                        }
                } else {
                    val localPosition = data.getLongExtra(POSITION_RESULT_ARG, -1)
                    if (localPosition >= 0) {
                        sceneActionsAdapter.set(
                            CREATE_MARKER_POS,
                            CreateMarkerAction(localPosition),
                        )
                    }
                    if (serverPreferences.trackActivity) {
                        viewLifecycleOwner.lifecycleScope.launch(Dispatchers.IO + StashCoroutineExceptionHandler()) {
                            Log.v(TAG, "ResultCallback saveSceneActivity start")
                            mutationEngine.saveSceneActivity(
                                sceneId,
                                localPosition,
                            )
                        }
                    }

                    position = localPosition
                    setupPlayActionsAdapter()
                }
            }
        }
    }

    private fun convertMarker(it: FullSceneData.Scene_marker): MarkerData {
        return MarkerData(
            id = it.id,
            title = it.title,
            created_at = "",
            updated_at = "",
            stream = it.stream,
            screenshot = it.screenshot,
            seconds = it.seconds,
            preview = "",
            primary_tag = MarkerData.Primary_tag("", it.primary_tag.tagData.asSlimTagData),
            scene = MarkerData.Scene(sceneId, sceneData!!.asVideoSceneData),
            tags = it.tags.map { MarkerData.Tag("", it.tagData.asSlimTagData) },
            __typename = "",
        )
    }

    private inner class OCounterLongClickCallBack : StashPresenter.LongClickCallBack<OCounter> {
        override fun getPopUpItems(
            context: Context,
            item: OCounter,
        ): List<StashPresenter.PopUpItem> {
            return listOf(
                StashPresenter.PopUpItem(0L, getString(R.string.increment)),
                StashPresenter.PopUpItem(1L, getString(R.string.decrement)),
                StashPresenter.PopUpItem(2L, getString(R.string.reset)),
            )
        }

        override fun onItemLongClick(
            context: Context,
            item: OCounter,
            popUpItem: StashPresenter.PopUpItem,
        ) {
            viewLifecycleOwner.lifecycleScope.launch(
                StashCoroutineExceptionHandler(
                    Toast.makeText(
                        requireContext(),
                        getString(R.string.failed_o_counter),
                        Toast.LENGTH_SHORT,
                    ),
                ),
            ) {
                when (popUpItem.id) {
                    0L -> {
                        // Increment
                        val newCount = mutationEngine.incrementOCounter(sceneId)
                        sceneActionsAdapter.set(O_COUNTER_POS, newCount)
                        sceneData = sceneData!!.copy(o_counter = newCount.count)
                    }

                    1L -> {
                        // Decrement
                        val newCount = mutationEngine.decrementOCounter(sceneId)
                        sceneActionsAdapter.set(O_COUNTER_POS, newCount)
                        sceneData = sceneData!!.copy(o_counter = newCount.count)
                    }

                    2L -> {
                        // Reset
                        val newCount = mutationEngine.resetOCounter(sceneId)
                        sceneData = sceneData!!.copy(o_counter = newCount.count)
                        sceneActionsAdapter.set(O_COUNTER_POS, newCount)
                    }

                    else ->
                        Log.w(
                            TAG,
                            "Unknown position for OCounterLongClickCallBack: $popUpItem",
                        )
                }
            }
        }
    }

    /**
     * Just a convenience interface for tags, performers, & markers since they all use the same popup items
     */
    private interface DetailsLongClickCallBack<T> : StashPresenter.LongClickCallBack<T> {
        override fun getPopUpItems(
            context: Context,
            item: T,
        ): List<StashPresenter.PopUpItem> {
            return listOf(REMOVE_POPUP_ITEM)
        }
    }

    private inner class TagLongClickCallBack : DetailsLongClickCallBack<TagData> {
        override fun onItemLongClick(
            context: Context,
            item: TagData,
            popUpItem: StashPresenter.PopUpItem,
        ) {
            if (popUpItem == REMOVE_POPUP_ITEM) {
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
        }
    }

    private inner class GroupLongClickCallBack : DetailsLongClickCallBack<GroupData> {
        override fun onItemLongClick(
            context: Context,
            item: GroupData,
            popUpItem: StashPresenter.PopUpItem,
        ) {
            if (popUpItem == REMOVE_POPUP_ITEM) {
                viewLifecycleOwner.lifecycleScope.launch(
                    CoroutineExceptionHandler { _, ex ->
                        Log.e(TAG, "Exception setting groups", ex)
                        Toast.makeText(
                            requireContext(),
                            "Failed to remove group: ${ex.message}",
                            Toast.LENGTH_LONG,
                        ).show()
                    },
                ) {
                    if (groupsRowManager.remove(item)) {
                        Toast.makeText(
                            requireContext(),
                            "Removed group '${item.name}' from scene",
                            Toast.LENGTH_SHORT,
                        ).show()
                    }
                }
            }
        }
    }

    private inner class MarkerLongClickCallBack : StashPresenter.LongClickCallBack<MarkerData> {
        override fun getPopUpItems(
            context: Context,
            item: MarkerData,
        ): List<StashPresenter.PopUpItem> {
            return listOf(
                StashPresenter.PopUpItem(177L, getString(R.string.stashapp_details)),
                REMOVE_POPUP_ITEM,
            )
        }

        override fun onItemLongClick(
            context: Context,
            item: MarkerData,
            popUpItem: StashPresenter.PopUpItem,
        ) {
            when (popUpItem.id) {
                177L -> {
                    val intent = Intent(context, DataTypeActivity::class.java)
                    intent.putDataType(DataType.MARKER)
                    intent.putExtra("marker", Marker(item))
                    context.startActivity(intent)
                }

                REMOVE_POPUP_ITEM.id -> {
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
                        val mutResult = mutationEngine.deleteMarker(item.id)
                        if (mutResult) {
                            markersAdapter.remove(item)
                            if (markersAdapter.size() == 0) {
                                mAdapter.clear(MARKER_POS)
                            }
                            Toast.makeText(
                                requireContext(),
                                "Removed marker from scene",
                                Toast.LENGTH_SHORT,
                            ).show()
                        }
                    }
                }

                else -> {
                    throw IllegalArgumentException("Unknown id ${popUpItem.id}")
                }
            }
        }
    }

    private inner class PerformerLongClickCallBack : DetailsLongClickCallBack<PerformerData> {
        override fun onItemLongClick(
            context: Context,
            item: PerformerData,
            popUpItem: StashPresenter.PopUpItem,
        ) {
            if (popUpItem == REMOVE_POPUP_ITEM) {
                val performerId = item.id
                Log.d(
                    TAG,
                    "Removing performer $performerId to scene ${sceneData?.id}",
                )
                viewLifecycleOwner.lifecycleScope.launch(
                    CoroutineExceptionHandler { _, ex ->
                        Log.e(TAG, "Exception setting performers", ex)
                        Toast.makeText(
                            requireContext(),
                            "Failed to remove performer: ${ex.message}",
                            Toast.LENGTH_LONG,
                        ).show()
                    },
                ) {
                    if (performersRowManager.remove(item)) {
                        Toast.makeText(
                            requireContext(),
                            "Removed performer '${item.name}' from scene",
                            Toast.LENGTH_SHORT,
                        ).show()
                    }
                }
            }
        }
    }

    private inner class StudioLongClickCallBack : DetailsLongClickCallBack<StudioData> {
        override fun onItemLongClick(
            context: Context,
            item: StudioData,
            popUpItem: StashPresenter.PopUpItem,
        ) {
            if (popUpItem == REMOVE_POPUP_ITEM) {
                viewLifecycleOwner.lifecycleScope.launch(
                    CoroutineExceptionHandler { _, ex ->
                        Log.e(TAG, "Exception setting studio", ex)
                        Toast.makeText(
                            requireContext(),
                            "Failed to remove studio: ${ex.message}",
                            Toast.LENGTH_LONG,
                        ).show()
                    },
                ) {
                    if (studioAdapter.remove(item)) {
                        Toast.makeText(
                            requireContext(),
                            "Removed studio from scene",
                            Toast.LENGTH_SHORT,
                        ).show()
                    }
                }
            }
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

        const val POSITION_RESULT_ARG = "position.result"
        const val FORCE_TRANSCODE = "forceTranscode"
        const val FORCE_DIRECT_PLAY = "forceDirectPlay"

        // Row order
        private const val DETAILS_POS = 1
        private const val MARKER_POS = DETAILS_POS + 1
        private const val GROUP_POS = MARKER_POS + 1
        private const val STUDIO_POS = GROUP_POS + 1
        private const val PERFORMER_POS = STUDIO_POS + 1
        private const val TAG_POS = PERFORMER_POS + 1
        private const val GALLERY_POS = TAG_POS + 1
        private const val ACTIONS_POS = GALLERY_POS + 1

        // Actions row order
        private const val O_COUNTER_POS = 1
        private const val CREATE_MARKER_POS = O_COUNTER_POS + 1
        private const val ADD_TAG_POS = CREATE_MARKER_POS + 1
        private const val ADD_PERFORMER_POS = ADD_TAG_POS + 1
        private const val ADD_GROUP_POS = ADD_PERFORMER_POS + 1
        private const val SET_STUDIO_POS = ADD_GROUP_POS + 1
        private const val FORCE_TRANSCODE_POS = SET_STUDIO_POS + 1
        private const val FORCE_DIRECT_PLAY_POS = FORCE_TRANSCODE_POS + 1

        val REMOVE_POPUP_ITEM =
            StashPresenter.PopUpItem(
                0L,
                StashApplication.getApplication().getString(R.string.stashapp_actions_remove),
            )
    }
}
