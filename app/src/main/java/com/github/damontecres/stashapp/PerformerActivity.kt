package com.github.damontecres.stashapp

import android.os.Bundle
import androidx.fragment.app.FragmentActivity
import androidx.leanback.widget.ObjectAdapter
import androidx.preference.PreferenceManager
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
import com.github.damontecres.stashapp.util.getInt

class PerformerActivity : FragmentActivity() {
    private lateinit var sceneFragment: StashGridFragment<FindScenesQuery.Data, SlimSceneData>

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_performer)
        if (savedInstanceState == null) {
            val performer = this.intent.getParcelableExtra<Performer>("performer")

            val cardSize =
                PreferenceManager.getDefaultSharedPreferences(this)
                    .getInt("cardSize", getString(R.string.card_size_default))
            // At medium size, 3 scenes fit in the space vs 5 normally
            val columns = cardSize * 3 / 5

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
                    columns,
                )

            supportFragmentManager.beginTransaction()
                .replace(R.id.performer_fragment, PerformerFragment())
                .replace(R.id.performer_list_fragment, sceneFragment)
                .commitNow()

            sceneFragment.pagingAdapter.registerObserver(
                object : ObjectAdapter.DataObserver() {
                    override fun onChanged() {
                        sceneFragment.view!!.requestFocus()
                        sceneFragment.pagingAdapter.unregisterObserver(this)
                    }
                },
            )
        }
    }
}
