package com.github.damontecres.stashapp

import android.content.Intent
import android.os.Bundle
import androidx.fragment.app.Fragment
import androidx.fragment.app.FragmentActivity
import androidx.fragment.app.FragmentManager
import androidx.leanback.tab.LeanbackTabLayout
import androidx.leanback.tab.LeanbackViewPager
import androidx.leanback.widget.ClassPresenterSelector
import androidx.preference.PreferenceManager
import com.apollographql.apollo3.api.Optional
import com.github.damontecres.stashapp.api.fragment.PerformerData
import com.github.damontecres.stashapp.api.type.CriterionModifier
import com.github.damontecres.stashapp.api.type.GalleryFilterType
import com.github.damontecres.stashapp.api.type.ImageFilterType
import com.github.damontecres.stashapp.api.type.MovieFilterType
import com.github.damontecres.stashapp.api.type.MultiCriterionInput
import com.github.damontecres.stashapp.api.type.PerformerFilterType
import com.github.damontecres.stashapp.api.type.SceneFilterType
import com.github.damontecres.stashapp.data.PerformTogetherAppFilter
import com.github.damontecres.stashapp.data.Performer
import com.github.damontecres.stashapp.presenters.PerformerPresenter
import com.github.damontecres.stashapp.presenters.StashPresenter
import com.github.damontecres.stashapp.suppliers.GalleryDataSupplier
import com.github.damontecres.stashapp.suppliers.ImageDataSupplier
import com.github.damontecres.stashapp.suppliers.MovieDataSupplier
import com.github.damontecres.stashapp.suppliers.PerformerDataSupplier
import com.github.damontecres.stashapp.suppliers.PerformerTagDataSupplier
import com.github.damontecres.stashapp.suppliers.SceneDataSupplier
import com.github.damontecres.stashapp.util.GalleryComparator
import com.github.damontecres.stashapp.util.ImageComparator
import com.github.damontecres.stashapp.util.ListFragmentPagerAdapter
import com.github.damontecres.stashapp.util.MovieComparator
import com.github.damontecres.stashapp.util.PerformerComparator
import com.github.damontecres.stashapp.util.SceneComparator
import com.github.damontecres.stashapp.util.TagComparator
import com.github.damontecres.stashapp.util.getInt

class PerformerActivity : FragmentActivity() {
    private lateinit var performer: Performer

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_performer)
        if (savedInstanceState == null) {
            performer = this.intent.getParcelableExtra<Performer>("performer")!!

            val cardSize =
                PreferenceManager.getDefaultSharedPreferences(this)
                    .getInt("cardSize", getString(R.string.card_size_default))
            // At medium size, 3 scenes fit in the space vs 5 normally
            val columns = cardSize * 3 / 5

            val tabLayout = findViewById<LeanbackTabLayout>(R.id.performer_tab_layout)
            val viewPager = findViewById<LeanbackViewPager>(R.id.performer_view_pager)
            viewPager.adapter = PagerAdapter(columns, supportFragmentManager)
            tabLayout.setupWithViewPager(viewPager)

            supportFragmentManager.beginTransaction()
                .replace(R.id.performer_details, PerformerFragment())
                .commitNow()
        }
    }

    inner class PagerAdapter(
        private val columns: Int,
        fm: FragmentManager,
    ) :
        ListFragmentPagerAdapter(
                listOf(
                    "Scenes",
                    "Galleries",
                    "Images",
                    "Movies",
                    "Tags",
                    "Appears With",
                ),
                fm,
            ) {
        override fun getItem(position: Int): Fragment {
            return if (position == 0) {
                StashGridFragment(
                    SceneComparator,
                    SceneDataSupplier(
                        SceneFilterType(
                            performers =
                                Optional.present(
                                    MultiCriterionInput(
                                        value = Optional.present(listOf(performer.id)),
                                        modifier = CriterionModifier.INCLUDES_ALL,
                                    ),
                                ),
                        ),
                    ),
                    columns,
                )
            } else if (position == 1) {
                StashGridFragment(
                    GalleryComparator,
                    GalleryDataSupplier(
                        GalleryFilterType(
                            performers =
                                Optional.present(
                                    MultiCriterionInput(
                                        value = Optional.present(listOf(performer.id)),
                                        modifier = CriterionModifier.INCLUDES_ALL,
                                    ),
                                ),
                        ),
                    ),
                    columns,
                )
            } else if (position == 2) {
                StashGridFragment(
                    ImageComparator,
                    ImageDataSupplier(
                        ImageFilterType(
                            performers =
                                Optional.present(
                                    MultiCriterionInput(
                                        value = Optional.present(listOf(performer.id)),
                                        modifier = CriterionModifier.INCLUDES_ALL,
                                    ),
                                ),
                        ),
                    ),
                    columns,
                )
            } else if (position == 3) {
                StashGridFragment(
                    MovieComparator,
                    MovieDataSupplier(
                        MovieFilterType(
                            performers =
                                Optional.present(
                                    MultiCriterionInput(
                                        value = Optional.present(listOf(performer.id)),
                                        modifier = CriterionModifier.INCLUDES_ALL,
                                    ),
                                ),
                        ),
                    ),
                    columns,
                )
            } else if (position == 4) {
                StashGridFragment(
                    TagComparator,
                    PerformerTagDataSupplier(performer.id),
                    columns,
                )
            } else if (position == 5) {
                val presenter =
                    ClassPresenterSelector()
                        .addClassPresenter(
                            PerformerData::class.java,
                            PerformerPresenter(
                                object :
                                    StashPresenter.LongClickCallBack<PerformerData> {
                                    override val popUpItems: List<String>
                                        get() = listOf("View scenes together")

                                    override fun onItemLongClick(
                                        item: PerformerData,
                                        popUpItemPosition: Int,
                                    ) {
                                        val performerIds = listOf(performer.id, item.id)
                                        val name = "${performer.name} & ${item.name}"
                                        val appFilter = PerformTogetherAppFilter(name, performerIds)
                                        val intent =
                                            Intent(
                                                this@PerformerActivity,
                                                FilterListActivity::class.java,
                                            )
                                        intent.putExtra("filter", appFilter)
                                        this@PerformerActivity.startActivity(intent)
                                    }
                                },
                            ),
                        )
                StashGridFragment(
                    presenter,
                    PerformerComparator,
                    PerformerDataSupplier(
                        PerformerFilterType(
                            performers =
                                Optional.present(
                                    MultiCriterionInput(
                                        value = Optional.present(listOf(performer.id)),
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
