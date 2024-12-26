package com.github.damontecres.stashapp

import android.os.Bundle
import android.view.View
import android.widget.Toast
import androidx.fragment.app.viewModels
import com.apollographql.apollo.api.Optional
import com.github.damontecres.stashapp.api.type.CriterionModifier
import com.github.damontecres.stashapp.api.type.HierarchicalMultiCriterionInput
import com.github.damontecres.stashapp.api.type.SceneFilterType
import com.github.damontecres.stashapp.api.type.SceneMarkerFilterType
import com.github.damontecres.stashapp.data.DataType
import com.github.damontecres.stashapp.data.GroupRelationshipType
import com.github.damontecres.stashapp.suppliers.DataSupplierOverride
import com.github.damontecres.stashapp.suppliers.FilterArgs
import com.github.damontecres.stashapp.util.PageFilterKey
import com.github.damontecres.stashapp.util.StashFragmentPagerAdapter
import com.github.damontecres.stashapp.util.getUiTabs
import com.github.damontecres.stashapp.views.models.GroupViewModel

class GroupFragment : TabbedFragment(DataType.GROUP.name) {
    private val viewModel: GroupViewModel by viewModels()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        viewModel.init(requireArguments())
    }

    override fun onViewCreated(
        view: View,
        savedInstanceState: Bundle?,
    ) {
        super.onViewCreated(view, savedInstanceState)
        viewModel.item.observe(viewLifecycleOwner) { group ->
            if (group == null) {
                Toast.makeText(requireContext(), "Group not found", Toast.LENGTH_LONG).show()
                serverViewModel.navigationManager.goBack()
                return@observe
            }
            tabViewModel.title.value = group.name
            val groupSceneFilter =
                serverViewModel.requireServer().serverPreferences.getDefaultFilter(PageFilterKey.GROUP_SCENES)
            val subGroupFilter =
                serverViewModel.requireServer().serverPreferences.getDefaultFilter(PageFilterKey.GROUP_SUB_GROUPS)
            tabViewModel.tabs.value =
                listOf(
                    StashFragmentPagerAdapter.PagerEntry(getString(R.string.stashapp_details)) {
                        GroupDetailsFragment()
                    },
                    StashFragmentPagerAdapter.PagerEntry(DataType.SCENE) {
                        StashGridFragment(
                            dataType = DataType.SCENE,
                            findFilter = groupSceneFilter.findFilter,
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
                    // TODO use subgroup filter
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
        }
    }
}
