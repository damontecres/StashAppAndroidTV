package com.github.damontecres.stashapp

import android.os.Build
import android.os.Bundle
import android.view.View
import android.widget.Toast
import androidx.fragment.app.viewModels
import androidx.lifecycle.lifecycleScope
import com.github.damontecres.stashapp.api.fragment.PerformerData
import com.github.damontecres.stashapp.api.type.CircumisedEnum
import com.github.damontecres.stashapp.presenters.StashPresenter
import com.github.damontecres.stashapp.util.StashCoroutineExceptionHandler
import com.github.damontecres.stashapp.util.StashGlide
import com.github.damontecres.stashapp.util.ageInYears
import com.github.damontecres.stashapp.util.showSetRatingToast
import com.github.damontecres.stashapp.views.models.PerformerViewModel
import com.github.damontecres.stashapp.views.parseTimeToString
import kotlinx.coroutines.launch
import kotlin.math.floor
import kotlin.math.roundToInt

/**
 * Details for a performer
 */
class PerformerDetailsFragment : DetailsFragment() {
    private val viewModel: PerformerViewModel by viewModels(ownerProducer = { requireParentFragment() })

    override fun onViewCreated(
        view: View,
        savedInstanceState: Bundle?,
    ) {
        super.onViewCreated(view, savedInstanceState)
        viewModel.item.observe(viewLifecycleOwner) { performer ->
            if (performer != null) {
                updateUi(performer)
            }
        }
    }

    private fun updateUi(perf: PerformerData) {
        updateFavorite(perf.favorite)

        favoriteButton.setOnClickListener {
            viewLifecycleOwner.lifecycleScope.launch(StashCoroutineExceptionHandler()) {
                val newPerformer = mutationEngine.updatePerformer(perf.id, !perf.favorite)
                if (newPerformer != null) {
                    if (newPerformer.favorite) {
                        Toast
                            .makeText(
                                requireContext(),
                                getString(R.string.stashapp_performer_favorite),
                                Toast.LENGTH_SHORT,
                            ).show()
                    }
                    updateUi(newPerformer)
                }
            }
        }

        ratingBar.rating100 = perf.rating100 ?: 0
        ratingBar.setRatingCallback { newRating100 ->
            viewLifecycleOwner.lifecycleScope.launch(StashCoroutineExceptionHandler()) {
                val newPerformer = mutationEngine.updatePerformer(perf.id, rating100 = newRating100)
                if (newPerformer != null) {
                    showSetRatingToast(requireContext(), newRating100)
                    updateUi(newPerformer)
                }
            }
        }

        if (perf.image_path != null) {
            StashGlide
                .with(requireContext(), perf.image_path)
                .optionalFitCenter()
                .error(StashPresenter.glideError(requireContext()))
                .into(imageView)
        }

        table.removeAllViews()

        if (perf.alias_list.isNotEmpty()) {
            addRow(
                R.string.stashapp_aliases,
                perf.alias_list.joinToString(", "),
            )
        }
        if (!perf.birthdate.isNullOrBlank()) {
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
                val age = perf.ageInYears.toString()
                addRow(R.string.stashapp_age, "$age (${perf.birthdate})")
            }
        }
        addRow(R.string.stashapp_death_date, perf.death_date)
        addRow(R.string.stashapp_country, perf.country)
        addRow(R.string.stashapp_ethnicity, perf.ethnicity)
        addRow(R.string.stashapp_hair_color, perf.hair_color)
        addRow(R.string.stashapp_eye_color, perf.eye_color)
        if (perf.height_cm != null) {
            val feet = floor(perf.height_cm / 30.48).toInt()
            val inches = (perf.height_cm / 2.54 - feet * 12).roundToInt()
            addRow(R.string.stashapp_height, "${perf.height_cm} cm ($feet'$inches\")")
        }
        if (perf.weight != null) {
            val pounds = (perf.weight * 2.2).roundToInt()
            addRow(R.string.stashapp_weight, "${perf.weight} kg ($pounds lbs)")
        }
        if (perf.penis_length != null) {
            val inches = kotlin.math.round(perf.penis_length / 2.54 * 100) / 100
            addRow(R.string.stashapp_penis_length, "${perf.penis_length} cm ($inches\")")
        }
        val circString =
            when (perf.circumcised) {
                CircumisedEnum.CUT -> getString(R.string.stashapp_circumcised_types_CUT)
                CircumisedEnum.UNCUT -> getString(R.string.stashapp_circumcised_types_UNCUT)
                CircumisedEnum.UNKNOWN__, null -> null
            }
        addRow(R.string.stashapp_circumcised, circString)

        addRow(R.string.stashapp_tattoos, perf.tattoos)
        addRow(R.string.stashapp_piercings, perf.piercings)
        addRow(R.string.stashapp_career_length, perf.career_length)
        addRow(R.string.stashapp_created_at, parseTimeToString(perf.created_at))
        addRow(R.string.stashapp_updated_at, parseTimeToString(perf.updated_at))
        table.setColumnShrinkable(1, true)
    }

    companion object {
        private const val TAG = "PerformerFragment"
    }
}
