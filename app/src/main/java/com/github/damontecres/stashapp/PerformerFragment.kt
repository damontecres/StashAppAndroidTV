package com.github.damontecres.stashapp

import android.content.Intent
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ArrayAdapter
import android.widget.GridLayout
import android.widget.GridView
import android.widget.ImageView
import android.widget.TextView
import android.widget.Toast
import androidx.fragment.app.Fragment
import androidx.leanback.widget.ArrayObjectAdapter
import androidx.leanback.widget.ListRow
import androidx.leanback.widget.ListRowView
import androidx.leanback.widget.VerticalGridView
import androidx.lifecycle.lifecycleScope
import androidx.preference.PreferenceManager
import com.apollographql.apollo3.api.Optional
import com.bumptech.glide.Glide
import com.github.damontecres.stashapp.api.FindPerformersQuery
import com.github.damontecres.stashapp.api.FindScenesQuery
import com.github.damontecres.stashapp.api.type.CriterionModifier
import com.github.damontecres.stashapp.api.type.MultiCriterionInput
import com.github.damontecres.stashapp.api.type.SceneFilterType
import com.github.damontecres.stashapp.data.Performer
import kotlinx.coroutines.launch

class PerformerFragment : Fragment(R.layout.performer_view) {

    private lateinit var mPerformerImage: ImageView
    private lateinit var mPerformerName: TextView
    private lateinit var mPerformerDisambiguation: TextView
    private lateinit var mPerformerScenes: ListRowView
    private var gridAdapter: ArrayObjectAdapter = ArrayObjectAdapter(ScenePresenter())

//    override fun onCreateView(
//        inflater: LayoutInflater,
//        container: ViewGroup?,
//        savedInstanceState: Bundle?
//    ): View? {
//        val root= inflater.inflate(R.layout.performer_view, container)
//
//        return root
//    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        mPerformerImage = view.findViewById(R.id.performer_image)
        mPerformerName=view.findViewById(R.id.performer_name)
        mPerformerDisambiguation=view.findViewById(R.id.performer_disambiguation)

        val performer = requireActivity().intent.getParcelableExtra<Performer>("performer")
        if(performer!=null) {
            mPerformerName.text = performer.name
            mPerformerDisambiguation.text = performer.disambiguation

            val apolloClient = createApolloClient(requireContext())
            if (apolloClient != null) {
                viewLifecycleOwner.lifecycleScope.launch {
                    val performers = apolloClient.query(
                        FindPerformersQuery(
                            performer_ids = Optional.present(
                                listOf(
                                    performer.id.toInt()
                                )
                            )
                        )
                    ).execute().data?.findPerformers?.performers;
                    if(performers!=null && !performers.isEmpty()){
                        val performer = performers.first().performerData
                        val apiKey = PreferenceManager.getDefaultSharedPreferences(requireContext())
                            .getString("stashApiKey", "")
                        if(performer.image_path!=null) {
                            val url = createGlideUrl(performer.image_path, apiKey)
                            Glide.with(activity!!)
                                .load(url)
                                .centerCrop()
                                .error(R.drawable.default_background)
                                .into(mPerformerImage)
                        }
                    }

                }
            }
        } else {
            val intent = Intent(activity!!, MainActivity::class.java)
            startActivity(intent)
        }
    }
}