package com.github.damontecres.stashapp

import android.os.Bundle
import android.widget.TextView
import androidx.fragment.app.FragmentActivity
import com.apollographql.apollo3.api.Optional
import com.github.damontecres.stashapp.api.type.CriterionModifier
import com.github.damontecres.stashapp.api.type.FindFilterType
import com.github.damontecres.stashapp.api.type.HierarchicalMultiCriterionInput
import com.github.damontecres.stashapp.api.type.SceneFilterType
import com.github.damontecres.stashapp.api.type.SortDirectionEnum
import com.github.damontecres.stashapp.data.Tag
import com.github.damontecres.stashapp.suppliers.SceneDataSupplier

class TagActivity : FragmentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_tag)
        if (savedInstanceState == null) {
            val tag = this.intent.getParcelableExtra<Tag>("tag")
            findViewById<TextView>(R.id.tag_title).text = "${tag?.name}"
            supportFragmentManager.beginTransaction()
                .replace(
                    R.id.tag_fragment,
                    StashGridFragment(
                        SceneComparator,
                        SceneDataSupplier(
                            FindFilterType(
                                sort = Optional.present("date"),
                                direction = Optional.present(SortDirectionEnum.DESC),
                            ),
                            SceneFilterType(
                                tags =
                                    Optional.present(
                                        HierarchicalMultiCriterionInput(
                                            value = Optional.present(listOf(tag?.id.toString())),
                                            modifier = CriterionModifier.INCLUDES_ALL,
                                        ),
                                    ),
                            ),
                        ),
                    ),
                ).commitNow()
        }
    }
}
