package com.github.damontecres.stashapp

import android.content.Intent
import android.os.Bundle
import android.view.View
import android.widget.ImageView
import android.widget.TableLayout
import android.widget.TableRow
import android.widget.TextView
import androidx.fragment.app.Fragment
import androidx.lifecycle.lifecycleScope
import com.github.damontecres.stashapp.data.Movie
import com.github.damontecres.stashapp.presenters.MoviePresenter
import com.github.damontecres.stashapp.presenters.StashPresenter
import com.github.damontecres.stashapp.util.Constants
import com.github.damontecres.stashapp.util.QueryEngine
import com.github.damontecres.stashapp.util.StashCoroutineExceptionHandler
import com.github.damontecres.stashapp.util.StashGlide
import kotlinx.coroutines.launch
import kotlin.time.DurationUnit
import kotlin.time.toDuration

class MovieFragment : Fragment(R.layout.movie_view) {
    private lateinit var frontImage: ImageView
    private lateinit var backImage: ImageView
    private lateinit var titleText: TextView
    private lateinit var table: TableLayout

    override fun onViewCreated(
        view: View,
        savedInstanceState: Bundle?,
    ) {
        super.onViewCreated(view, savedInstanceState)

        frontImage = view.findViewById(R.id.movie_front_image)
        backImage = view.findViewById(R.id.movie_back_image)
        titleText = view.findViewById(R.id.movie_name)

        table = view.findViewById(R.id.movie_table)

        val movie = requireActivity().intent.getParcelableExtra<Movie>("movie")
        if (movie != null) {
            titleText.text = movie.name
            if (movie.frontImagePath != null) {
                configureLayout(frontImage)
                StashGlide.with(requireActivity(), movie.frontImagePath)
                    .centerCrop()
                    .error(StashPresenter.glideError(requireContext()))
                    .into(frontImage)
            }
            if (movie.backImagePath != null) {
                configureLayout(backImage)
                StashGlide.with(requireActivity(), movie.backImagePath)
                    .centerCrop()
                    .error(StashPresenter.glideError(requireContext()))
                    .into(backImage)
            }
            viewLifecycleOwner.lifecycleScope.launch(StashCoroutineExceptionHandler()) {
                val queryEngine = QueryEngine(requireContext())
                val movie = queryEngine.getMovie(movie.id)
                addRow(
                    R.string.stashapp_duration,
                    movie?.duration?.toDuration(DurationUnit.MINUTES)?.toString(),
                )
                addRow(R.string.stashapp_date, movie?.date)
                addRow(R.string.stashapp_studio, movie?.studio?.name)
                addRow(R.string.stashapp_director, movie?.director)
                addRow(R.string.stashapp_synopsis, movie?.synopsis)
                addRow(R.string.stashapp_created_at, Constants.parseTimeToString(movie?.created_at))
                addRow(R.string.stashapp_updated_at, Constants.parseTimeToString(movie?.updated_at))
                table.setColumnShrinkable(1, true)
            }
        } else {
            val intent = Intent(requireActivity(), MainActivity::class.java)
            startActivity(intent)
        }
    }

    private fun configureLayout(view: ImageView) {
        val lp = view.layoutParams
        val scale = 2.0
        lp.width = (MoviePresenter.CARD_WIDTH * scale).toInt()
        lp.height = (MoviePresenter.CARD_HEIGHT * scale).toInt()
        view.layoutParams = lp
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
}
