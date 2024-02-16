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
import com.github.damontecres.stashapp.suppliers.SceneDataSupplier
import com.github.damontecres.stashapp.util.SceneComparator

class TagActivity : FragmentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.grid_view)
        if (savedInstanceState == null) {
            val tagId = intent.getStringExtra("tagId")!!
            val tagName = intent.getStringExtra("tagName")
            findViewById<TextView>(R.id.grid_title).text = tagName
            supportFragmentManager.beginTransaction()
                .replace(
                    R.id.grid_fragment,
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
                                            value = Optional.present(listOf(tagId)),
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
