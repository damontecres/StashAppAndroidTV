package com.github.damontecres.stashapp

import android.os.Bundle
import androidx.fragment.app.FragmentActivity
import androidx.leanback.widget.ObjectAdapter
import androidx.lifecycle.lifecycleScope
import androidx.preference.PreferenceManager
import com.apollographql.apollo3.api.Optional
import com.github.damontecres.stashapp.api.type.CriterionModifier
import com.github.damontecres.stashapp.api.type.FindFilterType
import com.github.damontecres.stashapp.api.type.MultiCriterionInput
import com.github.damontecres.stashapp.api.type.SceneFilterType
import com.github.damontecres.stashapp.api.type.SortDirectionEnum
import com.github.damontecres.stashapp.suppliers.SceneDataSupplier
import com.github.damontecres.stashapp.util.QueryEngine
import com.github.damontecres.stashapp.util.SceneComparator
import com.github.damontecres.stashapp.util.StashCoroutineExceptionHandler
import com.github.damontecres.stashapp.util.getInt
import kotlinx.coroutines.launch

class MovieActivity : FragmentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_movie)
        if (savedInstanceState == null) {
            val movieId = intent.getStringExtra("id")!!
            lifecycleScope.launch(StashCoroutineExceptionHandler()) {
                val queryEngine = QueryEngine(this@MovieActivity)
                val movie = queryEngine.getMovie(movieId)!!
                val cardSize =
                    PreferenceManager.getDefaultSharedPreferences(this@MovieActivity)
                        .getInt("cardSize", getString(R.string.card_size_default))
                // At medium size, 3 scenes fit in the space vs 5 normally
                val columns = cardSize * 3 / 5

                val sceneFragment =
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
                        columns,
                    )

                supportFragmentManager.beginTransaction()
                    .replace(R.id.movie_fragment, MovieFragment(movie))
                    .replace(R.id.movie_list_fragment, sceneFragment)
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
}
