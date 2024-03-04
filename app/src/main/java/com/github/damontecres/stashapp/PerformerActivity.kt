package com.github.damontecres.stashapp

import android.os.Bundle
import androidx.fragment.app.Fragment
import androidx.fragment.app.FragmentActivity
import androidx.fragment.app.FragmentManager
import androidx.fragment.app.FragmentStatePagerAdapter
import androidx.leanback.tab.LeanbackTabLayout
import androidx.leanback.tab.LeanbackViewPager
import androidx.preference.PreferenceManager
import com.apollographql.apollo3.api.Optional
import com.github.damontecres.stashapp.api.type.CriterionModifier
import com.github.damontecres.stashapp.api.type.MovieFilterType
import com.github.damontecres.stashapp.api.type.MultiCriterionInput
import com.github.damontecres.stashapp.api.type.PerformerFilterType
import com.github.damontecres.stashapp.api.type.SceneFilterType
import com.github.damontecres.stashapp.data.Performer
import com.github.damontecres.stashapp.suppliers.MovieDataSupplier
import com.github.damontecres.stashapp.suppliers.PerformerDataSupplier
import com.github.damontecres.stashapp.suppliers.PerformerTagDataSupplier
import com.github.damontecres.stashapp.suppliers.SceneDataSupplier
import com.github.damontecres.stashapp.util.MovieComparator
import com.github.damontecres.stashapp.util.PerformerComparator
import com.github.damontecres.stashapp.util.SceneComparator
import com.github.damontecres.stashapp.util.TagComparator
import com.github.damontecres.stashapp.util.getInt

class PerformerActivity : FragmentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_performer)
        if (savedInstanceState == null) {
            val performer = this.intent.getParcelableExtra<Performer>("performer")

            val cardSize =
                PreferenceManager.getDefaultSharedPreferences(this)
                    .getInt("cardSize", getString(R.string.card_size_default))
            // At medium size, 3 scenes fit in the space vs 5 normally
            val columns = cardSize * 3 / 5

            val tabLayout = findViewById<LeanbackTabLayout>(R.id.performer_tab_layout)
            val viewPager = findViewById<LeanbackViewPager>(R.id.performer_view_pager)
            viewPager.adapter =
                PagerAdapter(performer!!.id.toString(), columns, supportFragmentManager)
            tabLayout.setupWithViewPager(viewPager)

            supportFragmentManager.beginTransaction()
                .replace(R.id.performer_details, PerformerFragment())
                .commitNow()
        }
    }

    class PagerAdapter(
        private val performerId: String,
        private val columns: Int,
        fm: FragmentManager,
    ) :
        FragmentStatePagerAdapter(fm, BEHAVIOR_RESUME_ONLY_CURRENT_FRAGMENT) {
        override fun getCount(): Int {
            return 4
        }

        override fun getPageTitle(position: Int): CharSequence {
            return when (position) {
                0 -> "Scenes"
                1 -> "Movies"
                2 -> "Tags"
                3 -> "Appears With"
                else -> throw IllegalStateException()
            }
        }

        override fun getItem(position: Int): Fragment {
            return if (position == 0) {
                StashGridFragment(
                    SceneComparator,
                    SceneDataSupplier(
                        SceneFilterType(
                            performers =
                                Optional.present(
                                    MultiCriterionInput(
                                        value = Optional.present(listOf(performerId)),
                                        modifier = CriterionModifier.INCLUDES_ALL,
                                    ),
                                ),
                        ),
                    ),
                    columns,
                )
            } else if (position == 1) {
                StashGridFragment(
                    MovieComparator,
                    MovieDataSupplier(
                        MovieFilterType(
                            performers =
                                Optional.present(
                                    MultiCriterionInput(
                                        value = Optional.present(listOf(performerId)),
                                        modifier = CriterionModifier.INCLUDES_ALL,
                                    ),
                                ),
                        ),
                    ),
                    columns,
                )
            } else if (position == 2) {
                StashGridFragment(
                    TagComparator,
                    PerformerTagDataSupplier(performerId),
                    columns,
                )
            } else if (position == 3) {
                StashGridFragment(
                    PerformerComparator,
                    PerformerDataSupplier(
                        PerformerFilterType(
                            performers =
                                Optional.present(
                                    MultiCriterionInput(
                                        value = Optional.present(listOf(performerId)),
                                        modifier = CriterionModifier.INCLUDES_ALL,
                                    ),
                                ),
                        ),
                    ),
                    columns,
                )
            } else {
                throw IllegalStateException()
            }
        }
    }
}
