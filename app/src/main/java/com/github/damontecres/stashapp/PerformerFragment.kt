package com.github.damontecres.stashapp

import android.content.Intent
import android.graphics.Color
import android.os.Build
import android.os.Bundle
import android.util.Log
import android.view.Gravity
import android.view.View
import android.widget.ImageView
import android.widget.TableLayout
import android.widget.TableRow
import android.widget.TextView
import android.widget.Toast
import androidx.fragment.app.Fragment
import androidx.lifecycle.lifecycleScope
import com.bumptech.glide.Glide
import com.github.damontecres.stashapp.api.type.CircumisedEnum
import com.github.damontecres.stashapp.data.Performer
import com.github.damontecres.stashapp.presenters.PerformerPresenter
import com.github.damontecres.stashapp.util.QueryEngine
import com.github.damontecres.stashapp.util.ageInYears
import com.github.damontecres.stashapp.util.createGlideUrl
import kotlinx.coroutines.CoroutineExceptionHandler
import kotlinx.coroutines.launch
import kotlin.math.floor
import kotlin.math.roundToInt

class PerformerFragment : Fragment(R.layout.performer_view) {
    private lateinit var mPerformerImage: ImageView
    private lateinit var mPerformerName: TextView
    private lateinit var mPerformerDisambiguation: TextView
    private lateinit var table: TableLayout

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

        val performer = requireActivity().intent.getParcelableExtra<Performer>("performer")
        if (performer != null) {
            mPerformerName.text = performer.name
            mPerformerDisambiguation.text = performer.disambiguation

            val queryEngine = QueryEngine(requireContext(), true)

            val exceptionHandler =
                CoroutineExceptionHandler { _, ex ->
                    Log.e(TAG, "\"Error fetching data", ex)
                    Toast.makeText(
                        requireContext(),
                        "Error fetching data: ${ex.message}",
                        Toast.LENGTH_LONG,
                    ).show()
                }
            viewLifecycleOwner.lifecycleScope.launch(exceptionHandler) {
                val perf =
                    queryEngine.findPerformers(performerIds = listOf(performer.id.toInt())).first()

                if (perf.image_path != null) {
                    val url = createGlideUrl(perf.image_path, requireContext())
                    Glide.with(requireContext())
                        .load(url)
                        .centerCrop()
                        .error(R.drawable.default_background)
                        .into(mPerformerImage)
                }

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
            }
        } else {
            val intent = Intent(requireActivity(), MainActivity::class.java)
            startActivity(intent)
        }
    }

    private fun addRow(
        key: Int,
        value: String?,
    ) {
        if (value.isNullOrBlank()) {
            return
        }
        val keyString = getString(key) + ":"

        val row = TableRow(requireContext())
        row.layoutParams =
            TableLayout.LayoutParams(
                TableLayout.LayoutParams.MATCH_PARENT,
                TableLayout.LayoutParams.MATCH_PARENT,
            )
        row.gravity = Gravity.CENTER_HORIZONTAL

        val keyView = TextView(requireContext())
        keyView.text = keyString
        keyView.textSize = TABLE_TEXT_SIZE
        keyView.setTextColor(Color.WHITE)
        keyView.textAlignment = TextView.TEXT_ALIGNMENT_VIEW_START
        keyView.setPadding(5, 3, 5, 3)

        val valueView = TextView(requireContext())
        valueView.text = value
        valueView.textSize = TABLE_TEXT_SIZE
        valueView.setTextColor(Color.WHITE)
        valueView.textAlignment = TextView.TEXT_ALIGNMENT_VIEW_START
        valueView.setPadding(15, 3, 5, 3)

        row.addView(keyView)
        row.addView(valueView)

        table.addView(row)
    }

    companion object {
        private const val TAG = "PerformerFragment"
        private const val TABLE_TEXT_SIZE = 13F
    }
}
