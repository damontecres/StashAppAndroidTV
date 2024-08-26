package com.github.damontecres.stashapp.image

import android.os.Bundle
import android.text.Spannable
import android.text.SpannableString
import android.util.Log
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Button
import androidx.annotation.StringRes
import androidx.core.content.ContextCompat
import androidx.fragment.app.activityViewModels
import androidx.leanback.app.DetailsSupportFragment
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
import com.github.damontecres.stashapp.StashApplication
import com.github.damontecres.stashapp.api.fragment.GalleryData
import com.github.damontecres.stashapp.api.fragment.ImageData
import com.github.damontecres.stashapp.api.fragment.PerformerData
import com.github.damontecres.stashapp.api.fragment.StudioData
import com.github.damontecres.stashapp.api.fragment.TagData
import com.github.damontecres.stashapp.data.DataType
import com.github.damontecres.stashapp.presenters.GalleryPresenter
import com.github.damontecres.stashapp.presenters.PerformerInScenePresenter
import com.github.damontecres.stashapp.presenters.PerformerPresenter
import com.github.damontecres.stashapp.presenters.StudioPresenter
import com.github.damontecres.stashapp.presenters.TagPresenter
import com.github.damontecres.stashapp.util.ListRowManager
import com.github.damontecres.stashapp.util.MutationEngine
import com.github.damontecres.stashapp.util.QueryEngine
import com.github.damontecres.stashapp.util.StashCoroutineExceptionHandler
import com.github.damontecres.stashapp.util.height
import com.github.damontecres.stashapp.util.isNotNullOrBlank
import com.github.damontecres.stashapp.util.width
import com.github.damontecres.stashapp.views.FontSpan
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

    private val mPerformersAdapter = ArrayObjectAdapter(PerformerPresenter())
    private val performersRowManager =
        ListRowManager<PerformerData>(
            DataType.PERFORMER,
            ListRowManager.SparseArrayRowModifier(mAdapter, PERFORMER_POS),
            mPerformersAdapter,
        ) { performerIds ->
            TODO()
        }

    private val tagsRowManager =
        ListRowManager<TagData>(
            DataType.TAG,
            ListRowManager.SparseArrayRowModifier(mAdapter, TAG_POS),
            ArrayObjectAdapter(TagPresenter()),
        ) { tagIds ->
            TODO()
        }

    private val galleriesRowManager =
        ListRowManager<GalleryData>(
            DataType.GALLERY,
            ListRowManager.SparseArrayRowModifier(mAdapter, GALLERY_POS),
            ArrayObjectAdapter(GalleryPresenter()),
        ) { galleryIds ->
            TODO()
        }

    private val mStudioAdapter = ArrayObjectAdapter(StudioPresenter())
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
            TODO()
//            val result = mutationEngine.setStudioOnScene(mSelectedMovie!!.id, newStudioId)
//            val newStudio = result?.studio?.studioData
//            if (newStudio != null) {
//                listOf(newStudio)
//            } else {
//                listOf()
//            }
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

        val fragment = requireActivity().supportFragmentManager.findFragmentByTag(ImageTabbedFragment::class.java.simpleName)
        val titleBar = fragment?.view?.findViewById<View>(R.id.browse_title_group)
        Log.v(TAG, "Got titleBar: ${titleBar != null}")
        titleView = titleBar

        detailsPresenter.backgroundColor =
            ContextCompat.getColor(requireActivity(), R.color.default_card_background)

        val detailsActionsAdapter = ArrayObjectAdapter(DetailsActionsPresenter())
        detailsActionsAdapter.add(Action(R.string.fa_rotate_left.toLong()))
        detailsActionsAdapter.add(Action(R.string.fa_rotate_right.toLong()))

        detailsPresenter.onActionClickedListener =
            OnActionClickedListener { action ->
                val controller = viewModel.imageController
                if (controller != null) {
                    when (action.id) {
                        R.string.fa_rotate_left.toLong() -> controller.rotateLeft()
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

    private fun getIcon(
        @StringRes string: Int,
    ): CharSequence {
        return SpannableString(getString(string)).apply {
            val start = 0
            val end = 1
            setSpan(
                FontSpan(StashApplication.getFont(R.font.fa_solid_900)),
                start,
                end,
                Spannable.SPAN_INCLUSIVE_INCLUSIVE,
            )
        }
    }

    private class ImageDetailsRowPresenter : AbstractDetailsDescriptionPresenter() {
        override fun onBindDescription(
            vh: ViewHolder,
            item: Any,
        ) {
            val context = vh.view.context
            val image = item as ImageData
            vh.title.text = image.title
            vh.subtitle.text = image.date

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
}
