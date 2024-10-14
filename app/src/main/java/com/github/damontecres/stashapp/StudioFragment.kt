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
import com.github.damontecres.stashapp.util.getUiTabs

class StudioFragment : TabbedFragment(DataType.STUDIO.name) {
    override fun getTitleText(): String? {
        return requireActivity().intent.getStringExtra("studioName")
    }

    override fun getPagerAdapter(fm: FragmentManager): StashFragmentPagerAdapter {
        val studioId = requireActivity().intent.getStringExtra("studioId")!!
        val studios =
            Optional.present(
                HierarchicalMultiCriterionInput(
                    value = Optional.present(listOf(studioId)),
                    modifier = CriterionModifier.INCLUDES,
                ),
            )
        val items =
            listOf(
                StashFragmentPagerAdapter.PagerEntry(DataType.SCENE) {
                    StashGridFragment(
                        dataType = DataType.SCENE,
                        objectFilter = SceneFilterType(studios = studios),
                    )
                },
                StashFragmentPagerAdapter.PagerEntry(DataType.GALLERY) {
                    StashGridFragment(
                        dataType = DataType.GALLERY,
                        objectFilter = GalleryFilterType(studios = studios),
                    )
                },
                StashFragmentPagerAdapter.PagerEntry(DataType.IMAGE) {
                    StashGridFragment(
                        dataType = DataType.IMAGE,
                        objectFilter = ImageFilterType(studios = studios),
                    ).withImageGridClickListener()
                },
                StashFragmentPagerAdapter.PagerEntry(DataType.PERFORMER) {
                    StashGridFragment(
                        dataType = DataType.PERFORMER,
                        objectFilter = PerformerFilterType(studios = studios),
                    )
                },
                StashFragmentPagerAdapter.PagerEntry(DataType.GROUP) {
                    StashGridFragment(
                        dataType = DataType.GROUP,
                        objectFilter = GroupFilterType(studios = studios),
                    )
                },
                StashFragmentPagerAdapter.PagerEntry(DataType.TAG) {
                    StashGridFragment(
                        FilterArgs(
                            DataType.TAG,
                            override = DataSupplierOverride.StudioTags(studioId),
                        ),
                    )
                },
                StashFragmentPagerAdapter.PagerEntry(getString(R.string.stashapp_subsidiary_studios)) {
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
                },
            ).filter { it.title in getUiTabs(requireContext(), DataType.STUDIO) }
        return StashFragmentPagerAdapter(items, fm)
    }
}
