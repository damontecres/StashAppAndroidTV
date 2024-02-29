package com.github.damontecres.stashapp

import android.os.Bundle
import androidx.fragment.app.FragmentActivity
import com.github.damontecres.stashapp.data.Movie

class MovieActivity : FragmentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
//        setContentView(R.layout.activity_performer)
        if (savedInstanceState == null) {
            val movie = this.intent.getParcelableExtra<Movie>("movie")
            // TODO requires updates after https://github.com/damontecres/StashAppAndroidTV/pull/155

//            supportFragmentManager.beginTransaction()
//                .replace(R.id.performer_fragment, MovieFragment())
//                .replace(
//                    R.id.performer_list_fragment,
//                    StashGridFragment(
//                        SceneComparator,
//                        SceneDataSupplier(
//                            FindFilterType(
//                                sort = Optional.present("movie_scene_number"),
//                                direction = Optional.present(SortDirectionEnum.ASC),
//                            ),
//                            SceneFilterType(
//                                movies =
//                                    Optional.present(
//                                        MultiCriterionInput(
//                                            value = Optional.present(listOf(movie?.id.toString())),
//                                            modifier = CriterionModifier.INCLUDES,
//                                        ),
//                                    ),
//                            ),
//                        ),
//                    ),
//                )
//                .commitNow()
        }
    }
}
