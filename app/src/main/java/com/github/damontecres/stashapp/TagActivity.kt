package com.github.damontecres.stashapp

import androidx.fragment.app.Fragment
import androidx.fragment.app.FragmentManager
import androidx.viewpager.widget.PagerAdapter
import com.apollographql.apollo3.api.Optional
import com.github.damontecres.stashapp.api.type.CriterionModifier
import com.github.damontecres.stashapp.api.type.GalleryFilterType
import com.github.damontecres.stashapp.api.type.HierarchicalMultiCriterionInput
import com.github.damontecres.stashapp.api.type.ImageFilterType
import com.github.damontecres.stashapp.api.type.PerformerFilterType
import com.github.damontecres.stashapp.api.type.SceneFilterType
import com.github.damontecres.stashapp.api.type.SceneMarkerFilterType
import com.github.damontecres.stashapp.api.type.TagFilterType
import com.github.damontecres.stashapp.data.DataType
import com.github.damontecres.stashapp.suppliers.GalleryDataSupplier
import com.github.damontecres.stashapp.suppliers.ImageDataSupplier
import com.github.damontecres.stashapp.suppliers.MarkerDataSupplier
import com.github.damontecres.stashapp.suppliers.PerformerDataSupplier
import com.github.damontecres.stashapp.suppliers.SceneDataSupplier
import com.github.damontecres.stashapp.suppliers.TagDataSupplier
import com.github.damontecres.stashapp.util.GalleryComparator
import com.github.damontecres.stashapp.util.ImageComparator
import com.github.damontecres.stashapp.util.ListFragmentPagerAdapter
import com.github.damontecres.stashapp.util.MarkerComparator
import com.github.damontecres.stashapp.util.PerformerComparator
import com.github.damontecres.stashapp.util.SceneComparator
import com.github.damontecres.stashapp.util.TagComparator

class TagActivity : TabbedGridFragmentActivity() {
    override fun getTitleText(): CharSequence? {
        // TODO
        return null
//        return intent.getStringExtra("tagName")
    }

    override fun getPagerAdapter(): PagerAdapter {
        val tagId = intent.getStringExtra("id")!!
        val includeSubTags = intent.getBooleanExtra("includeSubTags", false)
        val tabs =
            listOf(
                getString(DataType.SCENE.pluralStringId),
                getString(DataType.GALLERY.pluralStringId),
                getString(DataType.IMAGE.pluralStringId),
                getString(DataType.MARKER.pluralStringId),
                getString(DataType.PERFORMER.pluralStringId),
                getString(R.string.stashapp_sub_tags),
            )

        return TabPageAdapter(tabs, tagId, includeSubTags, supportFragmentManager)
    }

    class TabPageAdapter(
        tabs: List<String>,
        private val tagId: String,
        private val includeSubTags: Boolean,
        fm: FragmentManager,
    ) :
        ListFragmentPagerAdapter(tabs, fm) {
        override fun getItem(position: Int): Fragment {
            val depth = Optional.present(if (includeSubTags) -1 else 0)
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
                                        depth = depth,
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
                                        depth = depth,
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
                                        depth = depth,
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
                                        depth = depth,
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
                                        depth = depth,
                                    ),
                                ),
                        ),
                    ),
                )
            } else if (position == 5) {
                StashGridFragment(
                    TagComparator,
                    TagDataSupplier(
                        DataType.TAG.asDefaultFindFilterType,
                        TagFilterType(
                            parents =
                                Optional.present(
                                    HierarchicalMultiCriterionInput(
                                        value = Optional.present(listOf(tagId)),
                                        modifier = CriterionModifier.INCLUDES_ALL,
                                        depth = depth,
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
