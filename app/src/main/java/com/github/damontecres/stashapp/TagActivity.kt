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
import com.github.damontecres.stashapp.api.type.PerformerFilterType
import com.github.damontecres.stashapp.api.type.SceneFilterType
import com.github.damontecres.stashapp.api.type.SceneMarkerFilterType
import com.github.damontecres.stashapp.api.type.SortDirectionEnum
import com.github.damontecres.stashapp.suppliers.MarkerDataSupplier
import com.github.damontecres.stashapp.suppliers.PerformerDataSupplier
import com.github.damontecres.stashapp.suppliers.SceneDataSupplier
import com.github.damontecres.stashapp.util.MarkerComparator
import com.github.damontecres.stashapp.util.PerformerComparator
import com.github.damontecres.stashapp.util.SceneComparator

class TagActivity : FragmentActivity() {
    private lateinit var tagId: String

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.tabbed_grid_view)
        if (savedInstanceState == null) {
            tagId = intent.getStringExtra("tagId")!!
            val tagName = intent.getStringExtra("tagName")
            findViewById<TextView>(R.id.grid_title).text = tagName

            val viewPager = findViewById<LeanbackViewPager>(R.id.view_pager)
            val tabLayout = findViewById<LeanbackTabLayout>(R.id.tab_layout)

            val tagAdapter = TabPageAdapter(tagId, supportFragmentManager)
            viewPager.adapter = tagAdapter
            tabLayout.setupWithViewPager(viewPager)
        }
    }

    class TabPageAdapter(private val tagId: String, fm: FragmentManager) :
        FragmentStatePagerAdapter(fm, BEHAVIOR_RESUME_ONLY_CURRENT_FRAGMENT) {
        override fun getCount(): Int {
            return 3
        }

        override fun getPageTitle(position: Int): CharSequence? {
            return when (position) {
                0 -> "Scenes"
                1 -> "Markers"
                2 -> "Performers"
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
                            tags =
                                Optional.present(
                                    HierarchicalMultiCriterionInput(
                                        value = Optional.present(listOf(tagId)),
                                        modifier = CriterionModifier.INCLUDES_ALL,
                                    ),
                                ),
                        ),
                    ),
                )
            } else if (position == 1) {
                StashGridFragment(
                    MarkerComparator,
                    MarkerDataSupplier(
                        FindFilterType(
                            sort = Optional.present("created_at"),
                            direction = Optional.present(SortDirectionEnum.DESC),
                        ),
                        SceneMarkerFilterType(
                            tags =
                                Optional.present(
                                    HierarchicalMultiCriterionInput(
                                        value = Optional.present(listOf(tagId)),
                                        modifier = CriterionModifier.INCLUDES_ALL,
                                    ),
                                ),
                        ),
                    ),
                )
            } else if (position == 2) {
                StashGridFragment(
                    PerformerComparator,
                    PerformerDataSupplier(
                        FindFilterType(
                            sort = Optional.present("name"),
                            direction = Optional.present(SortDirectionEnum.DESC),
                        ),
                        PerformerFilterType(
                            tags =
                                Optional.present(
                                    HierarchicalMultiCriterionInput(
                                        value = Optional.present(listOf(tagId)),
                                        modifier = CriterionModifier.INCLUDES_ALL,
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
