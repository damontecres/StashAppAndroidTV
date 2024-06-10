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
import androidx.leanback.widget.FullWidthDetailsOverviewSharedElementHelper
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
import com.github.damontecres.stashapp.api.fragment.MarkerData
import com.github.damontecres.stashapp.api.fragment.PerformerData
import com.github.damontecres.stashapp.api.fragment.SlimSceneData
import com.github.damontecres.stashapp.api.fragment.TagData
import com.github.damontecres.stashapp.data.DataType
import com.github.damontecres.stashapp.data.Marker
import com.github.damontecres.stashapp.data.OCounter
import com.github.damontecres.stashapp.data.Scene
import com.github.damontecres.stashapp.presenters.ActionPresenter
import com.github.damontecres.stashapp.presenters.CreateMarkerActionPresenter
import com.github.damontecres.stashapp.presenters.DetailsDescriptionPresenter
import com.github.damontecres.stashapp.presenters.GalleryPresenter
import com.github.damontecres.stashapp.presenters.MarkerPresenter
import com.github.damontecres.stashapp.presenters.MoviePresenter
import com.github.damontecres.stashapp.presenters.OCounterPresenter
import com.github.damontecres.stashapp.presenters.PerformerInScenePresenter
import com.github.damontecres.stashapp.presenters.ScenePresenter
import com.github.damontecres.stashapp.presenters.StashPresenter
import com.github.damontecres.stashapp.presenters.StudioPresenter
import com.github.damontecres.stashapp.presenters.TagPresenter
import com.github.damontecres.stashapp.util.GalleryDiffCallback
import com.github.damontecres.stashapp.util.ListRowManager
import com.github.damontecres.stashapp.util.MarkerDiffCallback
import com.github.damontecres.stashapp.util.MovieDiffCallback
import com.github.damontecres.stashapp.util.MutationEngine
import com.github.damontecres.stashapp.util.QueryEngine
import com.github.damontecres.stashapp.util.ServerPreferences
import com.github.damontecres.stashapp.util.StashCoroutineExceptionHandler
import com.github.damontecres.stashapp.util.StashGlide
import com.github.damontecres.stashapp.util.StudioDiffCallback
import com.github.damontecres.stashapp.util.isNotNullOrBlank
import com.github.damontecres.stashapp.util.showSetRatingToast
import com.github.damontecres.stashapp.views.ClassOnItemViewClickedListener
import com.github.damontecres.stashapp.views.StashItemViewClickListener
import kotlinx.coroutines.CoroutineExceptionHandler
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import java.util.concurrent.locks.ReentrantReadWriteLock
import kotlin.math.roundToInt

/**
 * A wrapper fragment for leanback details screens.
 * It shows a detailed view of video and its metadata plus related videos.
 */
class VideoDetailsFragment : DetailsSupportFragment() {
    private var mSelectedMovie: SlimSceneData? = null

    private lateinit var queryEngine: QueryEngine
    private lateinit var mutationEngine: MutationEngine

    private val mPresenterSelector = ClassPresenterSelector()
    private val mAdapter = SparseArrayObjectAdapter(mPresenterSelector)

    private val studioAdapter = ArrayObjectAdapter(StudioPresenter())

    // Presenter is set in fetchData because it requires mSelectedMovie
    private val mPerformersAdapter = ArrayObjectAdapter()
    private val performersRowManager =
        ListRowManager<PerformerData>(
            DataType.PERFORMER,
            ListRowManager.SparseArrayRowModifier(mAdapter, PERFORMER_POS),
            mPerformersAdapter,
        ) { performerIds ->
            val result = mutationEngine.setPerformersOnScene(mSelectedMovie!!.id, performerIds)
            result?.performers?.map { it.performerData }.orEmpty()
        }

    private val tagsRowManager =
        ListRowManager<TagData>(
            DataType.TAG,
            ListRowManager.SparseArrayRowModifier(mAdapter, TAG_POS),
            ArrayObjectAdapter(TagPresenter(TagLongClickCallBack())),
        ) { tagIds ->
            val result = mutationEngine.setTagsOnScene(mSelectedMovie!!.id, tagIds)
            result?.tags?.map { it.tagData }.orEmpty()
        }

    private val markersAdapter = ArrayObjectAdapter(MarkerPresenter(MarkerLongClickCallBack()))
    private val moviesAdapter = ArrayObjectAdapter(MoviePresenter())
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
            DetailsDescriptionPresenter { rating100: Int ->
                viewLifecycleOwner.lifecycleScope.launch(
                    StashCoroutineExceptionHandler(
                        Toast.makeText(
                            requireContext(),
                            "Failed to set rating",
                            Toast.LENGTH_SHORT,
                        ),
                    ),
                ) {
                    MutationEngine(requireContext()).setRating(
                        mSelectedMovie!!.id,
                        rating100,
                    )
                    showSetRatingToast(requireContext(), rating100)
                }
            },
        )

    override fun onCreate(savedInstanceState: Bundle?) {
        Log.d(TAG, "onCreate DetailsFragment")
        super.onCreate(savedInstanceState)

        val lock = ReentrantReadWriteLock()
        queryEngine = QueryEngine(requireContext(), lock = lock)
        mutationEngine = MutationEngine(requireContext(), lock = lock)

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
        mAdapter.set(ACTIONS_POS, ListRow(HeaderItem("Actions"), sceneActionsAdapter))
        mPresenterSelector.addClassPresenter(ListRow::class.java, ListRowPresenter())
        return super.onCreateView(inflater, container, savedInstanceState)
    }

    override fun onViewCreated(
        view: View,
        savedInstanceState: Bundle?,
    ) {
        super.onViewCreated(view, savedInstanceState)

        val sceneId = requireActivity().intent.getStringExtra(VideoDetailsActivity.MOVIE)
        if (sceneId == null) {
            Log.w(TAG, "No scene found in intent")
            val intent = Intent(requireActivity(), MainActivity::class.java)
            startActivity(intent)
        } else {
            fetchData(sceneId)
        }
    }

    override fun onResume() {
        super.onResume()
        if (mSelectedMovie != null) {
            fetchData(mSelectedMovie!!.id)
        }
    }

    private fun fetchData(sceneId: String) {
        viewLifecycleOwner.lifecycleScope.launch(
            StashCoroutineExceptionHandler(
                Toast.makeText(
                    requireContext(),
                    "Failed to load scene",
                    Toast.LENGTH_LONG,
                ),
            ),
        ) {
            mSelectedMovie = queryEngine.getScene(sceneId)
            if (mSelectedMovie != null) {
                mPerformersAdapter.presenterSelector =
                    SinglePresenterSelector(
                        PerformerInScenePresenter(
                            mSelectedMovie!!,
                            PerformerLongClickCallBack(),
                        ),
                    )
            }

            val serverPreferences = ServerPreferences(requireContext())
            // Need to check position because the activity result callback happens before onResume
            if (position <= 0 &&
                serverPreferences.trackActivity &&
                mSelectedMovie?.resume_time != null &&
                mSelectedMovie?.resume_time!! > 0
            ) {
                position = (mSelectedMovie?.resume_time!! * 1000).toLong()
            }
            setupPlayActionsAdapter()

            setupDetailsOverviewRow()

            adapter = mAdapter
            initializeBackground()

            sceneActionsAdapter.set(
                O_COUNTER_POS,
                OCounter(
                    mSelectedMovie!!.id,
                    mSelectedMovie?.o_counter ?: 0,
                ),
            )
            sceneActionsAdapter.set(ADD_TAG_POS, StashAction.ADD_TAG)
            sceneActionsAdapter.set(ADD_PERFORMER_POS, StashAction.ADD_PERFORMER)
            sceneActionsAdapter.set(CREATE_MARKER_POS, CreateMarkerAction(position))
            sceneActionsAdapter.set(FORCE_TRANSCODE_POS, StashAction.FORCE_TRANSCODE)
            sceneActionsAdapter.set(FORCE_DIRECT_PLAY_POS, StashAction.FORCE_DIRECT_PLAY)

            if (mSelectedMovie!!.studio?.studioData != null) {
                studioAdapter.setItems(
                    listOf(mSelectedMovie!!.studio!!.studioData),
                    StudioDiffCallback,
                )
                if (mAdapter.lookup(STUDIO_POS) == null) {
                    mAdapter.set(
                        STUDIO_POS,
                        ListRow(HeaderItem(getString(R.string.stashapp_studio)), studioAdapter),
                    )
                }
            } else {
                mAdapter.clear(STUDIO_POS)
            }

            tagsRowManager.setItems(mSelectedMovie!!.tags.map { it.tagData })

            if (mSelectedMovie!!.scene_markers.isNotEmpty()) {
                if (mAdapter.lookup(MARKER_POS) == null) {
                    mAdapter.set(
                        MARKER_POS,
                        ListRow(HeaderItem(getString(R.string.stashapp_markers)), markersAdapter),
                    )
                }
                markersAdapter.setItems(
                    mSelectedMovie!!.scene_markers.map(::convertMarker),
                    MarkerDiffCallback,
                )
            } else {
                mAdapter.clear(MARKER_POS)
            }

            val performerIds = mSelectedMovie!!.performers.map { it.id }
            Log.v(TAG, "fetchData performerIds=$performerIds")
            if (performerIds.isNotEmpty()) {
                val perfs = queryEngine.findPerformers(performerIds = performerIds)
                performersRowManager.setItems(perfs)
            } else {
                performersRowManager.setItems(listOf())
            }

            if (mSelectedMovie!!.movies.isNotEmpty()) {
                if (mAdapter.lookup(MOVIE_POS) == null) {
                    mAdapter.set(
                        MOVIE_POS,
                        ListRow(
                            HeaderItem(getString(R.string.stashapp_movies)),
                            moviesAdapter,
                        ),
                    )
                }
                val movies = mSelectedMovie!!.movies.map { it.movie.movieData }
                moviesAdapter.setItems(movies, MovieDiffCallback)
            } else {
                mAdapter.clear(MOVIE_POS)
            }

            if (mSelectedMovie!!.galleries.isNotEmpty()) {
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
                        queryEngine.getGalleries(mSelectedMovie!!.galleries.map { it.id })
                    galleriesAdapter.setItems(galleries, GalleryDiffCallback)
                }
            } else {
                mAdapter.clear(GALLERY_POS)
            }
        }
    }

    private fun initializeBackground() {
        if (mDetailsBackground.coverBitmap == null) {
            mDetailsBackground.enableParallax()

            val screenshotUrl = mSelectedMovie!!.paths.screenshot

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

    private fun setupDetailsOverviewRow() {
        val row = detailsOverviewRow ?: DetailsOverviewRow(mSelectedMovie)

        val screenshotUrl = mSelectedMovie?.paths?.screenshot
        if ((detailsOverviewRow == null || mSelectedMovie != row.item) && !screenshotUrl.isNullOrBlank()) {
            row.item = mSelectedMovie
            val width = convertDpToPixel(requireActivity(), DETAIL_THUMB_WIDTH)
            val height = convertDpToPixel(requireActivity(), DETAIL_THUMB_HEIGHT)
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

        row.actionsAdapter = playActionsAdapter

        mAdapter.set(DETAILS_POS, row)
        detailsOverviewRow = row
    }

    private fun setupDetailsOverviewRowPresenter() {
        // Set detail background.
        detailsPresenter.backgroundColor =
            ContextCompat.getColor(requireActivity(), R.color.default_card_background)

        // Hook up transition element.
        val sharedElementHelper = FullWidthDetailsOverviewSharedElementHelper()
        sharedElementHelper.setSharedElementEnterTransition(
            activity,
            VideoDetailsActivity.SHARED_ELEMENT_NAME,
        )
        detailsPresenter.setListener(sharedElementHelper)
        detailsPresenter.isParticipatingEntranceTransition = true

        detailsPresenter.onActionClickedListener =
            OnActionClickedListener { action ->
                if (mSelectedMovie != null) {
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
                            VideoDetailsActivity.MOVIE,
                            Scene.fromSlimSceneData(mSelectedMovie!!),
                        )
                        if (action.id == ACTION_RESUME_SCENE ||
                            action.id == ACTION_TRANSCODE_RESUME_SCENE ||
                            action.id == ACTION_DIRECT_PLAY_RESUME_SCENE
                        ) {
                            intent.putExtra(POSITION_ARG, position)
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
                        StashAction.CREATE_MARKER -> {
                            intent.putExtra(
                                SearchForFragment.TITLE_KEY,
                                "for primary tag for scene marker",
                            )
                            DataType.TAG
                        }

                        else -> throw RuntimeException("Unsupported search for type $action")
                    }
                intent.putExtra("dataType", dataType.name)
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
                mSelectedMovie = mSelectedMovie!!.copy(o_counter = newCounter.count)
                sceneActionsAdapter.set(O_COUNTER_POS, newCounter)
            }
        }
    }

    private fun setupPlayActionsAdapter() {
        if (position > 0) {
            playActionsAdapter.set(0, Action(ACTION_RESUME_SCENE, "Resume"))
            // Force focus to move to Resume
            playActionsAdapter.clear(1)
            playActionsAdapter.set(1, Action(ACTION_PLAY_SCENE, "Restart"))
        } else {
            playActionsAdapter.set(
                0,
                Action(ACTION_PLAY_SCENE, resources.getString(R.string.play_scene)),
            )
            playActionsAdapter.clear(1)
        }
    }

    private inner class ResultCallback : ActivityResultCallback<ActivityResult> {
        override fun onActivityResult(result: ActivityResult) {
            if (result.resultCode == Activity.RESULT_OK) {
                val data: Intent? = result.data
                val id = data!!.getLongExtra(SearchForFragment.ID_KEY, -1)
                if (id == StashAction.ADD_TAG.id) {
                    val tagId = data.getStringExtra(SearchForFragment.RESULT_ID_KEY)!!
                    Log.d(TAG, "Adding tag $tagId to scene ${mSelectedMovie?.id}")
                    viewLifecycleOwner.lifecycleScope.launch(
                        CoroutineExceptionHandler { _, ex ->
                            Log.e(TAG, "Exception setting tags", ex)
                            Toast.makeText(
                                requireContext(),
                                "Failed to add tag: ${ex.message}",
                                Toast.LENGTH_LONG,
                            ).show()
                        },
                    ) {
                        val newTag = tagsRowManager.add(tagId)
                        if (newTag != null) {
                            Toast.makeText(
                                requireContext(),
                                "Added tag '${newTag.name}' to scene",
                                Toast.LENGTH_SHORT,
                            ).show()
                        }
                    }
                } else if (id == StashAction.ADD_PERFORMER.id) {
                    val performerId = data.getStringExtra(SearchForFragment.RESULT_ID_KEY)!!
                    Log.d(TAG, "Adding performer $performerId to scene ${mSelectedMovie?.id}")
                    viewLifecycleOwner.lifecycleScope.launch(
                        CoroutineExceptionHandler { _, ex ->
                            Log.e(TAG, "Exception setting performers", ex)
                            Toast.makeText(
                                requireContext(),
                                "Failed to add performer: ${ex.message}",
                                Toast.LENGTH_LONG,
                            ).show()
                        },
                    ) {
                        val newPerformer = performersRowManager.add(performerId)
                        if (newPerformer != null) {
                            Toast.makeText(
                                requireContext(),
                                "Added performer '${newPerformer.name}' to scene",
                                Toast.LENGTH_SHORT,
                            ).show()
                        }
                    }
                } else if (id == StashAction.CREATE_MARKER.id) {
                    viewLifecycleOwner.lifecycleScope.launch(
                        CoroutineExceptionHandler { _, ex ->
                            Log.e(TAG, "Exception creating marker", ex)
                            Toast.makeText(
                                requireContext(),
                                "Failed to create marker: ${ex.message}",
                                Toast.LENGTH_LONG,
                            ).show()
                        },
                    ) {
                        val tagId = data.getStringExtra(SearchForFragment.RESULT_ID_KEY)!!
                        Log.d(
                            TAG,
                            "Adding marker at $position with tagId=$tagId to scene ${mSelectedMovie?.id}",
                        )
                        val newMarker =
                            MutationEngine(requireContext()).createMarker(
                                mSelectedMovie!!.id,
                                position,
                                tagId,
                            )!!
                        val markers =
                            newMarker.scene.slimSceneData.scene_markers.map(::convertMarker)
                        markersAdapter.setItems(markers, MarkerDiffCallback)
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
                } else {
                    val localPosition = data.getLongExtra(POSITION_RESULT_ARG, -1)
                    if (localPosition >= 0) {
                        sceneActionsAdapter.set(
                            CREATE_MARKER_POS,
                            CreateMarkerAction(localPosition),
                        )
                    }

                    viewLifecycleOwner.lifecycleScope.launch(Dispatchers.IO + StashCoroutineExceptionHandler()) {
                        val serverPreferences = ServerPreferences(requireContext())
                        if (serverPreferences.trackActivity) {
                            Log.v(TAG, "ResultCallback saveSceneActivity start")
                            MutationEngine(requireContext(), false).saveSceneActivity(
                                mSelectedMovie!!.id,
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

    private fun convertMarker(it: SlimSceneData.Scene_marker): MarkerData {
        return MarkerData(
            id = it.id,
            title = it.title,
            created_at = "",
            updated_at = "",
            stream = it.stream,
            screenshot = it.screenshot,
            seconds = it.seconds,
            preview = "",
            primary_tag = MarkerData.Primary_tag("", it.primary_tag.tagData),
            scene = MarkerData.Scene("", mSelectedMovie!!),
            tags = it.tags.map { MarkerData.Tag("", it.tagData) },
            __typename = "",
        )
    }

    private inner class OCounterLongClickCallBack : StashPresenter.LongClickCallBack<OCounter> {
        override fun getPopUpItems(
            context: Context,
            item: OCounter,
        ): List<StashPresenter.PopUpItem> {
            return listOf(
                StashPresenter.PopUpItem(0L, "Increment"),
                StashPresenter.PopUpItem(1L, "Decrement"),
                StashPresenter.PopUpItem(2L, "Reset"),
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
                        val newCount = mutationEngine.incrementOCounter(mSelectedMovie!!.id)
                        sceneActionsAdapter.set(O_COUNTER_POS, newCount)
                        mSelectedMovie = mSelectedMovie!!.copy(o_counter = newCount.count)
                    }

                    1L -> {
                        // Decrement
                        val newCount = mutationEngine.decrementOCounter(mSelectedMovie!!.id)
                        sceneActionsAdapter.set(O_COUNTER_POS, newCount)
                        mSelectedMovie = mSelectedMovie!!.copy(o_counter = newCount.count)
                    }

                    2L -> {
                        // Reset
                        val newCount = mutationEngine.resetOCounter(mSelectedMovie!!.id)
                        mSelectedMovie = mSelectedMovie!!.copy(o_counter = newCount.count)
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
                    val intent = Intent(context, MarkerActivity::class.java)
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
                        val mutResult =
                            MutationEngine(requireContext()).deleteMarker(item.id)
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
                    "Removing performer $performerId to scene ${mSelectedMovie?.id}",
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

    companion object {
        private const val TAG = "VideoDetailsFragment"

        private const val ACTION_PLAY_SCENE = 1L
        private const val ACTION_RESUME_SCENE = 2L
        private const val ACTION_TRANSCODE_RESUME_SCENE = 3L
        private const val ACTION_DIRECT_PLAY_RESUME_SCENE = 4L

        private const val DETAIL_THUMB_WIDTH = ScenePresenter.CARD_WIDTH
        private const val DETAIL_THUMB_HEIGHT = ScenePresenter.CARD_HEIGHT

        const val POSITION_ARG = "position"
        const val POSITION_RESULT_ARG = "position.result"
        const val FORCE_TRANSCODE = "forceTranscode"
        const val FORCE_DIRECT_PLAY = "forceDirectPlay"

        // Row order
        private const val DETAILS_POS = 1
        private const val MARKER_POS = DETAILS_POS + 1
        private const val MOVIE_POS = MARKER_POS + 1
        private const val STUDIO_POS = MOVIE_POS + 1
        private const val PERFORMER_POS = STUDIO_POS + 1
        private const val TAG_POS = PERFORMER_POS + 1
        private const val GALLERY_POS = TAG_POS + 1
        private const val ACTIONS_POS = GALLERY_POS + 1

        // Actions row order
        private const val O_COUNTER_POS = 1
        private const val ADD_TAG_POS = O_COUNTER_POS + 1
        private const val ADD_PERFORMER_POS = ADD_TAG_POS + 1
        private const val CREATE_MARKER_POS = ADD_PERFORMER_POS + 1
        private const val FORCE_TRANSCODE_POS = CREATE_MARKER_POS + 1
        private const val FORCE_DIRECT_PLAY_POS = FORCE_TRANSCODE_POS + 1

        private val REMOVE_POPUP_ITEM = StashPresenter.PopUpItem(0L, "Remove")
    }
}
