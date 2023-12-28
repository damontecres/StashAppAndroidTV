package com.github.damontecres.stashapp

import android.os.Bundle
import androidx.fragment.app.FragmentActivity
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

class PerformerActivity : FragmentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_performer)
        if (savedInstanceState == null) {
            getSupportFragmentManager().beginTransaction()
                .replace(R.id.performer_fragment, PerformerFragment())
                .replace(R.id.performer_list_fragment, StashGridFragment(ScenePresenter()){fragment, adapter ->
                    val apolloClient = createApolloClient(fragment.requireContext())
                    val performer = fragment.requireActivity().intent.getParcelableExtra<Performer>("performer")
                    if (apolloClient != null && performer!=null) {
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
                            adapter.addAll(0, scenes)
                        }
                    }

                })
                .commitNow()
        }
    }
}

