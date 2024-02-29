package com.github.damontecres.stashapp

import android.os.Bundle
import android.widget.TextView
import androidx.fragment.app.Fragment
import androidx.fragment.app.FragmentActivity
import androidx.fragment.app.FragmentManager
import androidx.leanback.tab.LeanbackTabLayout
import androidx.leanback.tab.LeanbackViewPager
import com.apollographql.apollo3.api.Optional
import com.github.damontecres.stashapp.api.type.CriterionModifier
import com.github.damontecres.stashapp.api.type.HierarchicalMultiCriterionInput
import com.github.damontecres.stashapp.api.type.MovieFilterType
import com.github.damontecres.stashapp.api.type.MultiCriterionInput
import com.github.damontecres.stashapp.api.type.PerformerFilterType
import com.github.damontecres.stashapp.api.type.SceneFilterType
import com.github.damontecres.stashapp.api.type.StudioFilterType
import com.github.damontecres.stashapp.data.DataType
import com.github.damontecres.stashapp.suppliers.MovieDataSupplier
import com.github.damontecres.stashapp.suppliers.PerformerDataSupplier
import com.github.damontecres.stashapp.suppliers.SceneDataSupplier
import com.github.damontecres.stashapp.suppliers.StudioDataSupplier
import com.github.damontecres.stashapp.util.ListFragmentPagerAdapter
import com.github.damontecres.stashapp.util.MovieComparator
import com.github.damontecres.stashapp.util.PerformerComparator
import com.github.damontecres.stashapp.util.SceneComparator
import com.github.damontecres.stashapp.util.StudioComparator

class StudioActivity : FragmentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.tabbed_grid_view)
        if (savedInstanceState == null) {
            val studioId = this.intent.getIntExtra("studioId", -1)
            val studioName = this.intent.getStringExtra("studioName")
            findViewById<TextView>(R.id.grid_title).text = "$studioName"

            val viewPager = findViewById<LeanbackViewPager>(R.id.view_pager)
            val tabLayout = findViewById<LeanbackTabLayout>(R.id.tab_layout)

            val tabTitles =
                listOf(
                    getString(DataType.SCENE.pluralStringId),
                    getString(DataType.PERFORMER.pluralStringId),
                    getString(DataType.MOVIE.pluralStringId),
                    getString(R.string.stashapp_subsidiary_studios),
                )

            val tagAdapter = PagerAdapter(tabTitles, studioId.toString(), supportFragmentManager)
            viewPager.adapter = tagAdapter
            tabLayout.setupWithViewPager(viewPager)
        }
    }

    class PagerAdapter(tabTitles: List<String>, private val studioId: String, fm: FragmentManager) :
        ListFragmentPagerAdapter(tabTitles, fm) {
        override fun getItem(position: Int): Fragment {
            return if (position == 0) {
                StashGridFragment(
                    SceneComparator,
                    SceneDataSupplier(
                        SceneFilterType(
                            studios =
                                Optional.present(
                                    HierarchicalMultiCriterionInput(
                                        value = Optional.present(listOf(studioId)),
                                        modifier = CriterionModifier.INCLUDES,
                                    ),
                                ),
                        ),
                    ),
                )
            } else if (position == 1) {
                StashGridFragment(
                    PerformerComparator,
                    PerformerDataSupplier(
                        PerformerFilterType(
                            studios =
                                Optional.present(
                                    HierarchicalMultiCriterionInput(
                                        value = Optional.present(listOf(studioId)),
                                        modifier = CriterionModifier.INCLUDES,
                                    ),
                                ),
                        ),
                    ),
                )
            } else if (position == 2) {
                StashGridFragment(
                    MovieComparator,
                    MovieDataSupplier(
                        MovieFilterType(
                            studios =
                                Optional.present(
                                    HierarchicalMultiCriterionInput(
                                        value = Optional.present(listOf(studioId)),
                                        modifier = CriterionModifier.INCLUDES,
                                    ),
                                ),
                        ),
                    ),
                )
            } else if (position == 3) {
                StashGridFragment(
                    StudioComparator,
                    StudioDataSupplier(
                        StudioFilterType(
                            parents =
                                Optional.present(
                                    MultiCriterionInput(
                                        value = Optional.present(listOf(studioId)),
                                        modifier = CriterionModifier.INCLUDES,
                                    ),
                                ),
                        ),
                    ),
                )
            } else {
                throw IllegalStateException()
            }
        }
    }
}
