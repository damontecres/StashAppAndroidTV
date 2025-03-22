package com.github.damontecres.stashapp

import android.os.Bundle
import android.view.View
import android.widget.Toast
import androidx.fragment.app.viewModels
import com.apollographql.apollo.api.Optional
import com.github.damontecres.stashapp.api.fragment.GroupRelationshipType
import com.github.damontecres.stashapp.api.type.CriterionModifier
import com.github.damontecres.stashapp.api.type.HierarchicalMultiCriterionInput
import com.github.damontecres.stashapp.api.type.SceneFilterType
import com.github.damontecres.stashapp.api.type.SceneMarkerFilterType
import com.github.damontecres.stashapp.api.type.StashDataFilter
import com.github.damontecres.stashapp.data.DataType
import com.github.damontecres.stashapp.data.StashFindFilter
import com.github.damontecres.stashapp.suppliers.DataSupplierOverride
import com.github.damontecres.stashapp.suppliers.FilterArgs
import com.github.damontecres.stashapp.util.PageFilterKey
import com.github.damontecres.stashapp.util.StashFragmentPagerAdapter
import com.github.damontecres.stashapp.util.getUiTabs
import com.github.damontecres.stashapp.views.models.GroupViewModel

class GroupFragment : TabbedFragment(DataType.GROUP.name) {
    private val viewModel: GroupViewModel by viewModels()

    override fun onViewCreated(
        view: View,
        savedInstanceState: Bundle?,
    ) {
        super.onViewCreated(view, savedInstanceState)

        serverViewModel.currentServer.observe(viewLifecycleOwner) {
            viewModel.init(requireArguments())
        }
        serverViewModel
            .withLiveData(viewModel.item)
            .observe(viewLifecycleOwner) { (server, group) ->
                if (group == null) {
                    Toast
                        .makeText(
                            requireContext(),
                            "Group '${viewModel.itemId}' not found",
                            Toast.LENGTH_LONG,
                        ).show()
                    serverViewModel.navigationManager.goBack()
                    return@observe
                }
                tabViewModel.title.value = group.name
                val groupSceneFilter =
                    serverViewModel.requireServer().serverPreferences.getDefaultPageFilter(
                        PageFilterKey.GROUP_SCENES,
                    )
                val subGroupFilter =
                    serverViewModel.requireServer().serverPreferences.getDefaultPageFilter(
                        PageFilterKey.GROUP_SUB_GROUPS,
                    )
                tabViewModel.tabs.value =
                    listOf(
                        StashFragmentPagerAdapter.PagerEntry(getString(R.string.stashapp_details)) {
                            GroupDetailsFragment()
                        },
                        StashFragmentPagerAdapter.PagerEntry(DataType.SCENE) {
                            createStashGridFragment(
                                groupId = group.id,
                                dataType = DataType.SCENE,
                                defaultFindFilter = groupSceneFilter.findFilter,
                                createObjectFilter = { SceneFilterType(groups = it) },
                            )
                        },
                        StashFragmentPagerAdapter.PagerEntry(DataType.MARKER) {
                            createStashGridFragment(
                                groupId = group.id,
                                dataType = DataType.MARKER,
                                defaultFindFilter = null,
                                createObjectFilter = {
                                    SceneMarkerFilterType(
                                        scene_filter =
                                            Optional.present(
                                                SceneFilterType(
                                                    groups = it,
                                                ),
                                            ),
                                    )
                                },
                            )
                        },
                        StashFragmentPagerAdapter.PagerEntry(DataType.TAG) {
                            StashGridControlsFragment(
                                FilterArgs(
                                    DataType.TAG,
                                    override = DataSupplierOverride.GroupTags(group.id),
                                ),
                            )
                        },
                        StashFragmentPagerAdapter.PagerEntry(getString(R.string.stashapp_containing_groups)) {
                            StashGridControlsFragment(
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
                            StashGridControlsFragment(
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

    private fun createStashGridFragment(
        groupId: String,
        dataType: DataType,
        defaultFindFilter: StashFindFilter?,
        createObjectFilter: (Optional<HierarchicalMultiCriterionInput>) -> StashDataFilter,
    ): StashGridControlsFragment {
        val fragment =
            StashGridControlsFragment(
                dataType = dataType,
                findFilter = defaultFindFilter,
                objectFilter = createObjectFilter(createCriterionInput(false, groupId)),
            )
        fragment.subContentSwitchInitialIsChecked = false
        fragment.subContentText = getString(R.string.stashapp_include_sub_group_content)
        fragment.subContentSwitchCheckedListener = { isChecked ->
            val newFilter =
                fragment.currentFilter.copy(
                    objectFilter =
                        createObjectFilter(
                            createCriterionInput(isChecked, groupId),
                        ),
                )
            fragment.currentFilter = newFilter
        }
        return fragment
    }

    private fun createCriterionInput(
        subTags: Boolean,
        groupId: String,
    ): Optional.Present<HierarchicalMultiCriterionInput> =
        Optional.present(
            HierarchicalMultiCriterionInput(
                value = Optional.present(listOf(groupId)),
                modifier = CriterionModifier.INCLUDES_ALL,
                depth = Optional.present(if (subTags) -1 else 0),
            ),
        )
}
