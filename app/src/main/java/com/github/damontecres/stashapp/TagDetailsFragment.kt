package com.github.damontecres.stashapp

import android.os.Bundle
import android.view.View
import android.widget.Toast
import androidx.fragment.app.viewModels
import androidx.lifecycle.lifecycleScope
import com.github.damontecres.stashapp.api.fragment.TagData
import com.github.damontecres.stashapp.presenters.StashPresenter
import com.github.damontecres.stashapp.util.StashCoroutineExceptionHandler
import com.github.damontecres.stashapp.util.StashGlide
import com.github.damontecres.stashapp.views.models.TagViewModel
import com.github.damontecres.stashapp.views.parseTimeToString
import kotlinx.coroutines.launch

/**
 * Details for a tag
 */
class TagDetailsFragment : DetailsFragment() {
    private val viewModel: TagViewModel by viewModels(ownerProducer = { requireParentFragment() })

    override fun onViewCreated(
        view: View,
        savedInstanceState: Bundle?,
    ) {
        super.onViewCreated(view, savedInstanceState)
        // Tags do not have a rating
        ratingBar.visibility = View.GONE

        viewModel.item.observe(viewLifecycleOwner) { tag ->
            if (tag == null) {
                return@observe
            } else {
                updateUi(tag)
            }
        }
    }

    private fun updateUi(tag: TagData) {
        updateFavorite(tag.favorite)

        favoriteButton.setOnClickListener {
            viewLifecycleOwner.lifecycleScope.launch(StashCoroutineExceptionHandler()) {
                val newTag = mutationEngine.setTagFavorite(tag.id, !tag.favorite)
                if (newTag != null) {
                    if (newTag.favorite) {
                        Toast
                            .makeText(
                                requireContext(),
                                "Tag favorited",
                                Toast.LENGTH_SHORT,
                            ).show()
                    }
                    updateUi(newTag)
                }
            }
        }
        if (tag.image_path != null) {
            StashGlide
                .with(requireContext(), tag.image_path)
                .optionalFitCenter()
                .error(StashPresenter.glideError(requireContext()))
                .into(imageView)
        }

        table.removeAllViews()

        addRow(R.string.stashapp_description, tag.description)

        if (tag.aliases.isNotEmpty()) {
            addRow(
                R.string.stashapp_aliases,
                tag.aliases.joinToString(", "),
            )
        }

        addRow(R.string.stashapp_created_at, parseTimeToString(tag.created_at))
        addRow(R.string.stashapp_updated_at, parseTimeToString(tag.updated_at))
        table.setColumnShrinkable(1, true)
    }

    companion object {
        private const val TAG = "TagDetailsFragment"
    }
}
