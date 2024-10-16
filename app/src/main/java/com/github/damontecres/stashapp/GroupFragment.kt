package com.github.damontecres.stashapp

import android.os.Bundle
import androidx.fragment.app.FragmentManager
import com.apollographql.apollo.api.Optional
import com.github.damontecres.stashapp.api.type.CriterionModifier
import com.github.damontecres.stashapp.api.type.HierarchicalMultiCriterionInput
import com.github.damontecres.stashapp.api.type.SceneFilterType
import com.github.damontecres.stashapp.api.type.SceneMarkerFilterType
import com.github.damontecres.stashapp.api.type.SortDirectionEnum
import com.github.damontecres.stashapp.data.DataType
import com.github.damontecres.stashapp.data.Group
import com.github.damontecres.stashapp.data.GroupRelationshipType
import com.github.damontecres.stashapp.data.SortAndDirection
import com.github.damontecres.stashapp.data.SortOption
import com.github.damontecres.stashapp.data.StashFindFilter
import com.github.damontecres.stashapp.suppliers.DataSupplierOverride
import com.github.damontecres.stashapp.suppliers.FilterArgs
import com.github.damontecres.stashapp.util.StashFragmentPagerAdapter
import com.github.damontecres.stashapp.util.getParcelable
import com.github.damontecres.stashapp.util.getUiTabs

class GroupFragment : TabbedFragment(DataType.GROUP.name) {
    private lateinit var group: Group

    override fun onCreate(savedInstanceState: Bundle?) {
        group = requireActivity().intent.getParcelable("group", Group::class)!!
        super.onCreate(savedInstanceState)
    }

    override fun getPagerAdapter(fm: FragmentManager): StashFragmentPagerAdapter {
        val pages =
            listOf(
                StashFragmentPagerAdapter.PagerEntry(getString(R.string.stashapp_details)) {
                    GroupDetailsFragment()
                },
                StashFragmentPagerAdapter.PagerEntry(DataType.SCENE) {
                    StashGridFragment(
                        dataType = DataType.SCENE,
                        findFilter =
                            StashFindFilter(
                                SortAndDirection(
                                    SortOption.GROUP_SCENE_NUMBER,
                                    SortDirectionEnum.ASC,
                                ),
                            ),
                        objectFilter =
                            SceneFilterType(
                                groups =
                                    Optional.present(
                                        HierarchicalMultiCriterionInput(
                                            value = Optional.present(listOf(group.id)),
                                            modifier = CriterionModifier.INCLUDES,
                                        ),
                                    ),
                            ),
                    )
                },
                StashFragmentPagerAdapter.PagerEntry(DataType.MARKER) {
                    StashGridFragment(
                        dataType = DataType.MARKER,
                        objectFilter =
                            SceneMarkerFilterType(
                                scene_filter =
                                    Optional.present(
                                        SceneFilterType(
                                            groups =
                                                Optional.present(
                                                    HierarchicalMultiCriterionInput(
                                                        value = Optional.present(listOf(group.id)),
                                                        modifier = CriterionModifier.INCLUDES,
                                                    ),
                                                ),
                                        ),
                                    ),
                            ),
                    )
                },
                StashFragmentPagerAdapter.PagerEntry(DataType.TAG) {
                    StashGridFragment(
                        FilterArgs(
                            DataType.TAG,
                            override = DataSupplierOverride.GroupTags(group.id),
                        ),
                    )
                },
                StashFragmentPagerAdapter.PagerEntry(getString(R.string.stashapp_containing_groups)) {
                    StashGridFragment(
                        FilterArgs(
                            DataType.GROUP,
                            override =
                                DataSupplierOverride.GroupRelationship(
                                    group.id,
                                    GroupRelationshipType.CONTAINING,
                                ),
                        ),
                    )
                },
                StashFragmentPagerAdapter.PagerEntry(getString(R.string.stashapp_sub_groups)) {
                    StashGridFragment(
                        FilterArgs(
                            DataType.GROUP,
                            override =
                                DataSupplierOverride.GroupRelationship(
                                    group.id,
                                    GroupRelationshipType.SUB,
                                ),
                        ),
                    )
                },
            ).filter { it.title in getUiTabs(requireContext(), DataType.GROUP) }
        return StashFragmentPagerAdapter(pages, fm)
    }

    override fun getTitleText(): String {
        return group.name
    }
}
