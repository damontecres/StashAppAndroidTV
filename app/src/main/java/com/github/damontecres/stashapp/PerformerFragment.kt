package com.github.damontecres.stashapp

import android.content.Intent
import android.os.Bundle
import android.view.View
import android.widget.ImageView
import android.widget.TextView
import androidx.fragment.app.Fragment
import androidx.leanback.widget.ArrayObjectAdapter
import androidx.leanback.widget.ListRowView
import androidx.lifecycle.lifecycleScope
import com.bumptech.glide.Glide
import com.github.damontecres.stashapp.data.Performer
import com.github.damontecres.stashapp.presenters.ScenePresenter
import kotlinx.coroutines.async

class PerformerFragment : Fragment(R.layout.performer_view) {

    private lateinit var mPerformerImage: ImageView
    private lateinit var mPerformerName: TextView
    private lateinit var mPerformerDisambiguation: TextView
    private lateinit var mPerformerScenes: ListRowView
    private var gridAdapter: ArrayObjectAdapter = ArrayObjectAdapter(ScenePresenter())

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        mPerformerImage = view.findViewById(R.id.performer_image)
        mPerformerName = view.findViewById(R.id.performer_name)
        mPerformerDisambiguation = view.findViewById(R.id.performer_disambiguation)

        val performer = requireActivity().intent.getParcelableExtra<Performer>("performer")
        if (performer != null) {
            mPerformerName.text = performer.name
            mPerformerDisambiguation.text = performer.disambiguation

            val queryEngine = QueryEngine(requireContext(), true)
            viewLifecycleOwner.lifecycleScope.async {
                val performers =
                    queryEngine.findPerformers(performerIds = listOf(performer.id.toInt()))
                if (performers.isNotEmpty()) {
                    val performerData = performers.first()
                    if (performerData.image_path != null) {
                        val url = createGlideUrl(performerData.image_path, requireContext())
                        Glide.with(requireActivity())
                            .load(url)
                            .centerCrop()
                            .error(R.drawable.default_background)
                            .into(mPerformerImage)
                    }
                }
            }
        } else {
            val intent = Intent(requireActivity(), MainActivity::class.java)
            startActivity(intent)
        }
    }
}