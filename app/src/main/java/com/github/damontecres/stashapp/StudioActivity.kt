package com.github.damontecres.stashapp

import android.os.Bundle
import android.widget.TextView
import androidx.fragment.app.Fragment
import androidx.fragment.app.FragmentActivity
import androidx.fragment.app.FragmentManager
import androidx.fragment.app.FragmentStatePagerAdapter
import androidx.leanback.tab.LeanbackTabLayout
import androidx.leanback.tab.LeanbackViewPager
import com.apollographql.apollo3.api.Optional
import com.github.damontecres.stashapp.api.type.CriterionModifier
import com.github.damontecres.stashapp.api.type.FindFilterType
import com.github.damontecres.stashapp.api.type.HierarchicalMultiCriterionInput
import com.github.damontecres.stashapp.api.type.MovieFilterType
import com.github.damontecres.stashapp.api.type.MultiCriterionInput
import com.github.damontecres.stashapp.api.type.PerformerFilterType
import com.github.damontecres.stashapp.api.type.SceneFilterType
import com.github.damontecres.stashapp.api.type.SortDirectionEnum
import com.github.damontecres.stashapp.api.type.StudioFilterType
import com.github.damontecres.stashapp.suppliers.MovieDataSupplier
import com.github.damontecres.stashapp.suppliers.PerformerDataSupplier
import com.github.damontecres.stashapp.suppliers.SceneDataSupplier
import com.github.damontecres.stashapp.suppliers.StudioDataSupplier
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

            val tagAdapter = PagerAdapter(studioId.toString(), supportFragmentManager)
            viewPager.adapter = tagAdapter
            tabLayout.setupWithViewPager(viewPager)
        }
    }

    class PagerAdapter(private val studioId: String, fm: FragmentManager) :
        FragmentStatePagerAdapter(fm, BEHAVIOR_RESUME_ONLY_CURRENT_FRAGMENT) {
        override fun getCount(): Int {
            return 4
        }

        override fun getPageTitle(position: Int): CharSequence? {
            return when (position) {
                0 -> "Scenes"
                1 -> "Performers"
                2 -> "Movies"
                3 -> "Subsidiary Studios"
                else -> throw IllegalStateException()
            }
        }

        override fun getItem(position: Int): Fragment {
            return if (position == 0) {
                StashGridFragment(
                    SceneComparator,
                    SceneDataSupplier(
                        FindFilterType(
                            sort = Optional.present("date"),
                            direction = Optional.present(SortDirectionEnum.DESC),
                        ),
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
                        FindFilterType(
                            sort = Optional.present("name"),
                            direction = Optional.present(SortDirectionEnum.ASC),
                        ),
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
                        FindFilterType(
                            sort = Optional.present("name"),
                            direction = Optional.present(SortDirectionEnum.ASC),
                        ),
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
                        FindFilterType(
                            sort = Optional.present("name"),
                            direction = Optional.present(SortDirectionEnum.ASC),
                        ),
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
