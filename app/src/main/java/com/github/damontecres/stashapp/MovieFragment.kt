package com.github.damontecres.stashapp

import android.content.Intent
import android.os.Bundle
import android.view.View
import android.widget.ImageView
import android.widget.TableLayout
import android.widget.TableRow
import android.widget.TextView
import androidx.fragment.app.Fragment
import com.bumptech.glide.Glide
import com.github.damontecres.stashapp.data.Movie
import com.github.damontecres.stashapp.presenters.MoviePresenter
import com.github.damontecres.stashapp.presenters.StashPresenter
import com.github.damontecres.stashapp.util.createGlideUrl

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
                val url = createGlideUrl(movie.frontImagePath, requireContext())
                Glide.with(requireActivity())
                    .load(url)
                    .centerCrop()
                    .error(StashPresenter.glideError(requireContext()))
                    .into(frontImage)
            }
            if (movie.backImagePath != null) {
                configureLayout(backImage)
                val url = createGlideUrl(movie.backImagePath, requireContext())
                Glide.with(requireActivity())
                    .load(url)
                    .centerCrop()
                    .error(StashPresenter.glideError(requireContext()))
                    .into(backImage)
            }
//            viewLifecycleOwner.lifecycleScope.launch(StashCoroutineExceptionHandler()) {
//                val queryEngine = QueryEngine(requireContext())
//
//            }
        } else {
            val intent = Intent(requireActivity(), MainActivity::class.java)
            startActivity(intent)
        }
    }

    private fun configureLayout(view: ImageView) {
        val lp = view.layoutParams
        val scale = 1.75
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
