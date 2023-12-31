package com.github.damontecres.stashapp

import android.os.Bundle
import android.widget.TextView
import androidx.fragment.app.FragmentActivity
import com.apollographql.apollo3.api.Optional
import com.github.damontecres.stashapp.api.type.CriterionModifier
import com.github.damontecres.stashapp.api.type.HierarchicalMultiCriterionInput
import com.github.damontecres.stashapp.api.type.SceneFilterType
import com.github.damontecres.stashapp.suppliers.SceneDataSupplier


class StudioActivity : SecureFragmentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_tag)
        if (savedInstanceState == null) {
            val studioId = this.intent.getIntExtra("studioId", -1)
            val studioName = this.intent.getStringExtra("studioName")
            findViewById<TextView>(R.id.tag_title).text = "${studioName}"
            getSupportFragmentManager().beginTransaction()
                .replace(
                    R.id.tag_fragment,
                    StashGridFragment(
                        sceneComparator, SceneDataSupplier(
                            SceneFilterType(
                                studios = Optional.present(
                                    HierarchicalMultiCriterionInput(
                                        value = Optional.present(listOf(studioId.toString())),
                                        modifier = CriterionModifier.INCLUDES_ALL
                                    )
                                )
                            )
                        )
                    )
                ).commitNow()
        }
    }
}

