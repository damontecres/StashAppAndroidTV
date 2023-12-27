package com.github.damontecres.stashapp

import android.os.Bundle
import android.util.DisplayMetrics
import android.view.View
import android.widget.Toast
import androidx.core.content.ContextCompat
import androidx.fragment.app.Fragment
import androidx.leanback.app.BackgroundManager
import androidx.leanback.app.BrowseSupportFragment
import androidx.leanback.widget.ArrayObjectAdapter
import androidx.leanback.widget.HeaderItem
import androidx.leanback.widget.ListRow
import androidx.leanback.widget.ListRowPresenter
import androidx.lifecycle.lifecycleScope
import com.apollographql.apollo3.api.Optional
import com.github.damontecres.stashapp.api.FindScenesQuery
import com.github.damontecres.stashapp.api.type.CriterionModifier
import com.github.damontecres.stashapp.api.type.FindFilterType
import com.github.damontecres.stashapp.api.type.MultiCriterionInput
import com.github.damontecres.stashapp.api.type.SceneFilterType
import com.github.damontecres.stashapp.api.type.SortDirectionEnum
import com.github.damontecres.stashapp.data.Performer
import kotlinx.coroutines.launch

class PerFormerListFragment : BrowseSupportFragment() {

    private var sceneAdapter: ArrayObjectAdapter= ArrayObjectAdapter(ScenePresenter())

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        headersState = HEADERS_DISABLED
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        val rowsAdapter = ArrayObjectAdapter(ListRowPresenter())
        rowsAdapter.add(ListRow(HeaderItem(0, "Scenes"), sceneAdapter))
        adapter = rowsAdapter
        onItemViewClickedListener = StashItemViewClickListener(requireActivity())
    }

    override fun onResume() {
        super.onResume()
        if(sceneAdapter.size()==0) {
            val apolloClient = createApolloClient(requireContext())
            val performer = requireActivity().intent.getParcelableExtra<Performer>("performer")
            if (apolloClient != null && performer!=null) {
                viewLifecycleOwner.lifecycleScope.launch {
                    // TODO: gross!
                    val result = apolloClient.query(
                        FindScenesQuery(
                            filter = Optional.present(
                                FindFilterType(
                                    sort = Optional.present("date"),
                                    direction = Optional.present(SortDirectionEnum.DESC)
                                )
                            ),
                            scene_filter = Optional.present(
                                SceneFilterType(
                                    performers = Optional.present(
                                        MultiCriterionInput(
                                            value = Optional.present(listOf(performer.id.toString())),
                                            modifier = CriterionModifier.INCLUDES_ALL
                                        )
                                    )
                                )
                            )
                        )
                    ).execute()
                    val scenes = result.data?.findScenes?.scenes?.map {
                        it.slimSceneData
                    }
                    if (!scenes.isNullOrEmpty()) {
                        sceneAdapter.addAll(0, scenes)
                    }
                }
            }
        }
    }
}
