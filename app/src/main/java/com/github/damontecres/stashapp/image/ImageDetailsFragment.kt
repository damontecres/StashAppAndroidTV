package com.github.damontecres.stashapp.image

import android.content.Context
import android.content.Intent
import android.os.Bundle
import android.util.Log
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Button
import android.widget.Toast
import androidx.activity.result.ActivityResultLauncher
import androidx.core.content.ContextCompat
import androidx.fragment.app.activityViewModels
import androidx.leanback.app.DetailsSupportFragment
import androidx.leanback.app.DetailsSupportFragmentBackgroundController
import androidx.leanback.widget.AbstractDetailsDescriptionPresenter
import androidx.leanback.widget.Action
import androidx.leanback.widget.ArrayObjectAdapter
import androidx.leanback.widget.ClassPresenterSelector
import androidx.leanback.widget.DetailsOverviewRow
import androidx.leanback.widget.FullWidthDetailsOverviewRowPresenter
import androidx.leanback.widget.ListRow
import androidx.leanback.widget.ListRowPresenter
import androidx.leanback.widget.OnActionClickedListener
import androidx.leanback.widget.Presenter
import androidx.leanback.widget.Presenter.ViewHolder
import androidx.leanback.widget.SinglePresenterSelector
import androidx.leanback.widget.SparseArrayObjectAdapter
import androidx.lifecycle.lifecycleScope
import com.github.damontecres.stashapp.R
import com.github.damontecres.stashapp.SceneDetailsFragment.Companion.REMOVE_POPUP_ITEM
import com.github.damontecres.stashapp.api.fragment.GalleryData
import com.github.damontecres.stashapp.api.fragment.ImageData
import com.github.damontecres.stashapp.api.fragment.PerformerData
import com.github.damontecres.stashapp.api.fragment.StudioData
import com.github.damontecres.stashapp.api.fragment.TagData
import com.github.damontecres.stashapp.data.DataType
import com.github.damontecres.stashapp.presenters.GalleryPresenter
import com.github.damontecres.stashapp.presenters.PerformerInScenePresenter
import com.github.damontecres.stashapp.presenters.PerformerPresenter
import com.github.damontecres.stashapp.presenters.StashPresenter
import com.github.damontecres.stashapp.presenters.StudioPresenter
import com.github.damontecres.stashapp.presenters.TagPresenter
import com.github.damontecres.stashapp.util.ListRowManager
import com.github.damontecres.stashapp.util.MutationEngine
import com.github.damontecres.stashapp.util.QueryEngine
import com.github.damontecres.stashapp.util.StashCoroutineExceptionHandler
import com.github.damontecres.stashapp.util.height
import com.github.damontecres.stashapp.util.isNotNullOrBlank
import com.github.damontecres.stashapp.util.name
import com.github.damontecres.stashapp.util.showSetRatingToast
import com.github.damontecres.stashapp.util.width
import com.github.damontecres.stashapp.views.StashOnFocusChangeListener
import com.github.damontecres.stashapp.views.StashRatingBar
import kotlinx.coroutines.CoroutineExceptionHandler
import kotlinx.coroutines.Job
import kotlinx.coroutines.launch
import java.util.concurrent.locks.ReentrantReadWriteLock

class ImageDetailsFragment : DetailsSupportFragment() {
    private val viewModel: ImageViewModel by activityViewModels<ImageViewModel>()

    private lateinit var queryEngine: QueryEngine
    private lateinit var mutationEngine: MutationEngine

    private val detailsPresenter = FullWidthDetailsOverviewRowPresenter(ImageDetailsRowPresenter())
    private val mPresenterSelector =
        ClassPresenterSelector()
            .addClassPresenter(ListRow::class.java, ListRowPresenter())
            .addClassPresenter(DetailsOverviewRow::class.java, detailsPresenter)
    private val mAdapter = SparseArrayObjectAdapter(mPresenterSelector)

    private lateinit var ratingBar: StashRatingBar
    private lateinit var resultLauncher: ActivityResultLauncher<Intent>

    private val mPerformersAdapter =
        ArrayObjectAdapter(PerformerPresenter(PerformerLongClickCallBack()))
    private val performersRowManager =
        ListRowManager<PerformerData>(
            DataType.PERFORMER,
            ListRowManager.SparseArrayRowModifier(mAdapter, PERFORMER_POS),
            mPerformersAdapter,
        ) { performerIds ->
            val imageId = viewModel.image.value!!.id
            val result = mutationEngine.updateImage(imageId, performerIds = performerIds)
            result?.performers?.map { it.performerData }.orEmpty()
        }

    private val tagsRowManager =
        ListRowManager<TagData>(
            DataType.TAG,
            ListRowManager.SparseArrayRowModifier(mAdapter, TAG_POS),
            ArrayObjectAdapter(TagPresenter(TagLongClickCallBack())),
        ) { tagIds ->
            val imageId = viewModel.image.value!!.id
            val result = mutationEngine.updateImage(imageId, tagIds = tagIds)
            result?.tags?.map { it.tagData }.orEmpty()
        }

    private val galleriesRowManager =
        ListRowManager<GalleryData>(
            DataType.GALLERY,
            ListRowManager.SparseArrayRowModifier(mAdapter, GALLERY_POS),
            ArrayObjectAdapter(GalleryPresenter(GalleryLongClickCallBack())),
        ) { galleryIds ->
            val imageId = viewModel.image.value!!.id
            val result = mutationEngine.updateImage(imageId, galleryIds = galleryIds)
            result?.galleries?.map { it.galleryData }.orEmpty()
        }

    private val mStudioAdapter = ArrayObjectAdapter(StudioPresenter(StudioLongClickCallBack()))
    private val studioAdapter =
        ListRowManager<StudioData>(
            DataType.STUDIO,
            ListRowManager.SparseArrayRowModifier(mAdapter, STUDIO_POS),
            mStudioAdapter,
        ) { studioIds ->
            val newStudioId =
                if (studioIds.isEmpty()) {
                    null
                } else if (studioIds.size == 1) {
                    studioIds.first()
                } else {
                    studioIds.last()
                }

            val result =
                mutationEngine.updateImage(viewModel.image.value!!.id, studioId = newStudioId)
            val newStudio = result?.studio?.studioData
            if (newStudio != null) {
                listOf(newStudio)
            } else {
                listOf()
            }
        }

    override fun onViewCreated(
        view: View,
        savedInstanceState: Bundle?,
    ) {
        super.onViewCreated(view, savedInstanceState)

        val lock = ReentrantReadWriteLock()
        queryEngine = QueryEngine(requireContext(), false, lock)
        mutationEngine = MutationEngine(requireContext(), false, lock)

        adapter = mAdapter

        detailsPresenter.actionsBackgroundColor =
            ContextCompat.getColor(requireActivity(), android.R.color.transparent)
        detailsPresenter.backgroundColor =
            ContextCompat.getColor(requireActivity(), R.color.transparent_black_50)

        val detailsBackground = DetailsSupportFragmentBackgroundController(this)
        detailsBackground.solidColor =
            ContextCompat.getColor(requireActivity(), R.color.transparent_black_75)
        detailsBackground.enableParallax()

        val detailsActionsAdapter = ArrayObjectAdapter(DetailsActionsPresenter())
        detailsActionsAdapter.add(Action(R.string.fa_rotate_left.toLong()))
        detailsActionsAdapter.add(Action(R.string.fa_rotate_right.toLong()))
        detailsActionsAdapter.add(Action(R.string.fa_magnifying_glass_plus.toLong()))
        detailsActionsAdapter.add(Action(R.string.fa_magnifying_glass_minus.toLong()))
        detailsActionsAdapter.add(Action(R.string.fa_arrow_right_arrow_left.toLong()))

        detailsPresenter.onActionClickedListener =
            OnActionClickedListener { action ->
                val controller = viewModel.imageController
                if (controller != null) {
                    when (action.id.toInt()) {
                        R.string.fa_rotate_left -> controller.rotateLeft()
                        R.string.fa_rotate_right -> controller.rotateRight()
                        R.string.fa_magnifying_glass_plus -> controller.zoomIn()
                        R.string.fa_magnifying_glass_minus -> controller.zoomOut()
                        R.string.fa_arrow_right_arrow_left -> controller.flip()
                    }
                }
            }

        val queryJob: Job? = null

        viewModel.image.observe(viewLifecycleOwner) { newImage ->
            queryJob?.cancel()

            val detailsRow = DetailsOverviewRow(newImage)
            detailsRow.actionsAdapter = detailsActionsAdapter

            mAdapter.set(1, detailsRow)

            if (newImage.date.isNotNullOrBlank()) {
                mPerformersAdapter.presenterSelector = SinglePresenterSelector(PerformerInScenePresenter(newImage.date))
            } else {
                mPerformersAdapter.presenterSelector = SinglePresenterSelector(PerformerPresenter())
            }

            if (newImage.studio != null) {
                studioAdapter.setItems(listOf(newImage.studio.studioData))
            } else {
                studioAdapter.clear()
            }
            viewLifecycleOwner.lifecycleScope.launch(StashCoroutineExceptionHandler()) {
                val extraImageData = queryEngine.getImageExtra(newImage.id)
                tagsRowManager.setItems(extraImageData?.tags?.map { it.tagData }.orEmpty())
                performersRowManager.setItems(extraImageData?.performers?.map { it.performerData }.orEmpty())
                galleriesRowManager.setItems(extraImageData?.galleries?.map { it.galleryData }.orEmpty())
            }
        }
    }

    private inner class ImageDetailsRowPresenter : AbstractDetailsDescriptionPresenter() {
        override fun onBindDescription(
            vh: ViewHolder,
            item: Any,
        ) {
            val context = vh.view.context
            val image = item as ImageData
            vh.title.text = image.title
            vh.subtitle.text = image.date

            // Need to override the background
            // TODO: maybe use a theme/style instead?
            val ratingBar = vh.view.findViewById<StashRatingBar>(R.id.rating_bar)
            ratingBar.rating100 = image.rating100 ?: 0
            ratingBar.setRatingCallback { rating100 ->
                viewLifecycleOwner.lifecycleScope.launch(StashCoroutineExceptionHandler(true)) {
                    val result = mutationEngine.updateImage(image.id, rating100 = rating100)
                    ratingBar.rating100 = result?.rating100 ?: 0
                    showSetRatingToast(requireContext(), rating100)
                }
            }

            val body =
                buildList {
                    if (image.visual_files.isNotEmpty()) {
                        val imageFile = image.visual_files.first()
                        val imageHeight = imageFile.height
                        val imageWidth = imageFile.width
                        if (imageHeight != null && imageWidth != null) {
                            add("${context.getString(R.string.stashapp_dimensions)}: ${imageWidth}x$imageHeight")
                        }
                    }
                    if (image.photographer.isNotNullOrBlank()) {
                        add("${context.getString(R.string.stashapp_photographer)}: ${image.photographer}")
                    }
                    add("")
                    if (image.details != null) {
                        add(image.details)
                    }
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
    }

    private class ActionViewHolder(button: Button) : ViewHolder(button) {
        var mAction: Long? = null
        var mButton: Button = button
    }

    private inner class DetailsActionsPresenter : Presenter() {
        override fun onCreateViewHolder(parent: ViewGroup): ViewHolder {
            val view =
                LayoutInflater.from(parent.context)
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
            vh.mButton.text = vh.view.context.getString(action.id.toInt())
        }

        override fun onUnbindViewHolder(viewHolder: ViewHolder) {
            val vh = viewHolder as ActionViewHolder
            vh.mAction = null
            vh.mButton.text = null
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
                            "Removed tag '${item.name}' from image",
                            Toast.LENGTH_SHORT,
                        ).show()
                    }
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
                Log.d(TAG, "Removing performer $performerId")
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
                            "Removed performer '${item.name}' from image",
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
                            "Removed studio from image",
                            Toast.LENGTH_SHORT,
                        ).show()
                    }
                }
            }
        }
    }

    private inner class GalleryLongClickCallBack : DetailsLongClickCallBack<GalleryData> {
        override fun onItemLongClick(
            context: Context,
            item: GalleryData,
            popUpItem: StashPresenter.PopUpItem,
        ) {
            if (popUpItem == REMOVE_POPUP_ITEM) {
                val performerId = item.id
                Log.d(TAG, "Removing performer $performerId")
                viewLifecycleOwner.lifecycleScope.launch(
                    CoroutineExceptionHandler { _, ex ->
                        Log.e(TAG, "Exception setting galleries", ex)
                        Toast.makeText(
                            requireContext(),
                            "Failed to remove gallery: ${ex.message}",
                            Toast.LENGTH_LONG,
                        ).show()
                    },
                ) {
                    if (galleriesRowManager.remove(item)) {
                        Toast.makeText(
                            requireContext(),
                            "Removed gallery '${item.name}' from image",
                            Toast.LENGTH_SHORT,
                        ).show()
                    }
                }
            }
        }
    }
}
