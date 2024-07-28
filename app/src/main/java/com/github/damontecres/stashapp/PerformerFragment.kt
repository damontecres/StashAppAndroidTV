package com.github.damontecres.stashapp

import android.os.Build
import android.os.Bundle
import android.util.Log
import android.view.View
import android.widget.Button
import android.widget.ImageView
import android.widget.ScrollView
import android.widget.TableLayout
import android.widget.TableRow
import android.widget.TextView
import android.widget.Toast
import androidx.fragment.app.Fragment
import androidx.lifecycle.lifecycleScope
import com.github.damontecres.stashapp.api.fragment.PerformerData
import com.github.damontecres.stashapp.api.type.CircumisedEnum
import com.github.damontecres.stashapp.presenters.PerformerPresenter
import com.github.damontecres.stashapp.presenters.StashPresenter
import com.github.damontecres.stashapp.util.MutationEngine
import com.github.damontecres.stashapp.util.QueryEngine
import com.github.damontecres.stashapp.util.StashCoroutineExceptionHandler
import com.github.damontecres.stashapp.util.StashGlide
import com.github.damontecres.stashapp.util.ageInYears
import com.github.damontecres.stashapp.util.onlyScrollIfNeeded
import com.github.damontecres.stashapp.views.StashOnFocusChangeListener
import com.github.damontecres.stashapp.views.parseTimeToString
import kotlinx.coroutines.CoroutineExceptionHandler
import kotlinx.coroutines.launch
import java.util.concurrent.locks.ReentrantReadWriteLock
import kotlin.math.floor
import kotlin.math.roundToInt

class PerformerFragment : Fragment(R.layout.performer_view) {
    private lateinit var mPerformerImage: ImageView
    private lateinit var mPerformerName: TextView
    private lateinit var mPerformerDisambiguation: TextView
    private lateinit var table: TableLayout
    private lateinit var favoriteButton: Button

    private lateinit var queryEngine: QueryEngine
    private lateinit var mutationEngine: MutationEngine

    override fun onViewCreated(
        view: View,
        savedInstanceState: Bundle?,
    ) {
        super.onViewCreated(view, savedInstanceState)

        mPerformerImage = view.findViewById(R.id.performer_image)
        val lp = mPerformerImage.layoutParams
        val scale = 1.33
        lp.width = (PerformerPresenter.CARD_WIDTH * scale).toInt()
        lp.height = (PerformerPresenter.CARD_HEIGHT * scale).toInt()
        mPerformerImage.layoutParams = lp

        mPerformerName = view.findViewById(R.id.performer_name)
        mPerformerDisambiguation = view.findViewById(R.id.performer_disambiguation)

        table = view.findViewById(R.id.performer_table)
        favoriteButton = view.findViewById(R.id.favorite_button)
        favoriteButton.onFocusChangeListener = StashOnFocusChangeListener(requireContext())

        val performerId = requireActivity().intent.getStringExtra("id")!!

        val lock = ReentrantReadWriteLock()
        queryEngine = QueryEngine(requireContext(), true, lock)
        mutationEngine = MutationEngine(requireContext(), true, lock)

        val exceptionHandler =
            CoroutineExceptionHandler { _, ex ->
                Log.e(TAG, "Error fetching data", ex)
                Toast.makeText(
                    requireContext(),
                    "Error fetching data: ${ex.message}",
                    Toast.LENGTH_LONG,
                ).show()
            }
        viewLifecycleOwner.lifecycleScope.launch(exceptionHandler) {
            val perf = queryEngine.getPerformer(performerId)
            if (perf == null) {
                Toast.makeText(
                    requireContext(),
                    "Performer not found: $performerId",
                    Toast.LENGTH_LONG,
                ).show()
                return@launch
            } else {
                updateUi(perf)
            }
        }
    }

    private fun updateUi(perf: PerformerData) {
        mPerformerName.text = perf.name
        mPerformerDisambiguation.text = perf.disambiguation

        favoriteButton.isFocusable = true
        if (perf.favorite) {
            favoriteButton.setTextColor(
                resources.getColor(
                    android.R.color.holo_red_light,
                    requireActivity().theme,
                ),
            )
        } else {
            favoriteButton.setTextColor(
                resources.getColor(
                    R.color.transparent_grey_25,
                    requireActivity().theme,
                ),
            )
        }

        favoriteButton.setOnClickListener {
            viewLifecycleOwner.lifecycleScope.launch(StashCoroutineExceptionHandler()) {
                val newPerformer = mutationEngine.setPerformerFavorite(perf.id, !perf.favorite)
                if (newPerformer != null) {
                    if (newPerformer.favorite) {
                        Toast.makeText(
                            requireContext(),
                            getString(R.string.stashapp_performer_favorite),
                            Toast.LENGTH_LONG,
                        ).show()
                    }
                    updateUi(newPerformer)
                }
            }
        }
        if (perf.image_path != null) {
            StashGlide.with(requireContext(), perf.image_path)
                .optionalCenterCrop()
                .error(StashPresenter.glideError(requireContext()))
                .into(mPerformerImage)
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
        if (perf.circumcised != null) {
            val string =
                when (perf.circumcised) {
                    CircumisedEnum.CUT -> getString(R.string.stashapp_circumcised_types_CUT)
                    CircumisedEnum.UNCUT -> getString(R.string.stashapp_circumcised_types_UNCUT)
                    CircumisedEnum.UNKNOWN__ -> null
                }
            addRow(R.string.stashapp_circumcised, string)
        }
        addRow(R.string.stashapp_tattoos, perf.tattoos)
        addRow(R.string.stashapp_piercings, perf.piercings)
        addRow(R.string.stashapp_career_length, perf.career_length)
        addRow(R.string.stashapp_created_at, parseTimeToString(perf.created_at))
        addRow(R.string.stashapp_updated_at, parseTimeToString(perf.updated_at))
        table.setColumnShrinkable(1, true)
    }

    override fun onResume() {
        super.onResume()
        val scrollView = requireView().findViewById<ScrollView>(R.id.performer_scrollview)
        scrollView.onlyScrollIfNeeded()
    }

    private fun addRow(
        key: Int,
        value: String?,
    ) {
        if (value.isNullOrBlank()) {
            return
        }
        val keyString = getString(key) + ":"

        val row =
            requireActivity().layoutInflater.inflate(R.layout.table_row, table, false) as TableRow

        val keyView = row.findViewById<TextView>(R.id.table_row_key)
        keyView.text = keyString

        val valueView = row.findViewById<TextView>(R.id.table_row_value)
        valueView.text = value

        table.addView(row)
    }

    companion object {
        private const val TAG = "PerformerFragment"
    }
}
