package com.github.damontecres.stashapp

import androidx.fragment.app.FragmentManager
import com.apollographql.apollo.api.Optional
import com.github.damontecres.stashapp.api.type.CriterionModifier
import com.github.damontecres.stashapp.api.type.GalleryFilterType
import com.github.damontecres.stashapp.api.type.GroupFilterType
import com.github.damontecres.stashapp.api.type.HierarchicalMultiCriterionInput
import com.github.damontecres.stashapp.api.type.ImageFilterType
import com.github.damontecres.stashapp.api.type.MultiCriterionInput
import com.github.damontecres.stashapp.api.type.PerformerFilterType
import com.github.damontecres.stashapp.api.type.SceneFilterType
import com.github.damontecres.stashapp.api.type.StudioFilterType
import com.github.damontecres.stashapp.data.DataType
import com.github.damontecres.stashapp.suppliers.DataSupplierOverride
import com.github.damontecres.stashapp.suppliers.FilterArgs
import com.github.damontecres.stashapp.util.StashFragmentPagerAdapter

class StudioFragment : TabbedFragment() {
    override fun getTitleText(): String? {
        return requireActivity().intent.getStringExtra("studioName")
    }

    override fun getPagerAdapter(fm: FragmentManager): StashFragmentPagerAdapter {
        val studioId = requireActivity().intent.getIntExtra("studioId", -1)
        val tabTitles =
            mutableListOf(
                StashFragmentPagerAdapter.PagerEntry(DataType.SCENE),
                StashFragmentPagerAdapter.PagerEntry(DataType.GALLERY),
                StashFragmentPagerAdapter.PagerEntry(DataType.IMAGE),
                StashFragmentPagerAdapter.PagerEntry(DataType.PERFORMER),
                StashFragmentPagerAdapter.PagerEntry(DataType.GROUP),
                StashFragmentPagerAdapter.PagerEntry(DataType.TAG),
                StashFragmentPagerAdapter.PagerEntry(
                    getString(R.string.stashapp_subsidiary_studios),
                    DataType.STUDIO,
                ),
            )
        return StudioPagerAdapter(tabTitles, studioId.toString(), fm)
    }

    class StudioPagerAdapter(
        tabs: MutableList<StashFragmentPagerAdapter.PagerEntry>,
        private val studioId: String,
        fm: FragmentManager,
    ) :
        StashFragmentPagerAdapter(tabs, fm) {
        override fun getFragment(position: Int): StashGridFragment {
            val studios =
                Optional.present(
                    HierarchicalMultiCriterionInput(
                        value = Optional.present(listOf(studioId)),
                        modifier = CriterionModifier.INCLUDES,
                    ),
                )
            return if (position == 0) {
                StashGridFragment(
                    dataType = DataType.SCENE,
                    objectFilter = SceneFilterType(studios = studios),
                )
            } else if (position == 1) {
                StashGridFragment(
                    dataType = DataType.GALLERY,
                    objectFilter = GalleryFilterType(studios = studios),
                )
            } else if (position == 2) {
                StashGridFragment(
                    dataType = DataType.IMAGE,
                    objectFilter = ImageFilterType(studios = studios),
                ).withImageGridClickListener()
            } else if (position == 3) {
                StashGridFragment(
                    dataType = DataType.PERFORMER,
                    objectFilter = PerformerFilterType(studios = studios),
                )
            } else if (position == 4) {
                StashGridFragment(
                    dataType = DataType.GROUP,
                    objectFilter = GroupFilterType(studios = studios),
                )
            } else if (position == 5) {
                StashGridFragment(
                    FilterArgs(
                        DataType.TAG,
                        override = DataSupplierOverride.StudioTags(studioId),
                    ),
                )
            } else if (position == 6) {
                StashGridFragment(
                    dataType = DataType.STUDIO,
                    objectFilter =
                        StudioFilterType(
                            parents =
                                Optional.present(
                                    MultiCriterionInput(
                                        value = Optional.present(listOf(studioId)),
                                        modifier = CriterionModifier.INCLUDES,
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
