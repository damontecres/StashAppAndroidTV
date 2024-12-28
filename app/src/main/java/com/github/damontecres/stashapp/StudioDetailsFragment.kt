package com.github.damontecres.stashapp

import android.os.Bundle
import android.view.View
import android.widget.Toast
import androidx.fragment.app.viewModels
import androidx.lifecycle.lifecycleScope
import com.github.damontecres.stashapp.api.fragment.StudioData
import com.github.damontecres.stashapp.presenters.StashPresenter
import com.github.damontecres.stashapp.util.StashCoroutineExceptionHandler
import com.github.damontecres.stashapp.util.StashGlide
import com.github.damontecres.stashapp.util.showSetRatingToast
import com.github.damontecres.stashapp.views.models.StudioViewModel
import com.github.damontecres.stashapp.views.parseTimeToString
import kotlinx.coroutines.launch

/**
 * Details for a studio
 */
class StudioDetailsFragment : DetailsFragment() {
    private val viewModel: StudioViewModel by viewModels(ownerProducer = { requireParentFragment() })

    override fun onViewCreated(
        view: View,
        savedInstanceState: Bundle?,
    ) {
        super.onViewCreated(view, savedInstanceState)

        viewModel.item.observe(viewLifecycleOwner) { studio ->
            if (studio == null) {
                return@observe
            } else {
                updateUi(studio)
            }
        }
    }

    private fun updateUi(studio: StudioData) {
        updateFavorite(studio.favorite)

        ratingBar.rating100 = studio.rating100 ?: 0
        ratingBar.setRatingCallback { newRating100 ->
            viewLifecycleOwner.lifecycleScope.launch(StashCoroutineExceptionHandler()) {
                val newStudio = mutationEngine.updateStudio(studio.id, rating100 = newRating100)
                if (newStudio != null) {
                    showSetRatingToast(requireContext(), newRating100)
                    viewModel.update(newStudio)
                }
            }
        }

        favoriteButton.setOnClickListener {
            viewLifecycleOwner.lifecycleScope.launch(StashCoroutineExceptionHandler()) {
                val newStudio = mutationEngine.updateStudio(studio.id, !studio.favorite)
                if (newStudio != null) {
                    if (newStudio.favorite) {
                        Toast
                            .makeText(
                                requireContext(),
                                "Studio favorited",
                                Toast.LENGTH_SHORT,
                            ).show()
                    }
                    viewModel.update(newStudio)
                }
            }
        }
        if (studio.image_path != null) {
            StashGlide
                .with(requireContext(), studio.image_path)
                .optionalFitCenter()
                .error(StashPresenter.glideError(requireContext()))
                .into(imageView)
        }

        table.removeAllViews()

        addRow(R.string.stashapp_details, studio.details)
        addRow(R.string.stashapp_parent_studio, studio.parent_studio?.name)

        if (studio.aliases.isNotEmpty()) {
            addRow(
                R.string.stashapp_aliases,
                studio.aliases.joinToString(", "),
            )
        }

        addRow(R.string.stashapp_created_at, parseTimeToString(studio.created_at))
        addRow(R.string.stashapp_updated_at, parseTimeToString(studio.updated_at))
        table.setColumnShrinkable(1, true)
    }

    companion object {
        private const val TAG = "StudioDetailsFragment"
    }
}
