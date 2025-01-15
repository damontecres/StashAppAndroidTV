package com.github.damontecres.stashapp

import android.os.Bundle
import android.view.View
import androidx.fragment.app.viewModels
import androidx.lifecycle.lifecycleScope
import androidx.preference.PreferenceManager
import com.apollographql.apollo.api.Optional
import com.github.damontecres.stashapp.api.type.CriterionModifier
import com.github.damontecres.stashapp.api.type.GalleryFilterType
import com.github.damontecres.stashapp.api.type.StringCriterionInput
import com.github.damontecres.stashapp.data.DataType
import com.github.damontecres.stashapp.navigation.Destination
import com.github.damontecres.stashapp.presenters.StashPresenter
import com.github.damontecres.stashapp.suppliers.FilterArgs
import com.github.damontecres.stashapp.util.StashCoroutineExceptionHandler
import com.github.damontecres.stashapp.util.StashGlide
import com.github.damontecres.stashapp.util.isNotNullOrBlank
import com.github.damontecres.stashapp.util.showSetRatingToast
import com.github.damontecres.stashapp.views.models.GalleryViewModel
import kotlinx.coroutines.launch

class GalleryDetailsFragment : DetailsFragment() {
    private val viewModel: GalleryViewModel by viewModels(ownerProducer = { requireParentFragment() })

    override fun onViewCreated(
        view: View,
        savedInstanceState: Bundle?,
    ) {
        super.onViewCreated(view, savedInstanceState)

        favoriteButton.visibility = View.GONE

        viewModel.item.observe(viewLifecycleOwner) { galleryData ->
            if (galleryData == null) {
                return@observe
            }
            if (PreferenceManager
                    .getDefaultSharedPreferences(requireContext())
                    .getBoolean(getString(R.string.pref_key_show_playback_debug_info), false)
            ) {
                addRow(R.string.id, galleryData.id)
            }
            addRow(R.string.stashapp_date, galleryData.date)
            addRow(R.string.stashapp_scene_code, galleryData.code)
            addRow(R.string.stashapp_photographer, galleryData.photographer) {
                if (galleryData.photographer.isNotNullOrBlank()) {
                    setTextColor(resources.getColorStateList(R.color.clickable_text, null))
                    setOnClickListener {
                        val objectFilter =
                            GalleryFilterType(
                                photographer =
                                    Optional.present(
                                        StringCriterionInput(
                                            value = galleryData.photographer,
                                            modifier = CriterionModifier.EQUALS,
                                        ),
                                    ),
                            )
                        val filterArgs =
                            FilterArgs(
                                dataType = DataType.GALLERY,
                                name = galleryData.photographer,
                                objectFilter = objectFilter,
                            )
                        serverViewModel.navigationManager.navigate(Destination.Filter(filterArgs))
                    }
                }
            }
            addRow(R.string.stashapp_details, galleryData.details)

            if (galleryData.paths.cover.isNotNullOrBlank()) {
                StashGlide
                    .with(requireContext(), galleryData.paths.cover)
                    .optionalFitCenter()
                    .error(StashPresenter.glideError(requireContext()))
                    .into(imageView)
            } else {
                imageView.visibility = View.INVISIBLE
            }

            ratingBar.rating100 = galleryData.rating100 ?: 0
            ratingBar.setRatingCallback { newRating100 ->
                viewLifecycleOwner.lifecycleScope.launch(StashCoroutineExceptionHandler()) {
                    val newItem =
                        mutationEngine.updateGallery(galleryData.id, rating100 = newRating100)
                    if (newItem != null) {
                        showSetRatingToast(requireContext(), newRating100)
                        viewModel.update(newItem)
                    }
                }
            }
        }
    }
}
