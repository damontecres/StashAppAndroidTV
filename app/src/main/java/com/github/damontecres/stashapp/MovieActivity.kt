package com.github.damontecres.stashapp

import android.os.Bundle
import androidx.fragment.app.FragmentActivity
import androidx.preference.PreferenceManager
import com.apollographql.apollo3.api.Optional
import com.github.damontecres.stashapp.api.type.CriterionModifier
import com.github.damontecres.stashapp.api.type.MultiCriterionInput
import com.github.damontecres.stashapp.api.type.SceneFilterType
import com.github.damontecres.stashapp.api.type.SortDirectionEnum
import com.github.damontecres.stashapp.data.DataType
import com.github.damontecres.stashapp.data.Movie
import com.github.damontecres.stashapp.data.SortAndDirection
import com.github.damontecres.stashapp.data.StashFindFilter
import com.github.damontecres.stashapp.util.getInt

class MovieActivity : FragmentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_movie)
        if (savedInstanceState == null) {
            val movie = this.intent.getParcelableExtra<Movie>("movie")
            val cardSize =
                PreferenceManager.getDefaultSharedPreferences(this)
                    .getInt("cardSize", getString(R.string.card_size_default))
            // At medium size, 3 scenes fit in the space vs 5 normally
            val columns = cardSize * 3 / 5

            val sceneFragment =
                StashGridFragment2(
                    dataType = DataType.SCENE,
                    findFilter =
                        StashFindFilter(SortAndDirection("movie_scene_number", SortDirectionEnum.ASC)),
                    objectFilter =
                        SceneFilterType(
                            movies =
                                Optional.present(
                                    MultiCriterionInput(
                                        value = Optional.present(listOf(movie?.id.toString())),
                                        modifier = CriterionModifier.INCLUDES,
                                    ),
                                ),
                        ),
                    cardSize = columns,
                )
            sceneFragment.sortButtonEnabled = true
            sceneFragment.requestFocus = true

            supportFragmentManager.beginTransaction()
                .replace(R.id.movie_fragment, MovieFragment())
                .replace(R.id.movie_list_fragment, sceneFragment)
                .commitNow()
        }
    }
}
