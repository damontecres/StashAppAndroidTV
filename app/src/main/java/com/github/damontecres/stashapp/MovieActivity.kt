package com.github.damontecres.stashapp

import android.os.Bundle
import androidx.fragment.app.Fragment
import com.apollographql.apollo3.api.Optional
import com.github.damontecres.stashapp.api.type.CriterionModifier
import com.github.damontecres.stashapp.api.type.MultiCriterionInput
import com.github.damontecres.stashapp.api.type.SceneFilterType
import com.github.damontecres.stashapp.api.type.SortDirectionEnum
import com.github.damontecres.stashapp.data.DataType
import com.github.damontecres.stashapp.data.Movie
import com.github.damontecres.stashapp.data.SortAndDirection
import com.github.damontecres.stashapp.data.StashFindFilter
import com.github.damontecres.stashapp.util.StashFragmentPagerAdapter

class MovieActivity : TabbedGridFragmentActivity() {
    private lateinit var movie: Movie

    override fun onCreate(savedInstanceState: Bundle?) {
        movie = intent.getParcelableExtra<Movie>("movie")!!
        super.onCreate(savedInstanceState)
    }

    override fun getPagerAdapter(): StashFragmentPagerAdapter {
        val pages =
            listOf(
                StashFragmentPagerAdapter.PagerEntry(getString(R.string.stashapp_details), null),
                StashFragmentPagerAdapter.PagerEntry(DataType.SCENE),
            )
        return object : StashFragmentPagerAdapter(pages, supportFragmentManager) {
            override fun getFragment(position: Int): Fragment {
                return when (position) {
                    0 -> MovieFragment()
                    1 ->
                        StashGridFragment(
                            dataType = DataType.SCENE,
                            findFilter =
                                StashFindFilter(
                                    SortAndDirection(
                                        "movie_scene_number",
                                        SortDirectionEnum.ASC,
                                    ),
                                ),
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
                        )

                    else -> throw IllegalArgumentException()
                }
            }
        }
    }

    override fun getTitleText(): String? {
        return movie.name
    }
}
