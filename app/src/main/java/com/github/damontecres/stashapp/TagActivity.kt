package com.github.damontecres.stashapp

import android.os.Bundle
import android.widget.TextView
import androidx.fragment.app.Fragment
import androidx.fragment.app.FragmentManager
import androidx.leanback.tab.LeanbackTabLayout
import androidx.leanback.tab.LeanbackViewPager
import com.apollographql.apollo3.api.Optional
import com.github.damontecres.stashapp.api.type.CriterionModifier
import com.github.damontecres.stashapp.api.type.GalleryFilterType
import com.github.damontecres.stashapp.api.type.HierarchicalMultiCriterionInput
import com.github.damontecres.stashapp.api.type.ImageFilterType
import com.github.damontecres.stashapp.api.type.PerformerFilterType
import com.github.damontecres.stashapp.api.type.SceneFilterType
import com.github.damontecres.stashapp.api.type.SceneMarkerFilterType
import com.github.damontecres.stashapp.data.DataType
import com.github.damontecres.stashapp.suppliers.GalleryDataSupplier
import com.github.damontecres.stashapp.suppliers.ImageDataSupplier
import com.github.damontecres.stashapp.suppliers.MarkerDataSupplier
import com.github.damontecres.stashapp.suppliers.PerformerDataSupplier
import com.github.damontecres.stashapp.suppliers.SceneDataSupplier
import com.github.damontecres.stashapp.util.GalleryComparator
import com.github.damontecres.stashapp.util.ImageComparator
import com.github.damontecres.stashapp.util.ListFragmentPagerAdapter
import com.github.damontecres.stashapp.util.MarkerComparator
import com.github.damontecres.stashapp.util.PerformerComparator
import com.github.damontecres.stashapp.util.SceneComparator

class TagActivity : TabbedGridFragmentActivity() {
    private lateinit var tagId: String

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        if (savedInstanceState == null) {
            tagId = intent.getStringExtra("tagId")!!
            val tagName = intent.getStringExtra("tagName")
            findViewById<TextView>(R.id.grid_title).text = tagName

            val viewPager = findViewById<LeanbackViewPager>(R.id.view_pager)
            val tabLayout = findViewById<LeanbackTabLayout>(R.id.tab_layout)

            val tabs =
                listOf(
                    getString(DataType.SCENE.pluralStringId),
                    getString(DataType.GALLERY.pluralStringId),
                    getString(DataType.IMAGE.pluralStringId),
                    getString(DataType.MARKER.pluralStringId),
                    getString(DataType.PERFORMER.pluralStringId),
                )

            val tagAdapter = TabPageAdapter(tabs, tagId, supportFragmentManager)
            viewPager.adapter = tagAdapter
            tabLayout.setupWithViewPager(viewPager)
        }
    }

    class TabPageAdapter(tabs: List<String>, private val tagId: String, fm: FragmentManager) :
        ListFragmentPagerAdapter(tabs, fm) {
        override fun getItem(position: Int): Fragment {
            return if (position == 0) {
                StashGridFragment(
                    SceneComparator,
                    SceneDataSupplier(
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
                    GalleryComparator,
                    GalleryDataSupplier(
                        GalleryFilterType(
                            tags =
                                Optional.present(
                                    HierarchicalMultiCriterionInput(
                                        value = Optional.present(listOf(tagId)),
                                        modifier = CriterionModifier.INCLUDES,
                                    ),
                                ),
                        ),
                    ),
                )
            } else if (position == 2) {
                StashGridFragment(
                    ImageComparator,
                    ImageDataSupplier(
                        ImageFilterType(
                            tags =
                                Optional.present(
                                    HierarchicalMultiCriterionInput(
                                        value = Optional.present(listOf(tagId)),
                                        modifier = CriterionModifier.INCLUDES,
                                    ),
                                ),
                        ),
                    ),
                )
            } else if (position == 3) {
                StashGridFragment(
                    MarkerComparator,
                    MarkerDataSupplier(
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
            } else if (position == 4) {
                StashGridFragment(
                    PerformerComparator,
                    PerformerDataSupplier(
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
