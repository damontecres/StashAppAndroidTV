package com.github.damontecres.stashapp.image

import android.os.Bundle
import android.util.Log
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Button
import android.widget.Toast
import androidx.core.content.ContextCompat
import androidx.fragment.app.activityViewModels
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
import androidx.leanback.widget.Presenter
import androidx.leanback.widget.Presenter.ViewHolder
import androidx.leanback.widget.SparseArrayObjectAdapter
import androidx.lifecycle.lifecycleScope
import androidx.preference.PreferenceManager
import com.github.damontecres.stashapp.R
import com.github.damontecres.stashapp.SearchForFragment
import com.github.damontecres.stashapp.StashApplication
import com.github.damontecres.stashapp.actions.StashAction
import com.github.damontecres.stashapp.actions.StashActionClickedListener
import com.github.damontecres.stashapp.api.fragment.GalleryData
import com.github.damontecres.stashapp.api.fragment.ImageData
import com.github.damontecres.stashapp.api.fragment.PerformerData
import com.github.damontecres.stashapp.api.fragment.StudioData
import com.github.damontecres.stashapp.api.fragment.TagData
import com.github.damontecres.stashapp.data.DataType
import com.github.damontecres.stashapp.data.OCounter
import com.github.damontecres.stashapp.navigation.Destination
import com.github.damontecres.stashapp.navigation.NavigationOnItemViewClickedListener
import com.github.damontecres.stashapp.presenters.ActionPresenter
import com.github.damontecres.stashapp.presenters.GalleryPresenter
import com.github.damontecres.stashapp.presenters.OCounterPresenter
import com.github.damontecres.stashapp.presenters.PerformerInScenePresenter
import com.github.damontecres.stashapp.presenters.PerformerPresenter
import com.github.damontecres.stashapp.presenters.StudioPresenter
import com.github.damontecres.stashapp.presenters.TagPresenter
import com.github.damontecres.stashapp.util.ListRowManager
import com.github.damontecres.stashapp.util.MutationEngine
import com.github.damontecres.stashapp.util.QueryEngine
import com.github.damontecres.stashapp.util.StashCoroutineExceptionHandler
import com.github.damontecres.stashapp.util.configRowManager
import com.github.damontecres.stashapp.util.createOCounterLongClickCallBack
import com.github.damontecres.stashapp.util.getDataType
import com.github.damontecres.stashapp.util.height
import com.github.damontecres.stashapp.util.isImageClip
import com.github.damontecres.stashapp.util.isNotNullOrBlank
import com.github.damontecres.stashapp.util.joinNotNullOrBlank
import com.github.damontecres.stashapp.util.name
import com.github.damontecres.stashapp.util.readOnlyModeDisabled
import com.github.damontecres.stashapp.util.readOnlyModeEnabled
import com.github.damontecres.stashapp.util.showSetRatingToast
import com.github.damontecres.stashapp.util.titleOrFilename
import com.github.damontecres.stashapp.util.width
import com.github.damontecres.stashapp.views.ClassOnItemViewClickedListener
import com.github.damontecres.stashapp.views.StashOnFocusChangeListener
import com.github.damontecres.stashapp.views.StashRatingBar
import com.github.damontecres.stashapp.views.formatBytes
import com.github.damontecres.stashapp.views.models.ImageViewModel
import com.github.damontecres.stashapp.views.models.ServerViewModel
import com.github.damontecres.stashapp.views.parseTimeToString
import kotlinx.coroutines.launch

/**
 * An overlay for details about an image
 */
class ImageDetailsFragment : DetailsSupportFragment() {
    private val serverViewModel: ServerViewModel by activityViewModels()
    private val viewModel: ImageViewModel by viewModels(ownerProducer = { requireParentFragment() })

    private lateinit var queryEngine: QueryEngine
    private lateinit var mutationEngine: MutationEngine

    private var detailsPresenter: FullWidthDetailsOverviewRowPresenter? = null
    private val mAdapter = SparseArrayObjectAdapter()

    private lateinit var firstButton: Button

    private val itemPresenter = ClassPresenterSelector()

    /**
     * The bottom row of actions that can be performed on the image (add tags, etc)
     */
    private val itemActionsAdapter = SparseArrayObjectAdapter(itemPresenter)

    private val performersRowManager =
        ListRowManager<PerformerData>(
            DataType.PERFORMER,
            ListRowManager.SparseArrayRowModifier(mAdapter, PERFORMER_POS),
            ArrayObjectAdapter(),
        ) { performerIds ->
            val imageId = viewModel.imageId.value!!
            val result = mutationEngine.updateImage(imageId, performerIds = performerIds)
            result?.performers?.map { it.performerData }.orEmpty()
        }

    private val tagsRowManager =
        ListRowManager<TagData>(
            DataType.TAG,
            ListRowManager.SparseArrayRowModifier(mAdapter, TAG_POS),
            ArrayObjectAdapter(),
        ) { tagIds ->
            val imageId = viewModel.imageId.value!!
            val result = mutationEngine.updateImage(imageId, tagIds = tagIds)
            result?.tags?.map { it.tagData }.orEmpty()
        }

    private val galleriesRowManager =
        ListRowManager<GalleryData>(
            DataType.GALLERY,
            ListRowManager.SparseArrayRowModifier(mAdapter, GALLERY_POS),
            ArrayObjectAdapter(),
        ) { galleryIds ->
            val imageId = viewModel.imageId.value!!
            val result = mutationEngine.updateImage(imageId, galleryIds = galleryIds)
            result?.galleries?.map { it.galleryData }.orEmpty()
        }

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

            val imageId = viewModel.imageId.value!!
            val result =
                mutationEngine.updateImage(imageId, studioId = newStudioId)
            val newStudio = result?.studio?.studioData
            if (newStudio != null) {
                listOf(newStudio)
            } else {
                listOf()
            }
        }

    override fun onInflateTitleView(
        inflater: LayoutInflater?,
        parent: ViewGroup?,
        savedInstanceState: Bundle?,
    ): View? = null

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        itemPresenter.addClassPresenter(
            StashAction::class.java,
            ActionPresenter(serverViewModel.requireServer()),
        )
        val detailsBackground = DetailsSupportFragmentBackgroundController(this)
        detailsBackground.enableParallax()

        // Since this fragment is a child of ImageFragment, need to explicitly use the activity's fragment manager
        requireActivity().supportFragmentManager.setFragmentResultListener(
            ImageDetailsFragment::class.simpleName!!,
            this,
        ) { _, bundle ->
            val sourceId = bundle.getLong(SearchForFragment.RESULT_ID_KEY)
            val dataType = bundle.getDataType()
            val newId = bundle.getString(SearchForFragment.RESULT_ITEM_ID_KEY)
            Log.v(TAG, "Adding $dataType: $newId")
            if (newId != null) {
                viewLifecycleOwner.lifecycleScope.launch(StashCoroutineExceptionHandler()) {
                    when (dataType) {
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
                            val newTag = tagsRowManager.add(newId)
                            if (newTag != null) {
                                Toast
                                    .makeText(
                                        requireContext(),
                                        "Added tag '${newTag.name}' to scene",
                                        Toast.LENGTH_SHORT,
                                    ).show()
                            }
                        }

                        DataType.GALLERY -> {
                            val newGallery = galleriesRowManager.add(newId)
                            if (newGallery != null) {
                                Toast
                                    .makeText(
                                        requireContext(),
                                        "Added gallery '${newGallery.name}'",
                                        Toast.LENGTH_SHORT,
                                    ).show()
                            }
                        }

                        DataType.GROUP, DataType.SCENE, DataType.MARKER, DataType.IMAGE -> throw IllegalArgumentException()
                    }
                }
            }
        }
    }

    override fun onViewCreated(
        view: View,
        savedInstanceState: Bundle?,
    ) {
        super.onViewCreated(view, savedInstanceState)
        val server = serverViewModel.requireServer()
        queryEngine = QueryEngine(server)
        mutationEngine = MutationEngine(server)

        configRowManager(
            server,
            { viewLifecycleOwner.lifecycleScope },
            tagsRowManager,
            ::TagPresenter,
        )
        configRowManager(
            server,
            { viewLifecycleOwner.lifecycleScope },
            galleriesRowManager,
            ::GalleryPresenter,
        )
        configRowManager(
            server,
            { viewLifecycleOwner.lifecycleScope },
            studioRowManager,
            ::StudioPresenter,
        )

        val detailsActionsAdapter = ArrayObjectAdapter(DetailsActionsPresenter())
        detailsPresenter =
            FullWidthDetailsOverviewRowPresenter(ImageDetailsRowPresenter()).apply {
                actionsBackgroundColor =
                    ContextCompat.getColor(
                        requireActivity(),
                        R.color.transparent_black_25,
                    )
                backgroundColor =
                    ContextCompat.getColor(
                        requireActivity(),
                        R.color.transparent_default_card_background_75,
                    )

                onActionClickedListener =
                    OnActionClickedListener { action ->
                        if (action.id.toInt() == R.string.play_slideshow || action.id.toInt() == R.string.stop_slideshow) {
                            Log.v(TAG, "Clicked play/stop slideshow")
                            if (viewModel.slideshow.value!!) {
                                detailsActionsAdapter.replace(
                                    detailsActionsAdapter.size() - 1,
                                    Action(R.string.play_slideshow.toLong()),
                                )
                                viewModel.stopSlideshow()
                            } else {
                                // Start slideshow
                                detailsActionsAdapter.replace(
                                    detailsActionsAdapter.size() - 1,
                                    Action(R.string.stop_slideshow.toLong()),
                                )
                                (requireParentFragment() as ImageFragment).hideOverlay()
                                viewModel.startSlideshow()
                            }
                            detailsActionsAdapter.notifyItemRangeChanged(
                                detailsActionsAdapter.size() - 1,
                                1,
                            )
                        } else if (action.id.toInt() == R.string.apply_filters) {
                            (requireParentFragment() as ImageFragment).showFilterOverlay()
                        }
                        val controller = viewModel.imageController
                        if (controller != null) {
                            when (action.id.toInt()) {
                                R.string.fa_rotate_left -> controller.rotateLeft()
                                R.string.fa_rotate_right -> controller.rotateRight()
                                R.string.fa_magnifying_glass_plus -> controller.zoomIn()
                                R.string.fa_magnifying_glass_minus -> controller.zoomOut()
                                R.string.fa_arrow_right_arrow_left -> controller.flip()
                                R.string.stashapp_effect_filters_reset_transforms -> controller.reset()
                            }
                        }
                        val videoController = viewModel.videoController
                        if (videoController != null) {
                            when (action.id.toInt()) {
                                R.string.fa_play -> {
                                    videoController.play()
                                    detailsActionsAdapter.replace(
                                        0,
                                        Action(R.string.fa_pause.toLong()),
                                    )
                                }

                                R.string.fa_pause -> {
                                    videoController.pause()
                                    detailsActionsAdapter.replace(
                                        0,
                                        Action(R.string.fa_play.toLong()),
                                    )
                                }
                            }
                        }
                    }
            }
        mAdapter.presenterSelector =
            ClassPresenterSelector()
                .addClassPresenter(ListRow::class.java, ListRowPresenter())
                .addClassPresenter(DetailsOverviewRow::class.java, detailsPresenter)

        if (readOnlyModeDisabled()) {
            mAdapter.set(
                ITEM_ACTIONS_POS,
                ListRow(HeaderItem(getString(R.string.stashapp_actions_name)), itemActionsAdapter),
            )
        }
        itemActionsAdapter.set(ADD_TAG_POS, StashAction.ADD_TAG)
        itemActionsAdapter.set(ADD_PERFORMER_POS, StashAction.ADD_PERFORMER)
        itemActionsAdapter.set(ADD_GALLERY_POS, StashAction.ADD_GALLERY)
        itemActionsAdapter.set(SET_STUDIO_POS, StashAction.SET_STUDIO)

        val actionListener = ItemActionListener()
        onItemViewClickedListener =
            ClassOnItemViewClickedListener(NavigationOnItemViewClickedListener(serverViewModel.navigationManager))
                .addListenerForClass(StashAction::class.java) { item ->
                    actionListener.onClicked(item)
                }.addListenerForClass(Action::class.java) { _ ->
                    // no-op, detailsPresenter.onActionClickedListener will handle
                }.addListenerForClass(OCounter::class.java) { oCounter ->
                    actionListener.incrementOCounter(oCounter)
                }

        adapter = mAdapter

        viewModel.image.observe(viewLifecycleOwner) { newImage ->

            val detailsRow = DetailsOverviewRow(newImage)

            detailsActionsAdapter.clear()
            if (newImage.isImageClip) {
                detailsActionsAdapter.add(Action(R.string.fa_pause.toLong()))
            } else {
                detailsActionsAdapter.add(Action(R.string.fa_rotate_left.toLong()))
                detailsActionsAdapter.add(Action(R.string.fa_rotate_right.toLong()))
                detailsActionsAdapter.add(Action(R.string.fa_magnifying_glass_plus.toLong()))
                detailsActionsAdapter.add(Action(R.string.fa_magnifying_glass_minus.toLong()))
                detailsActionsAdapter.add(Action(R.string.fa_arrow_right_arrow_left.toLong()))
                detailsActionsAdapter.add(Action(R.string.apply_filters.toLong()))
                detailsActionsAdapter.add(Action(R.string.stashapp_effect_filters_reset_transforms.toLong()))
            }
            if (viewModel.slideshow.value!!) {
                detailsActionsAdapter.add(Action(R.string.stop_slideshow.toLong()))
            } else {
                detailsActionsAdapter.add(Action(R.string.play_slideshow.toLong()))
            }
            detailsRow.actionsAdapter = detailsActionsAdapter

            mAdapter.set(DETAILS_POS, detailsRow)

            if (newImage.date.isNotNullOrBlank()) {
                configRowManager(
                    server,
                    { viewLifecycleOwner.lifecycleScope },
                    performersRowManager,
                ) { server, callback ->
                    PerformerInScenePresenter(server, newImage.date, callback)
                }
            } else {
                configRowManager(
                    server,
                    { viewLifecycleOwner.lifecycleScope },
                    performersRowManager,
                    ::PerformerPresenter,
                )
            }

            itemPresenter.addClassPresenter(
                OCounter::class.java,
                OCounterPresenter(
                    server,
                    createOCounterLongClickCallBack(
                        DataType.IMAGE,
                        newImage.id,
                        mutationEngine,
                        viewLifecycleOwner.lifecycleScope,
                    ) { newCount ->
                        itemActionsAdapter.set(O_COUNTER_POS, newCount)
                    },
                ),
            )

            itemActionsAdapter.set(
                O_COUNTER_POS,
                OCounter(newImage.id, newImage.o_counter ?: 0),
            )

            viewLifecycleOwner.lifecycleScope.launch(StashCoroutineExceptionHandler()) {
                val extraImageData = queryEngine.getImageExtra(newImage.id)
                tagsRowManager.setItems(extraImageData?.tags?.map { it.tagData }.orEmpty())
                performersRowManager.setItems(extraImageData?.performers?.map { it.performerData }.orEmpty())
                galleriesRowManager.setItems(extraImageData?.galleries?.map { it.galleryData }.orEmpty())
                if (extraImageData?.studio != null) {
                    studioRowManager.setItems(listOf(extraImageData.studio.studioData))
                } else {
                    studioRowManager.clear()
                }
            }
        }
    }

    /**
     * Focus on the top button in the fragment, ie scroll to the top
     */
    fun requestFocus() {
        // TODO this is not very reliable
        if (this::firstButton.isInitialized) {
            firstButton.requestFocus()
        }
        showTitle(true)
    }

    override fun onDestroyView() {
        detailsPresenter?.onActionClickedListener = null
        detailsPresenter = null
        onItemViewClickedListener = null
        super.onDestroyView()
    }

    private inner class ImageDetailsRowPresenter : AbstractDetailsDescriptionPresenter() {
        override fun onBindDescription(
            vh: ViewHolder,
            item: Any,
        ) {
            val context = vh.view.context
            val image = item as ImageData
            vh.title.text = image.titleOrFilename
            vh.subtitle.text =
                listOf(
                    image.date,
                    if (image.visual_files.isNotEmpty()) {
                        val imageFile = image.visual_files.first()
                        val imageHeight = imageFile.height
                        val imageWidth = imageFile.width
                        if (imageHeight != null && imageWidth != null) {
                            "${context.getString(R.string.stashapp_dimensions)}: ${imageWidth}x$imageHeight"
                        } else {
                            null
                        }
                    } else {
                        null
                    },
                ).joinNotNullOrBlank(" - ")

            val ratingBar = vh.view.findViewById<StashRatingBar>(R.id.rating_bar)
            ratingBar.configure(serverViewModel.requireServer())
            ratingBar.rating100 = image.rating100 ?: 0
            if (readOnlyModeEnabled()) {
                ratingBar.disable()
            } else {
                ratingBar.setRatingCallback { rating100 ->
                    viewLifecycleOwner.lifecycleScope.launch(StashCoroutineExceptionHandler(true)) {
                        val result = mutationEngine.updateImage(image.id, rating100 = rating100)
                        ratingBar.rating100 = result?.rating100 ?: 0
                        showSetRatingToast(
                            requireContext(),
                            rating100,
                            serverViewModel.requireServer().serverPreferences.ratingsAsStars,
                        )
                    }
                }
            }

            val preferences = PreferenceManager.getDefaultSharedPreferences(context)
            val showDebug =
                preferences.getBoolean(getString(R.string.pref_key_show_playback_debug_info), false)

            val body =
                buildList {
                    if (showDebug) {
                        add("Image ID: ${image.id}")
                        add("Image ${viewModel.currentPosition.value} of ${viewModel.totalCount.value}")
                        val size =
                            formatBytes(
                                image.visual_files
                                    .firstOrNull()
                                    ?.onBaseFile
                                    ?.size
                                    ?.toString()
                                    ?.toIntOrNull() ?: 0,
                            )
                        add("Size: $size")
                    }
                    if (image.photographer.isNotNullOrBlank()) {
                        add("")
                        add("${context.getString(R.string.stashapp_photographer)}: ${image.photographer}")
                    }
                    if (image.details.isNotNullOrBlank()) {
                        add("")
                        add(image.details)
                    }
                    add("")
                    val createdAt =
                        context.getString(R.string.stashapp_created_at) + ": " +
                            parseTimeToString(
                                image.created_at,
                            )
                    add(createdAt)
                    val updatedAt =
                        context.getString(R.string.stashapp_updated_at) + ": " +
                            parseTimeToString(
                                image.updated_at,
                            )
                    add(updatedAt)
                }.joinToString("\n")
            vh.body.text = body
        }
    }

    companion object {
        private const val TAG = "ImageDetailsFragment"

        private const val DETAILS_POS = 1
        private const val STUDIO_POS = DETAILS_POS + 1
        private const val GALLERY_POS = STUDIO_POS + 1
        private const val TAG_POS = GALLERY_POS + 1
        private const val PERFORMER_POS = TAG_POS + 1
        private const val ITEM_ACTIONS_POS = PERFORMER_POS + 1

        // Actions row order
        private const val O_COUNTER_POS = 1
        private const val ADD_TAG_POS = O_COUNTER_POS + 1
        private const val ADD_PERFORMER_POS = ADD_TAG_POS + 1
        private const val ADD_GALLERY_POS = ADD_PERFORMER_POS + 1
        private const val SET_STUDIO_POS = ADD_GALLERY_POS + 1
    }

    private class ActionViewHolder(
        button: Button,
    ) : ViewHolder(button) {
        var mAction: Long? = null
        var mButton: Button = button
    }

    private class DetailsActionsPresenter : Presenter() {
        override fun onCreateViewHolder(parent: ViewGroup): ViewHolder {
            val view =
                LayoutInflater
                    .from(parent.context)
                    .inflate(R.layout.image_action_button, parent, false) as Button
            view.onFocusChangeListener = StashOnFocusChangeListener(parent.context)
            return ActionViewHolder(view)
        }

        override fun onBindViewHolder(
            viewHolder: ViewHolder,
            item: Any,
        ) {
            val action = item as Action
            val vh = viewHolder as ActionViewHolder
            vh.mAction = action.id
            if (action.id.toInt() in
                setOf(
                    R.string.apply_filters,
                    R.string.stashapp_effect_filters_reset_transforms,
                    R.string.play_slideshow,
                    R.string.stop_slideshow,
                )
            ) {
                vh.mButton.typeface = null
            } else if (vh.mButton.typeface == null) {
                vh.mButton.typeface = StashApplication.getFont(R.font.fa_solid_900)
            }
            vh.mButton.text = vh.view.context.getString(action.id.toInt())
//            if (action.id.toInt() == R.string.fa_rotate_left) {
//                firstButton = vh.mButton
//            }
        }

        override fun onUnbindViewHolder(viewHolder: ViewHolder) {
            val vh = viewHolder as ActionViewHolder
            vh.mAction = null
            vh.mButton.text = null
        }
    }

    private inner class ItemActionListener : StashActionClickedListener {
        override fun onClicked(action: StashAction) {
            if (action in StashAction.SEARCH_FOR_ACTIONS) {
                val dataType =
                    when (action) {
                        StashAction.ADD_TAG -> DataType.TAG
                        StashAction.ADD_PERFORMER -> DataType.PERFORMER
                        StashAction.SET_STUDIO -> DataType.STUDIO
                        StashAction.ADD_GALLERY -> DataType.GALLERY

                        else -> throw RuntimeException("Unsupported search for type $action")
                    }
                serverViewModel.navigationManager.navigate(
                    Destination.SearchFor(
                        ImageDetailsFragment::class.simpleName!!,
                        0L,
                        dataType,
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
                val newCounter = mutationEngine.incrementImageOCounter(counter.id)
                itemActionsAdapter.set(O_COUNTER_POS, newCounter)
            }
        }
    }
}
