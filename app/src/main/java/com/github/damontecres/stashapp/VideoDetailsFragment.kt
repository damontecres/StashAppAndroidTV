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
import androidx.leanback.widget.SparseArrayObjectAdapter
import androidx.lifecycle.lifecycleScope
import com.bumptech.glide.Glide
import com.bumptech.glide.request.target.CustomTarget
import com.bumptech.glide.request.transition.Transition
import com.github.damontecres.stashapp.PlaybackVideoFragment.Companion.coroutineExceptionHandler
import com.github.damontecres.stashapp.actions.StashAction
import com.github.damontecres.stashapp.actions.StashActionClickedListener
import com.github.damontecres.stashapp.api.fragment.MarkerData
import com.github.damontecres.stashapp.api.fragment.PerformerData
import com.github.damontecres.stashapp.api.fragment.SlimSceneData
import com.github.damontecres.stashapp.api.fragment.TagData
import com.github.damontecres.stashapp.data.DataType
import com.github.damontecres.stashapp.data.OCounter
import com.github.damontecres.stashapp.data.Scene
import com.github.damontecres.stashapp.presenters.ActionPresenter
import com.github.damontecres.stashapp.presenters.DetailsDescriptionPresenter
import com.github.damontecres.stashapp.presenters.MarkerPresenter
import com.github.damontecres.stashapp.presenters.MoviePresenter
import com.github.damontecres.stashapp.presenters.OCounterPresenter
import com.github.damontecres.stashapp.presenters.PerformerPresenter
import com.github.damontecres.stashapp.presenters.ScenePresenter
import com.github.damontecres.stashapp.presenters.StashImageCardView
import com.github.damontecres.stashapp.presenters.StashPresenter
import com.github.damontecres.stashapp.presenters.TagPresenter
import com.github.damontecres.stashapp.util.Constants
import com.github.damontecres.stashapp.util.MutationEngine
import com.github.damontecres.stashapp.util.QueryEngine
import com.github.damontecres.stashapp.util.ServerPreferences
import com.github.damontecres.stashapp.util.StashCoroutineExceptionHandler
import com.github.damontecres.stashapp.util.createGlideUrl
import com.github.damontecres.stashapp.util.isNotNullOrBlank
import kotlinx.coroutines.CoroutineExceptionHandler
import kotlinx.coroutines.launch
import kotlin.math.roundToInt

/**
 * A wrapper fragment for leanback details screens.
 * It shows a detailed view of video and its metadata plus related videos.
 */
class VideoDetailsFragment : DetailsSupportFragment() {
    private var mSelectedMovie: SlimSceneData? = null

    private lateinit var performersAdapter: ArrayObjectAdapter
    private lateinit var tagsAdapter: ArrayObjectAdapter
    private lateinit var markersAdapter: ArrayObjectAdapter
    private lateinit var moviesAdapter: ArrayObjectAdapter
    private val sceneActionsAdapter =
        SparseArrayObjectAdapter(
            ClassPresenterSelector().addClassPresenter(
                StashAction::class.java,
                VideoDetailsActionPresenter(),
            ).addClassPresenter(
                OCounter::class.java,
                OCounterPresenter(OCounterLongClickCallBack()),
            ),
        )

    private lateinit var mDetailsBackground: DetailsSupportFragmentBackgroundController
    private val mPresenterSelector = ClassPresenterSelector()
    private val mAdapter = SparseArrayObjectAdapter(mPresenterSelector)
    private lateinit var resultLauncher: ActivityResultLauncher<Intent>

    private val playActionsAdapter = SparseArrayObjectAdapter()
    private var position = -1L // The position in the video
    private val detailsPresenter =
        FullWidthDetailsOverviewRowPresenter(
            DetailsDescriptionPresenter { sceneId: Int, rating100: Int ->
                viewLifecycleOwner.lifecycleScope.launch(
                    StashCoroutineExceptionHandler(
                        Toast.makeText(
                            requireContext(),
                            "Failed to set rating",
                            Toast.LENGTH_SHORT,
                        ),
                    ),
                ) {
                    MutationEngine(requireContext()).setRating(sceneId, rating100)
                    val ratingsAsStars = ServerPreferences(requireContext()).ratingsAsStars
                    val ratingStr =
                        if (ratingsAsStars) {
                            (rating100 / 20.0).toString() + " stars"
                        } else {
                            (rating100 / 10.0).toString()
                        }
                    Toast.makeText(
                        requireContext(),
                        "Set rating to $ratingStr!",
                        Toast.LENGTH_SHORT,
                    ).show()
                }
            },
        )

    override fun onCreate(savedInstanceState: Bundle?) {
        Log.d(TAG, "onCreate DetailsFragment")
        super.onCreate(savedInstanceState)
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
            StashItemViewClickListener(requireActivity(), actionListener)
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
            val queryEngine = QueryEngine(requireContext())
            viewLifecycleOwner.lifecycleScope.launch(
                StashCoroutineExceptionHandler(
                    Toast.makeText(
                        requireContext(),
                        "Failed to load scene",
                        Toast.LENGTH_LONG,
                    ),
                ),
            ) {
                mSelectedMovie = queryEngine.getScene(sceneId.toInt())
                if (mSelectedMovie!!.resume_time != null) {
                    position = (mSelectedMovie!!.resume_time!! * 1000).toLong()
                }
                setupDetailsOverviewRow()

                adapter = mAdapter
                initializeBackground()

                sceneActionsAdapter.set(
                    O_COUNTER_POS,
                    OCounter(
                        mSelectedMovie!!.id.toInt(),
                        mSelectedMovie?.o_counter ?: 0,
                    ),
                )
                sceneActionsAdapter.set(ADD_TAG_POS, StashAction.ADD_TAG)
                sceneActionsAdapter.set(ADD_PERFORMER_POS, StashAction.ADD_PERFORMER)
                sceneActionsAdapter.set(CREATE_MARKER_POS, StashAction.CREATE_MARKER)
                sceneActionsAdapter.set(FORCE_TRANSCODE_POS, StashAction.FORCE_TRANSCODE)

                tagsAdapter =
                    ArrayObjectAdapter(
                        TagPresenter(
                            object :
                                StashPresenter.LongClickCallBack<TagData> {
                                override val popUpItems: List<String>
                                    get() = listOf("Remove")

                                override fun onItemLongClick(
                                    item: TagData,
                                    popUpItemPosition: Int,
                                ) {
                                    if (popUpItemPosition == 0) {
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
                                                    .map { it.id.toInt() }
                                                    .toMutableList()
                                            tagIds.remove(item.id.toInt())
                                            val mutResult =
                                                MutationEngine(requireContext()).setTagsOnScene(
                                                    mSelectedMovie!!.id.toLong(),
                                                    tagIds,
                                                )
                                            val newTags = mutResult?.tags?.map { it.tagData }
                                            tagsAdapter.clear()
                                            tagsAdapter.addAll(0, newTags)
                                            if (tagsAdapter.size() == 0) {
                                                mAdapter.clear(TAG_POS)
                                            } else {
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
                                                "Removed tag '${item.name}' from scene",
                                                Toast.LENGTH_SHORT,
                                            ).show()
                                        }
                                    }
                                }
                            },
                        ),
                    )

                if (mSelectedMovie!!.tags.isNotEmpty()) {
                    mAdapter.set(
                        TAG_POS,
                        ListRow(HeaderItem(getString(R.string.stashapp_tags)), tagsAdapter),
                    )
                    tagsAdapter.addAll(0, mSelectedMovie!!.tags.map { it.tagData })
                }

                markersAdapter =
                    ArrayObjectAdapter(
                        MarkerPresenter(
                            object :
                                StashPresenter.LongClickCallBack<MarkerData> {
                                override val popUpItems: List<String>
                                    get() = listOf("Remove")

                                override fun onItemLongClick(
                                    item: MarkerData,
                                    popUpItemPosition: Int,
                                ) {
                                    if (popUpItemPosition == 0) {
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
                                }
                            },
                        ),
                    )
                if (mSelectedMovie!!.scene_markers.isNotEmpty()) {
                    mAdapter.set(
                        MARKER_POS,
                        ListRow(HeaderItem(getString(R.string.stashapp_markers)), markersAdapter),
                    )
                    markersAdapter.addAll(
                        0,
                        mSelectedMovie!!.scene_markers.map(::convertMarker),
                    )
                }

                performersAdapter =
                    ArrayObjectAdapter(
                        PerformerPresenter(
                            object :
                                StashPresenter.LongClickCallBack<PerformerData> {
                                override val popUpItems: List<String>
                                    get() = listOf("Remove")

                                override fun onItemLongClick(
                                    item: PerformerData,
                                    popUpItemPosition: Int,
                                ) {
                                    if (popUpItemPosition == 0) {
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
                                            val performerIds =
                                                performersAdapter.unmodifiableList<PerformerData>()
                                                    .map { it.id }
                                                    .toMutableList()
                                            performerIds.remove(performerId)
                                            val mutResult =
                                                MutationEngine(requireContext()).setPerformersOnScene(
                                                    mSelectedMovie!!.id.toLong(),
                                                    performerIds.map { it.toInt() },
                                                )
                                            val resultPerformers =
                                                mutResult?.performers?.map { it.performerData }
                                            performersAdapter.clear()
                                            performersAdapter.addAll(0, resultPerformers)
                                            if (performersAdapter.size() == 0) {
                                                mAdapter.clear(PERFORMER_POS)
                                            } else {
                                                mAdapter.set(
                                                    PERFORMER_POS,
                                                    ListRow(
                                                        HeaderItem(getString(R.string.stashapp_performers)),
                                                        performersAdapter,
                                                    ),
                                                )
                                            }

                                            Toast.makeText(
                                                requireContext(),
                                                "Removed performer '${item.name}' from scene",
                                                Toast.LENGTH_SHORT,
                                            ).show()
                                        }
                                    }
                                }
                            },
                        ),
                    )

                val performerIds = mSelectedMovie!!.performers.map { it.id.toInt() }
                if (performerIds.isNotEmpty()) {
                    val perfs = queryEngine.findPerformers(performerIds = performerIds)
                    if (perfs.isNotEmpty()) {
                        mAdapter.set(
                            PERFORMER_POS,
                            ListRow(
                                HeaderItem(getString(R.string.stashapp_performers)),
                                performersAdapter,
                            ),
                        )
                        performersAdapter.addAll(0, perfs)
                    }
                }

                moviesAdapter = ArrayObjectAdapter(MoviePresenter())
                if (mSelectedMovie!!.movies.isNotEmpty()) {
                    val movies = mSelectedMovie!!.movies.map { it.movie.movieData }
                    moviesAdapter.addAll(0, movies)
                    mAdapter.set(
                        MOVIE_POS,
                        ListRow(
                            HeaderItem(getString(R.string.stashapp_movies)),
                            moviesAdapter,
                        ),
                    )
                }
            }
        }
    }

    override fun onResume() {
        super.onResume()

        viewLifecycleOwner.lifecycleScope.launch(StashCoroutineExceptionHandler()) {
            if (mSelectedMovie != null) {
                val queryEngine = QueryEngine(requireContext())
                mSelectedMovie = queryEngine.getScene(mSelectedMovie!!.id.toInt())
                // Refresh the o-counter which could have changed
                sceneActionsAdapter.set(
                    O_COUNTER_POS,
                    OCounter(mSelectedMovie!!.id.toInt(), mSelectedMovie!!.o_counter ?: 0),
                )
            }
        }
    }

    private fun initializeBackground() {
        mDetailsBackground.enableParallax()

        val screenshotUrl = mSelectedMovie!!.paths.screenshot

        if (screenshotUrl.isNotNullOrBlank()) {
            val url = createGlideUrl(screenshotUrl, requireContext())
            Glide.with(requireActivity())
                .asBitmap()
                .centerCrop()
                .load(url)
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

    private fun setupDetailsOverviewRow() {
        val row = DetailsOverviewRow(mSelectedMovie!!)
        val width = convertDpToPixel(requireActivity(), DETAIL_THUMB_WIDTH)
        val height = convertDpToPixel(requireActivity(), DETAIL_THUMB_HEIGHT)

        val screenshotUrl = mSelectedMovie?.paths?.screenshot
        if (!screenshotUrl.isNullOrBlank()) {
            val url = createGlideUrl(screenshotUrl, requireContext())
            Glide.with(requireActivity())
                .load(url)
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

        val serverPreferences = ServerPreferences(requireContext())
        if (serverPreferences.trackActivity && mSelectedMovie?.resume_time != null && mSelectedMovie?.resume_time!! > 0) {
            position = (mSelectedMovie?.resume_time!! * 1000).toLong()
            playActionsAdapter.set(0, Action(ACTION_RESUME_SCENE, "Resume"))
            playActionsAdapter.set(1, Action(ACTION_PLAY_SCENE, "Restart"))
        } else {
            playActionsAdapter.set(
                0,
                Action(
                    ACTION_PLAY_SCENE,
                    resources.getString(R.string.play_scene),
                ),
            )
            playActionsAdapter.clear(1)
        }

        row.actionsAdapter = playActionsAdapter

        mAdapter.set(DETAILS_POS, row)
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
                        )
                    ) {
                        val intent = Intent(requireActivity(), PlaybackActivity::class.java)
                        intent.putExtra(
                            VideoDetailsActivity.MOVIE,
                            Scene.fromSlimSceneData(mSelectedMovie!!),
                        )
                        if (action.id == ACTION_RESUME_SCENE || action.id == ACTION_TRANSCODE_RESUME_SCENE) {
                            intent.putExtra(POSITION_ARG, position)
                        }
                        if (action.id == ACTION_TRANSCODE_RESUME_SCENE) {
                            intent.putExtra(FORCE_TRANSCODE, true)
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
                val mutationEngine = MutationEngine(requireContext())
                val newCounter = mutationEngine.incrementOCounter(counter.sceneId)
                mSelectedMovie = mSelectedMovie!!.copy(o_counter = newCounter.count)
                sceneActionsAdapter.set(O_COUNTER_POS, newCounter)
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
                        val tagIds =
                            tagsAdapter.unmodifiableList<TagData>().map { it.id.toInt() }
                                .toMutableList()
                        tagIds.add(tagId!!.toInt())
                        val mutResult =
                            MutationEngine(requireContext()).setTagsOnScene(
                                mSelectedMovie!!.id.toLong(),
                                tagIds,
                            )
                        val newTags = mutResult?.tags?.map { it.tagData }
                        val newTagName =
                            newTags?.first { it.id == tagId }?.name
                        tagsAdapter.clear()
                        tagsAdapter.addAll(0, newTags)
                        mAdapter.set(
                            TAG_POS,
                            ListRow(HeaderItem(getString(R.string.stashapp_tags)), tagsAdapter),
                        )

                        Toast.makeText(
                            requireContext(),
                            "Added tag '$newTagName' to scene",
                            Toast.LENGTH_SHORT,
                        ).show()
                    }
                } else if (id == StashAction.ADD_PERFORMER.id) {
                    val performerId = data.getStringExtra(SearchForFragment.RESULT_ID_KEY)
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
                        val performerIds =
                            performersAdapter.unmodifiableList<PerformerData>().map { it.id }
                                .toMutableList()
                        performerIds.add(performerId.toString())
                        val mutResult =
                            MutationEngine(requireContext()).setPerformersOnScene(
                                mSelectedMovie!!.id.toLong(),
                                performerIds.map { it.toInt() },
                            )
                        val resultPerformers = mutResult?.performers?.map { it.performerData }
                        val newPerformer =
                            resultPerformers?.first { it.id == performerId }
                        performersAdapter.clear()
                        performersAdapter.addAll(0, resultPerformers)
                        mAdapter.set(
                            PERFORMER_POS,
                            ListRow(
                                HeaderItem(getString(R.string.stashapp_performers)),
                                performersAdapter,
                            ),
                        )

                        Toast.makeText(
                            requireContext(),
                            "Added performer '${newPerformer?.name}' to scene",
                            Toast.LENGTH_SHORT,
                        ).show()
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
                        markersAdapter.clear()
                        markersAdapter.addAll(0, markers)
                        mAdapter.set(
                            MARKER_POS,
                            ListRow(
                                HeaderItem(getString(R.string.stashapp_markers)),
                                markersAdapter,
                            ),
                        )
                        Toast.makeText(
                            requireContext(),
                            "Created a new marker with primary tag '${newMarker.primary_tag.tagData.name}'",
                            Toast.LENGTH_SHORT,
                        ).show()
                    }
                } else {
                    position = data.getLongExtra(POSITION_ARG, -1)
                    if (position >= 0) {
                        sceneActionsAdapter.notifyItemRangeChanged(CREATE_MARKER_POS, 1)
                    }
                    if (position > 10_000) {
                        // If some of the video played, reset the available actions
                        // This also causes the focused action to default to resume which is an added bonus
                        playActionsAdapter.set(0, Action(ACTION_RESUME_SCENE, "Resume"))
                        playActionsAdapter.set(1, Action(ACTION_PLAY_SCENE, "Restart"))

                        val serverPreferences = ServerPreferences(requireContext())
                        if (serverPreferences.trackActivity) {
                            viewLifecycleOwner.lifecycleScope.launch(coroutineExceptionHandler) {
                                MutationEngine(requireContext(), false).saveSceneActivity(
                                    mSelectedMovie!!.id.toLong(),
                                    position,
                                )
                            }
                        }
                    }
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
            tags = emptyList(),
            __typename = "",
        )
    }

    private inner class VideoDetailsActionPresenter : StashPresenter<StashAction>() {
        override fun doOnBindViewHolder(
            cardView: StashImageCardView,
            item: StashAction,
        ) {
            cardView.titleText = item.actionName
            if (item == StashAction.CREATE_MARKER) {
                cardView.contentText =
                    if (position >= 0) {
                        Constants.durationToString(position / 1000.0)
                    } else {
                        null
                    }
            }
            cardView.setMainImageDimensions(ActionPresenter.CARD_WIDTH, ActionPresenter.CARD_HEIGHT)
        }
    }

    private inner class OCounterLongClickCallBack : StashPresenter.LongClickCallBack<OCounter> {
        override val popUpItems: List<String>
            get() = listOf("Decrement", "Reset")

        override fun onItemLongClick(
            item: OCounter,
            popUpItemPosition: Int,
        ) {
            val mutationEngine = MutationEngine(requireContext())
            viewLifecycleOwner.lifecycleScope.launch(
                StashCoroutineExceptionHandler(
                    Toast.makeText(
                        requireContext(),
                        getString(R.string.failed_o_counter),
                        Toast.LENGTH_SHORT,
                    ),
                ),
            ) {
                when (popUpItemPosition) {
                    0 -> {
                        // Decrement
                        val newCount = mutationEngine.decrementOCounter(mSelectedMovie!!.id.toInt())
                        sceneActionsAdapter.set(O_COUNTER_POS, newCount)
                        mSelectedMovie = mSelectedMovie!!.copy(o_counter = newCount.count)
                    }

                    1 -> {
                        // Reset
                        val newCount = mutationEngine.resetOCounter(mSelectedMovie!!.id.toInt())
                        mSelectedMovie = mSelectedMovie!!.copy(o_counter = newCount.count)
                        sceneActionsAdapter.set(O_COUNTER_POS, newCount)
                    }

                    else ->
                        Log.w(
                            TAG,
                            "Unknown position for OCounterLongClickCallBack: $popUpItemPosition",
                        )
                }
            }
        }
    }

    companion object {
        private const val TAG = "VideoDetailsFragment"

        private const val ACTION_PLAY_SCENE = 1L
        private const val ACTION_RESUME_SCENE = 2L
        private const val ACTION_TRANSCODE_RESUME_SCENE = 3L

        private const val DETAIL_THUMB_WIDTH = ScenePresenter.CARD_WIDTH
        private const val DETAIL_THUMB_HEIGHT = ScenePresenter.CARD_HEIGHT

        const val POSITION_ARG = "position"
        const val FORCE_TRANSCODE = "forceTranscode"

        // Row order
        private const val DETAILS_POS = 1
        private const val MARKER_POS = DETAILS_POS + 1
        private const val MOVIE_POS = MARKER_POS + 1
        private const val PERFORMER_POS = MOVIE_POS + 1
        private const val TAG_POS = PERFORMER_POS + 1
        private const val ACTIONS_POS = TAG_POS + 1

        // Actions row order
        private const val O_COUNTER_POS = 1
        private const val ADD_TAG_POS = O_COUNTER_POS + 1
        private const val ADD_PERFORMER_POS = ADD_TAG_POS + 1
        private const val CREATE_MARKER_POS = ADD_PERFORMER_POS + 1
        private const val FORCE_TRANSCODE_POS = CREATE_MARKER_POS + 1
    }
}
