package com.github.damontecres.stashapp

import android.content.Intent
import android.os.Bundle
import android.view.View
import android.widget.ImageView
import android.widget.TextView
import androidx.fragment.app.Fragment
import androidx.lifecycle.lifecycleScope
import com.bumptech.glide.Glide
import com.github.damontecres.stashapp.data.Movie
import com.github.damontecres.stashapp.presenters.StashPresenter
import com.github.damontecres.stashapp.util.StashCoroutineExceptionHandler
import com.github.damontecres.stashapp.util.createGlideUrl
import kotlinx.coroutines.launch

class MovieFragment : Fragment(R.layout.performer_view) {
    private lateinit var mPerformerImage: ImageView
    private lateinit var mPerformerName: TextView
    private lateinit var mPerformerDisambiguation: TextView

    override fun onViewCreated(
        view: View,
        savedInstanceState: Bundle?,
    ) {
        super.onViewCreated(view, savedInstanceState)

        mPerformerImage = view.findViewById(R.id.performer_image)
        mPerformerName = view.findViewById(R.id.performer_name)
        mPerformerDisambiguation = view.findViewById(R.id.performer_disambiguation)

        val movie = requireActivity().intent.getParcelableExtra<Movie>("movie")
        if (movie != null) {
            mPerformerName.text = movie.name
            mPerformerDisambiguation.text = movie.aliases

            if (movie.frontImagePath != null) {
                viewLifecycleOwner.lifecycleScope.launch(StashCoroutineExceptionHandler()) {
                    val url = createGlideUrl(movie.frontImagePath, requireContext())
                    Glide.with(requireActivity())
                        .load(url)
                        .centerCrop()
                        .error(StashPresenter.glideError(requireContext()))
                        .into(mPerformerImage)
                }
            }
        } else {
            val intent = Intent(requireActivity(), MainActivity::class.java)
            startActivity(intent)
        }
    }
}
