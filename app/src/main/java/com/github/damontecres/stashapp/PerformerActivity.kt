package com.github.damontecres.stashapp

import android.os.Bundle
import androidx.fragment.app.FragmentActivity
import androidx.leanback.widget.ObjectAdapter
import com.apollographql.apollo3.api.Optional
import com.github.damontecres.stashapp.api.FindScenesQuery
import com.github.damontecres.stashapp.api.fragment.SlimSceneData
import com.github.damontecres.stashapp.api.type.CriterionModifier
import com.github.damontecres.stashapp.api.type.FindFilterType
import com.github.damontecres.stashapp.api.type.MultiCriterionInput
import com.github.damontecres.stashapp.api.type.SceneFilterType
import com.github.damontecres.stashapp.api.type.SortDirectionEnum
import com.github.damontecres.stashapp.data.Performer
import com.github.damontecres.stashapp.suppliers.SceneDataSupplier
import com.github.damontecres.stashapp.util.SceneComparator

class PerformerActivity : FragmentActivity() {
    private lateinit var sceneFragment: StashGridFragment<FindScenesQuery.Data, SlimSceneData>

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_performer)
        if (savedInstanceState == null) {
            val performer = this.intent.getParcelableExtra<Performer>("performer")

            sceneFragment =
                StashGridFragment(
                    SceneComparator,
                    SceneDataSupplier(
                        FindFilterType(
                            sort = Optional.present("date"),
                            direction = Optional.present(SortDirectionEnum.DESC),
                        ),
                        SceneFilterType(
                            performers =
                                Optional.present(
                                    MultiCriterionInput(
                                        value = Optional.present(listOf(performer?.id.toString())),
                                        modifier = CriterionModifier.INCLUDES_ALL,
                                    ),
                                ),
                        ),
                    ),
                    3,
                )

            supportFragmentManager.beginTransaction()
                .replace(R.id.performer_fragment, PerformerFragment())
                .replace(R.id.performer_list_fragment, sceneFragment)
                .commitNow()

            sceneFragment.mAdapter.registerObserver(
                object : ObjectAdapter.DataObserver() {
                    override fun onChanged() {
                        sceneFragment.view!!.requestFocus()
                        sceneFragment.mAdapter.unregisterObserver(this)
                    }
                },
            )
        }
    }
}
