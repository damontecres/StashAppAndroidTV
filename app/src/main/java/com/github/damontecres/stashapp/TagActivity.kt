package com.github.damontecres.stashapp

import androidx.fragment.app.FragmentManager
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
import com.github.damontecres.stashapp.util.StashFragmentPagerAdapter
import com.github.damontecres.stashapp.util.StashFragmentPagerAdapter.PagerEntry

class TagActivity : TabbedGridFragmentActivity() {
    override fun getTitleText(): CharSequence? {
        return intent.getStringExtra("tagName")
    }

    override fun getPagerAdapter(): StashFragmentPagerAdapter {
        val tagId = intent.getStringExtra("tagId")!!
        val includeSubTags = intent.getBooleanExtra("includeSubTags", false)
        val tabs =
            listOf(
                PagerEntry(DataType.SCENE),
                PagerEntry(DataType.GALLERY),
                PagerEntry(DataType.IMAGE),
                PagerEntry(DataType.MARKER),
                PagerEntry(DataType.PERFORMER),
                PagerEntry(getString(R.string.stashapp_sub_tags), DataType.TAG),
            )

        return TabPageAdapter(tabs, tagId, includeSubTags, supportFragmentManager)
    }

    class TabPageAdapter(
        tabs: List<PagerEntry>,
        private val tagId: String,
        private val includeSubTags: Boolean,
        fm: FragmentManager,
    ) :
        StashFragmentPagerAdapter(tabs, fm) {
        override fun getFragment(position: Int): StashGridFragment2 {
            val depth = Optional.present(if (includeSubTags) -1 else 0)
            return if (position == 0) {
                StashGridFragment2(
                    dataType = DataType.SCENE,
                    objectFilter =
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
                )
            } else if (position == 1) {
                StashGridFragment2(
                    dataType = DataType.GALLERY,
                    objectFilter =
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
                )
            } else if (position == 2) {
                StashGridFragment2(
                    dataType = DataType.IMAGE,
                    objectFilter =
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
                )
            } else if (position == 3) {
                StashGridFragment2(
                    dataType = DataType.MARKER,
                    objectFilter =
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
                )
            } else if (position == 4) {
                StashGridFragment2(
                    dataType = DataType.PERFORMER,
                    objectFilter =
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
                )
            } else if (position == 5) {
                StashGridFragment2(
                    dataType = DataType.TAG,
                    objectFilter =
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
                )
            } else {
                throw IllegalStateException()
            }
        }
    }
}
