package com.github.damontecres.stashapp

import android.content.Context
import android.content.Intent
import android.os.Bundle
import androidx.core.view.children
import androidx.fragment.app.FragmentActivity
import androidx.fragment.app.FragmentManager
import androidx.leanback.tab.LeanbackTabLayout
import androidx.leanback.tab.LeanbackViewPager
import androidx.leanback.widget.ClassPresenterSelector
import androidx.preference.PreferenceManager
import com.apollographql.apollo3.api.Optional
import com.github.damontecres.stashapp.api.fragment.PerformerData
import com.github.damontecres.stashapp.api.fragment.TagData
import com.github.damontecres.stashapp.api.type.CriterionModifier
import com.github.damontecres.stashapp.api.type.GalleryFilterType
import com.github.damontecres.stashapp.api.type.ImageFilterType
import com.github.damontecres.stashapp.api.type.MovieFilterType
import com.github.damontecres.stashapp.api.type.MultiCriterionInput
import com.github.damontecres.stashapp.api.type.PerformerFilterType
import com.github.damontecres.stashapp.api.type.SceneFilterType
import com.github.damontecres.stashapp.data.DataType
import com.github.damontecres.stashapp.data.PerformTogetherAppFilter
import com.github.damontecres.stashapp.data.Performer
import com.github.damontecres.stashapp.data.PerformerWithTagAppFilter
import com.github.damontecres.stashapp.presenters.PerformerPresenter
import com.github.damontecres.stashapp.presenters.StashPresenter
import com.github.damontecres.stashapp.presenters.TagPresenter
import com.github.damontecres.stashapp.suppliers.DataSupplierOverride
import com.github.damontecres.stashapp.suppliers.FilterArgs
import com.github.damontecres.stashapp.util.StashFragmentPagerAdapter
import com.github.damontecres.stashapp.util.getInt
import com.github.damontecres.stashapp.views.StashItemViewClickListener

class PerformerActivity : FragmentActivity() {
    private lateinit var performer: Performer

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_performer)
        if (savedInstanceState == null) {
            performer = this.intent.getParcelableExtra("performer")!!

            val cardSize =
                PreferenceManager.getDefaultSharedPreferences(this)
                    .getInt("cardSize", getString(R.string.card_size_default))
            // At medium size, 3 scenes fit in the space vs 5 normally
            val columns = cardSize * 3.0 / 5

            val tabLayout = findViewById<LeanbackTabLayout>(R.id.performer_tab_layout)
            val viewPager = findViewById<LeanbackViewPager>(R.id.performer_view_pager)
            viewPager.adapter = PagerAdapter(columns, supportFragmentManager)
            tabLayout.setupWithViewPager(viewPager)

            tabLayout.nextFocusDownId = R.id.performer_view_pager
            tabLayout.children.forEach { it.nextFocusDownId = R.id.performer_view_pager }

            supportFragmentManager.beginTransaction()
                .replace(R.id.performer_details, PerformerFragment())
                .commitNow()
        }
    }

    inner class PagerAdapter(
        private val columns: Double,
        fm: FragmentManager,
    ) :
        StashFragmentPagerAdapter(
                listOf(
                    PagerEntry(DataType.SCENE),
                    PagerEntry(DataType.GALLERY),
                    PagerEntry(DataType.IMAGE),
                    PagerEntry(DataType.MOVIE),
                    PagerEntry(DataType.TAG),
                    PagerEntry("Appears With", DataType.PERFORMER),
                ),
                fm,
            ) {
        private fun getColumns(dataType: DataType): Int {
            return (columns * dataType.defaultCardRatio).toInt()
        }

        override fun getFragment(position: Int): StashGridFragment2 {
            val performers =
                Optional.present(
                    MultiCriterionInput(
                        value = Optional.present(listOf(performer.id)),
                        modifier = CriterionModifier.INCLUDES_ALL,
                    ),
                )

            val fragment =
                if (position == 0) {
                    StashGridFragment2(
                        dataType = DataType.SCENE,
                        objectFilter = SceneFilterType(performers = performers),
                        cardSize = getColumns(DataType.SCENE),
                    )
                } else if (position == 1) {
                    StashGridFragment2(
                        dataType = DataType.GALLERY,
                        objectFilter = GalleryFilterType(performers = performers),
                        cardSize = getColumns(DataType.GALLERY),
                    )
                } else if (position == 2) {
                    StashGridFragment2(
                        dataType = DataType.IMAGE,
                        objectFilter = ImageFilterType(performers = performers),
                        cardSize = getColumns(DataType.IMAGE),
                    )
                } else if (position == 3) {
                    StashGridFragment2(
                        dataType = DataType.MOVIE,
                        objectFilter = MovieFilterType(performers = performers),
                        cardSize = getColumns(DataType.MOVIE),
                    )
                } else if (position == 4) {
                    val presenter =
                        ClassPresenterSelector()
                            .addClassPresenter(
                                TagData::class.java,
                                TagPresenter(PerformersWithTagLongClickCallback()),
                            )
                    val fragment =
                        StashGridFragment2(
                            FilterArgs(
                                dataType = DataType.TAG,
                                override = DataSupplierOverride.PerformerTags(performer.id),
                            ),
                            cardSize = getColumns(DataType.TAG),
                        )
                    fragment.presenterSelector = presenter
                    fragment
                } else if (position == 5) {
                    val presenter =
                        ClassPresenterSelector()
                            .addClassPresenter(
                                PerformerData::class.java,
                                PerformerPresenter(PerformTogetherLongClickCallback(performer)),
                            )
                    val fragment =
                        StashGridFragment2(
                            dataType = DataType.PERFORMER,
                            objectFilter =
                                PerformerFilterType(
                                    performers =
                                        Optional.present(
                                            MultiCriterionInput(
                                                value = Optional.present(listOf(performer.id)),
                                                modifier = CriterionModifier.INCLUDES_ALL,
                                            ),
                                        ),
                                ),
                            cardSize = getColumns(DataType.PERFORMER),
                        )
                    fragment.presenterSelector = presenter
                    fragment
                } else {
                    throw IllegalStateException()
                }
            fragment.sortButtonEnabled = true
            return fragment
        }
    }

    private class PerformTogetherLongClickCallback(val performer: Performer) :
        StashPresenter.LongClickCallBack<PerformerData> {
        override fun getPopUpItems(
            context: Context,
            item: PerformerData,
        ): List<StashPresenter.PopUpItem> {
            return listOf(
                StashPresenter.PopUpItem.getDefault(context),
                StashPresenter.PopUpItem(1, "View scenes together"),
            )
        }

        override fun onItemLongClick(
            context: Context,
            item: PerformerData,
            popUpItem: StashPresenter.PopUpItem,
        ) {
            when (popUpItem.id) {
                0L -> {
                    StashItemViewClickListener(context).onItemClicked(item)
                }

                1L -> {
                    val performerIds = listOf(performer.id, item.id)
                    val name = "${performer.name} & ${item.name}"
                    val appFilter = PerformTogetherAppFilter(name, performerIds)
                    val intent = Intent(context, FilterListActivity::class.java)
                    intent.putExtra("filter", appFilter)
                    context.startActivity(intent)
                }
            }
        }
    }

    private class PerformersWithTagLongClickCallback :
        StashPresenter.LongClickCallBack<TagData> {
        override fun getPopUpItems(
            context: Context,
            item: TagData,
        ): List<StashPresenter.PopUpItem> {
            return listOf(
                StashPresenter.PopUpItem.getDefault(context),
                StashPresenter.PopUpItem(1, "View performers with this tag"),
            )
        }

        override fun onItemLongClick(
            context: Context,
            item: TagData,
            popUpItem: StashPresenter.PopUpItem,
        ) {
            when (popUpItem.id) {
                StashPresenter.PopUpItem.DEFAULT_ID -> {
                    StashItemViewClickListener(context).onItemClicked(item)
                }

                1L -> {
                    val name = "Performers with ${item.name}"
                    val appFilter = PerformerWithTagAppFilter(name, item.id)
                    val intent = Intent(context, FilterListActivity::class.java)
                    intent.putExtra("filter", appFilter)
                    context.startActivity(intent)
                }
            }
        }
    }
}
