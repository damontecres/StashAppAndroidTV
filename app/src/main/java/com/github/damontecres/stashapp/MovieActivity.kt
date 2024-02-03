package com.github.damontecres.stashapp

import android.os.Bundle
import androidx.fragment.app.FragmentActivity
import com.apollographql.apollo3.api.Optional
import com.github.damontecres.stashapp.api.type.CriterionModifier
import com.github.damontecres.stashapp.api.type.FindFilterType
import com.github.damontecres.stashapp.api.type.MultiCriterionInput
import com.github.damontecres.stashapp.api.type.SceneFilterType
import com.github.damontecres.stashapp.api.type.SortDirectionEnum
import com.github.damontecres.stashapp.data.Movie
import com.github.damontecres.stashapp.suppliers.SceneDataSupplier
import com.github.damontecres.stashapp.util.SceneComparator

class MovieActivity : FragmentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_performer)
        if (savedInstanceState == null) {
            val movie = this.intent.getParcelableExtra<Movie>("movie")
            supportFragmentManager.beginTransaction()
                .replace(R.id.performer_fragment, MovieFragment())
                .replace(
                    R.id.performer_list_fragment,
                    StashGridFragment(
                        SceneComparator,
                        SceneDataSupplier(
                            FindFilterType(
                                sort = Optional.present("movie_scene_number"),
                                direction = Optional.present(SortDirectionEnum.ASC),
                            ),
                            SceneFilterType(
                                movies =
                                    Optional.present(
                                        MultiCriterionInput(
                                            value = Optional.present(listOf(movie?.id.toString())),
                                            modifier = CriterionModifier.INCLUDES,
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
