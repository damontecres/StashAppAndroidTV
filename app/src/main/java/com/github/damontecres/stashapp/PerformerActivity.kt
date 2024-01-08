package com.github.damontecres.stashapp

import android.os.Bundle
import com.apollographql.apollo3.api.Optional
import com.github.damontecres.stashapp.api.type.CriterionModifier
import com.github.damontecres.stashapp.api.type.FindFilterType
import com.github.damontecres.stashapp.api.type.MultiCriterionInput
import com.github.damontecres.stashapp.api.type.SceneFilterType
import com.github.damontecres.stashapp.api.type.SortDirectionEnum
import com.github.damontecres.stashapp.data.Performer
import com.github.damontecres.stashapp.suppliers.SceneDataSupplier

class PerformerActivity : SecureFragmentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_performer)
        if (savedInstanceState == null) {
            val performer = this.intent.getParcelableExtra<Performer>("performer")
            supportFragmentManager.beginTransaction()
                .replace(R.id.performer_fragment, PerformerFragment())
                .replace(
                    R.id.performer_list_fragment,
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
                    ),
                )
                .commitNow()
        }
    }
}
