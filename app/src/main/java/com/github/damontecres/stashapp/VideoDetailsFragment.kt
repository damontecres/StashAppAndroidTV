package com.github.damontecres.stashapp

import android.app.Activity
import android.content.Context
import android.content.Intent
import android.graphics.Bitmap
import android.os.Bundle
import android.util.Log
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Toast
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
import androidx.leanback.widget.OnItemViewClickedListener
import androidx.leanback.widget.Presenter
import androidx.leanback.widget.Row
import androidx.leanback.widget.RowPresenter
import androidx.lifecycle.lifecycleScope
import androidx.preference.PreferenceManager
import com.bumptech.glide.Glide
import com.bumptech.glide.request.target.SimpleTarget
import com.bumptech.glide.request.transition.Transition
import com.github.damontecres.stashapp.PlaybackVideoFragment.Companion.coroutineExceptionHandler
import com.github.damontecres.stashapp.actions.AddTagAction
import com.github.damontecres.stashapp.data.DataType
import com.github.damontecres.stashapp.data.Scene
import com.github.damontecres.stashapp.data.Tag
import com.github.damontecres.stashapp.data.fromSlimSceneDataTag
import com.github.damontecres.stashapp.data.fromTagData
import com.github.damontecres.stashapp.presenters.PerformerPresenter
import com.github.damontecres.stashapp.presenters.ScenePresenter
import com.github.damontecres.stashapp.presenters.StashPresenter
import com.github.damontecres.stashapp.presenters.TagPresenter
import kotlinx.coroutines.CoroutineExceptionHandler
import kotlinx.coroutines.launch

/**
 * A wrapper fragment for leanback details screens.
 * It shows a detailed view of video and its metadata plus related videos.
 */
class VideoDetailsFragment : DetailsSupportFragment() {
    private var mSelectedMovie: Scene? = null

    private var performersAdapter: ArrayObjectAdapter = ArrayObjectAdapter(PerformerPresenter())
    private var tagsAdapter: ArrayObjectAdapter = ArrayObjectAdapter(TagPresenter())
    private var actionsAdapter = ArrayObjectAdapter(StashPresenter.SELECTOR)

    private lateinit var mDetailsBackground: DetailsSupportFragmentBackgroundController
    private lateinit var mPresenterSelector: ClassPresenterSelector
    private lateinit var mAdapter: ArrayObjectAdapter
    private lateinit var resultLauncher: ActivityResultLauncher<Intent>

    private val actionAdapter = ArrayObjectAdapter()
    private var position = -1L // The position in the video

    override fun onCreate(savedInstanceState: Bundle?) {
        Log.d(TAG, "onCreate DetailsFragment")
        super.onCreate(savedInstanceState)

        actionsAdapter.add(AddTagAction())

        mDetailsBackground = DetailsSupportFragmentBackgroundController(this)

//        mSelectedMovie = activity!!.intent.getSerializableExtra(DetailsActivity.MOVIE) as Scene
        mSelectedMovie = requireActivity().intent.getParcelableExtra(DetailsActivity.MOVIE)
        if (mSelectedMovie != null) {
            mPresenterSelector = ClassPresenterSelector()
            mAdapter = ArrayObjectAdapter(mPresenterSelector)
            setupDetailsOverviewRow()
            setupDetailsOverviewRowPresenter()
            setupRelatedMovieListRow()
            adapter = mAdapter
            initializeBackground(mSelectedMovie)

            val actionListener =
                OnItemViewClickedListener { viewHolder: Presenter.ViewHolder, item: Any, rowViewHolder: RowPresenter.ViewHolder, row: Row ->
                    if (item is AddTagAction) {
                        val intent = Intent(requireActivity(), SearchForActivity::class.java)
                        intent.putExtra("dataType", DataType.TAG.name)
                        intent.putExtra(SearchForFragment.ID_KEY, ADD_TAG_SEARCH_ID)
                        resultLauncher.launch(intent)
                    }
                }

            onItemViewClickedListener =
                StashItemViewClickListener(requireActivity(), actionListener)
        } else {
            Log.w(TAG, "No movie found in intent")
            val intent = Intent(requireActivity(), MainActivity::class.java)
            startActivity(intent)
        }

        resultLauncher =
            registerForActivityResult(ActivityResultContracts.StartActivityForResult()) { result ->
                if (result.resultCode == Activity.RESULT_OK) {
                    val data: Intent? = result.data
                    val id = data!!.getLongExtra(SearchForFragment.ID_KEY, -1)
                    if (id == ADD_TAG_SEARCH_ID) {
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
                                tagsAdapter.unmodifiableList<Tag>().map { it.id }.toMutableList()
                            tagIds.add(tagId!!.toInt())
                            val mutResult =
                                MutationEngine(requireContext()).setTagsOnScene(
                                    mSelectedMovie!!.id,
                                    tagIds,
                                )
                            val newTags = mutResult?.tags?.map { fromTagData(it.tagData) }
                            val newTagName =
                                newTags?.first { it.id == tagId.toInt() }?.name
                            tagsAdapter.clear()
                            tagsAdapter.addAll(0, newTags)

                            Toast.makeText(
                                requireContext(),
                                "Added tag '$newTagName' to scene",
                                Toast.LENGTH_SHORT,
                            ).show()
                        }
                    } else {
                        position = data.getLongExtra(POSITION_ARG, -1)
                        if (position > 10_000) {
                            // If some of the video played, reset the available actions
                            // This also causes the focused action to default to resume which is an added bonus
                            actionAdapter.clear()
                            actionAdapter.add(Action(ACTION_RESUME_SCENE, "Resume"))
                            actionAdapter.add(Action(ACTION_PLAY_SCENE, "Restart"))

                            val serverPreferences = ServerPreferences(requireContext())
                            if (serverPreferences.trackActivity) {
                                viewLifecycleOwner.lifecycleScope.launch(coroutineExceptionHandler) {
                                    MutationEngine(requireContext(), false).saveSceneActivity(
                                        mSelectedMovie!!.id,
                                        position,
                                    )
                                }
                            }
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
        val queryEngine = QueryEngine(requireContext(), true)
        if (mSelectedMovie != null) {
            viewLifecycleOwner.lifecycleScope.launch {
                val scene =
                    queryEngine.findScenes(sceneIds = listOf(mSelectedMovie!!.id.toInt()))
                        .first()
                if (scene.tags.isNotEmpty()) {
                    tagsAdapter.addAll(0, scene.tags.map { fromSlimSceneDataTag(it) })
                }

                val performerIds =
                    scene.performers.map {
                        it.id.toInt()
                    }
                if (performerIds.isNotEmpty()) {
                    val perfs = queryEngine.findPerformers(performerIds = performerIds)
                    performersAdapter.addAll(0, perfs)
                }
            }
        }
        return super.onCreateView(inflater, container, savedInstanceState)
    }

    private fun initializeBackground(movie: Scene?) {
        mDetailsBackground.enableParallax()

        val screenshotUrl = movie?.screenshotUrl

        if (!screenshotUrl.isNullOrBlank()) {
            val apiKey =
                PreferenceManager.getDefaultSharedPreferences(requireContext())
                    .getString("stashApiKey", "")
            val url = createGlideUrl(screenshotUrl, apiKey)

            Glide.with(requireActivity())
                .asBitmap()
                .centerCrop()
                .error(R.drawable.default_background)
                .load(url)
                .into<SimpleTarget<Bitmap>>(
                    object : SimpleTarget<Bitmap>() {
                        override fun onResourceReady(
                            bitmap: Bitmap,
                            transition: Transition<in Bitmap>?,
                        ) {
                            mDetailsBackground.coverBitmap = bitmap
                            mAdapter.notifyArrayItemRangeChanged(0, mAdapter.size())
                        }
                    },
                )
        }
    }

    private fun setupDetailsOverviewRow() {
        val row = DetailsOverviewRow(mSelectedMovie!!)
        row.imageDrawable =
            ContextCompat.getDrawable(requireActivity(), R.drawable.default_background)
        val width = convertDpToPixel(requireActivity(), DETAIL_THUMB_WIDTH)
        val height = convertDpToPixel(requireActivity(), DETAIL_THUMB_HEIGHT)

        val screenshotUrl = mSelectedMovie?.screenshotUrl
        if (!screenshotUrl.isNullOrBlank()) {
            val apiKey =
                PreferenceManager.getDefaultSharedPreferences(requireContext())
                    .getString("stashApiKey", "")
            val url = createGlideUrl(screenshotUrl, apiKey)
            Glide.with(requireActivity())
                .asBitmap()
                .load(url)
                .centerCrop()
                .error(R.drawable.default_background)
                .into<SimpleTarget<Bitmap>>(
                    object : SimpleTarget<Bitmap>(width, height) {
                        override fun onResourceReady(
                            drawable: Bitmap,
                            transition: Transition<in Bitmap>?,
                        ) {
                            Log.d(TAG, "details overview card image url ready: " + drawable)
                            row.setImageBitmap(requireContext(), drawable)
                            mAdapter.notifyArrayItemRangeChanged(0, mAdapter.size())
                        }
                    },
                )
        }

        val serverPreferences = ServerPreferences(requireContext())
        if (serverPreferences.trackActivity && mSelectedMovie?.resumeTime != null && mSelectedMovie?.resumeTime!! > 0) {
            position = (mSelectedMovie?.resumeTime!! * 1000).toLong()
            actionAdapter.add(Action(ACTION_RESUME_SCENE, "Resume"))
            actionAdapter.add(Action(ACTION_PLAY_SCENE, "Restart"))
        } else {
            actionAdapter.add(
                Action(
                    ACTION_PLAY_SCENE,
                    resources.getString(R.string.play_scene),
                ),
            )
        }

        row.actionsAdapter = actionAdapter

        mAdapter.add(row)
    }

    private fun setupDetailsOverviewRowPresenter() {
        // Set detail background.
        val detailsPresenter = FullWidthDetailsOverviewRowPresenter(DetailsDescriptionPresenter())
        detailsPresenter.backgroundColor =
            ContextCompat.getColor(requireActivity(), R.color.default_card_background)

        // Hook up transition element.
        val sharedElementHelper = FullWidthDetailsOverviewSharedElementHelper()
        sharedElementHelper.setSharedElementEnterTransition(
            activity,
            DetailsActivity.SHARED_ELEMENT_NAME,
        )
        detailsPresenter.setListener(sharedElementHelper)
        detailsPresenter.isParticipatingEntranceTransition = true

        detailsPresenter.onActionClickedListener =
            OnActionClickedListener { action ->
                if (mSelectedMovie != null) {
                    if (action.id in longArrayOf(ACTION_PLAY_SCENE, ACTION_RESUME_SCENE)) {
                        val intent = Intent(requireActivity(), PlaybackActivity::class.java)
                        intent.putExtra(DetailsActivity.MOVIE, mSelectedMovie)
                        if (action.id == ACTION_RESUME_SCENE) {
                            intent.putExtra(POSITION_ARG, position)
                        }
                        resultLauncher.launch(intent)
                    } else {
                        throw IllegalArgumentException("Action $action (id=${action.id} is not supported!")
                    }
                }
            }
        mPresenterSelector.addClassPresenter(DetailsOverviewRow::class.java, detailsPresenter)
    }

    private fun setupRelatedMovieListRow() {
        // TODO related scenes
        mAdapter.add(ListRow(HeaderItem(0, "Performers"), performersAdapter))
        mAdapter.add(ListRow(HeaderItem(1, "Tags"), tagsAdapter))
        mAdapter.add(ListRow(HeaderItem(2, "Actions"), actionsAdapter))
        mPresenterSelector.addClassPresenter(ListRow::class.java, ListRowPresenter())
    }

    private fun convertDpToPixel(
        context: Context,
        dp: Int,
    ): Int {
        val density = context.applicationContext.resources.displayMetrics.density
        return Math.round(dp.toFloat() * density)
    }

    companion object {
        private val TAG = "VideoDetailsFragment"

        private val ACTION_PLAY_SCENE = 1L
        private val ACTION_RESUME_SCENE = 2L

        private val DETAIL_THUMB_WIDTH = ScenePresenter.CARD_WIDTH
        private val DETAIL_THUMB_HEIGHT = ScenePresenter.CARD_HEIGHT

        private val NUM_COLS = 10

        const val POSITION_ARG = "position"

        const val ADD_TAG_SEARCH_ID = 1L
    }
}
