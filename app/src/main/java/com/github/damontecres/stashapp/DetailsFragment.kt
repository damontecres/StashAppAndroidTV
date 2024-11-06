package com.github.damontecres.stashapp

import android.os.Bundle
import android.util.TypedValue
import android.view.View
import android.widget.Button
import android.widget.ImageView
import android.widget.ScrollView
import android.widget.TableLayout
import android.widget.TableRow
import android.widget.TextView
import androidx.fragment.app.Fragment
import com.github.damontecres.stashapp.util.MutationEngine
import com.github.damontecres.stashapp.util.QueryEngine
import com.github.damontecres.stashapp.util.StashServer
import com.github.damontecres.stashapp.util.onlyScrollIfNeeded
import com.github.damontecres.stashapp.util.readOnlyModeEnabled
import com.github.damontecres.stashapp.views.StashOnFocusChangeListener
import com.github.damontecres.stashapp.views.StashRatingBar

/**
 * Simple details page with an image, favorite button, rating bar, and table for details
 */
abstract class DetailsFragment : Fragment(R.layout.details_view) {
    protected lateinit var imageView: ImageView
    protected lateinit var table: TableLayout
    protected lateinit var favoriteButton: Button
    protected lateinit var ratingBar: StashRatingBar

    protected lateinit var queryEngine: QueryEngine
    protected lateinit var mutationEngine: MutationEngine

    override fun onViewCreated(
        view: View,
        savedInstanceState: Bundle?,
    ) {
        super.onViewCreated(view, savedInstanceState)

        imageView = view.findViewById(R.id.details_image)
        table = view.findViewById(R.id.details_table)
        favoriteButton = view.findViewById(R.id.favorite_button)
        favoriteButton.onFocusChangeListener = StashOnFocusChangeListener(requireContext())
        favoriteButton.isFocusable = true
        ratingBar = view.findViewById(R.id.rating_bar)
        if (readOnlyModeEnabled()) {
            favoriteButton.isFocusable = false
            ratingBar.disable()
        }

        val server = StashServer.requireCurrentServer()
        queryEngine = QueryEngine(server)
        mutationEngine = MutationEngine(server)
    }

    override fun onResume() {
        super.onResume()
        val scrollView = requireView().findViewById<ScrollView>(R.id.details_scrollview)
        scrollView.onlyScrollIfNeeded()
    }

    protected fun addRow(
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
        keyView.setTextSize(
            TypedValue.COMPLEX_UNIT_PX,
            resources.getDimension(R.dimen.table_text_size_large),
        )

        val valueView = row.findViewById<TextView>(R.id.table_row_value)
        valueView.text = value
        valueView.setTextSize(
            TypedValue.COMPLEX_UNIT_PX,
            resources.getDimension(R.dimen.table_text_size_large),
        )

        table.addView(row)
    }

    protected fun updateFavorite(favorite: Boolean) {
        if (favorite) {
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
    }

    companion object {
        private const val TAG = "DetailsFragment"
    }
}
