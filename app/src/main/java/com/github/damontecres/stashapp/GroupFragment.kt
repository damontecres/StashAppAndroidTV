package com.github.damontecres.stashapp

import android.os.Bundle
import androidx.fragment.app.Fragment
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
import com.github.damontecres.stashapp.data.StashFindFilter
import com.github.damontecres.stashapp.suppliers.DataSupplierOverride
import com.github.damontecres.stashapp.suppliers.FilterArgs
import com.github.damontecres.stashapp.util.StashFragmentPagerAdapter
import com.github.damontecres.stashapp.util.getParcelable

class GroupFragment : TabbedFragment() {
    private lateinit var group: Group

    override fun onCreate(savedInstanceState: Bundle?) {
        group = requireActivity().intent.getParcelable("group", Group::class)!!
        super.onCreate(savedInstanceState)
    }

    override fun getPagerAdapter(fm: FragmentManager): StashFragmentPagerAdapter {
        val pages =
            listOf(
                StashFragmentPagerAdapter.PagerEntry(getString(R.string.stashapp_details)),
                StashFragmentPagerAdapter.PagerEntry(DataType.SCENE),
                StashFragmentPagerAdapter.PagerEntry(DataType.MARKER),
                StashFragmentPagerAdapter.PagerEntry(DataType.TAG),
                StashFragmentPagerAdapter.PagerEntry(getString(R.string.stashapp_containing_groups)),
                StashFragmentPagerAdapter.PagerEntry(getString(R.string.stashapp_sub_groups)),
            )
        return object : StashFragmentPagerAdapter(pages, fm) {
            override fun getFragment(position: Int): Fragment {
                return when (position) {
                    // Details
                    0 -> GroupDetailsFragment()
                    // Scenes
                    1 ->
                        StashGridFragment(
                            dataType = DataType.SCENE,
                            findFilter =
                                StashFindFilter(
                                    SortAndDirection(
                                        "group_scene_number",
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
                    2 -> {
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
                    }
                    // Tags
                    3 ->
                        StashGridFragment(
                            FilterArgs(
                                DataType.TAG,
                                override = DataSupplierOverride.GroupTags(group.id),
                            ),
                        )
                    // containing groups
                    4 ->
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
                    // sub groups
                    5 ->
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
                    else -> throw IllegalArgumentException()
                }
            }
        }
    }

    override fun getTitleText(): String? {
        return group.name
    }
}
