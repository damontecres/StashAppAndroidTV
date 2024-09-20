package com.github.damontecres.stashapp.filter.picker

import android.os.Bundle
import android.util.Log
import androidx.core.content.ContextCompat
import androidx.fragment.app.commit
import androidx.leanback.widget.GuidanceStylist
import androidx.leanback.widget.GuidedAction
import com.apollographql.apollo.api.Optional
import com.github.damontecres.stashapp.R
import com.github.damontecres.stashapp.api.type.CriterionModifier
import com.github.damontecres.stashapp.api.type.MultiCriterionInput
import com.github.damontecres.stashapp.api.type.StashDataFilter
import com.github.damontecres.stashapp.data.DataType
import com.github.damontecres.stashapp.filter.CreateFilterGuidedStepFragment
import com.github.damontecres.stashapp.filter.CreateFilterViewModel
import com.github.damontecres.stashapp.filter.FilterOption
import com.github.damontecres.stashapp.views.getString

class MultiCriterionFragment(
    val dataType: DataType,
    val filterOption: FilterOption<StashDataFilter, MultiCriterionInput>,
) : CreateFilterGuidedStepFragment() {
    private var curVal = MultiCriterionInput(modifier = CriterionModifier.INCLUDES_ALL)

    override fun onCreateGuidance(savedInstanceState: Bundle?): GuidanceStylist.Guidance {
        return GuidanceStylist.Guidance(
            getString(filterOption.nameStringId),
            "Click to remove an item",
            null,
            ContextCompat.getDrawable(requireContext(), R.mipmap.stash_logo),
        )
    }

    override fun onCreateActions(
        actions: MutableList<GuidedAction>,
        savedInstanceState: Bundle?,
    ) {
        curVal =
            filterOption.getter.invoke(
                viewModel.objectFilter.value!!,
            ).getOrNull() ?: MultiCriterionInput(modifier = CriterionModifier.INCLUDES_ALL)

        val modifierOptions =
            buildList {
                add(modifierAction(CriterionModifier.INCLUDES))
                add(modifierAction(CriterionModifier.INCLUDES_ALL))
            }
        actions.add(
            GuidedAction.Builder(requireContext())
                .id(MODIFIER)
                .hasNext(false)
                .title("Modifier")
                .description(curVal.modifier.getString(requireContext()))
                .subActions(modifierOptions)
                .build(),
        )

        val includeItems = createItemList(curVal.value.getOrNull().orEmpty())
        actions.add(
            GuidedAction.Builder(requireContext())
                .id(INCLUDE_LIST)
                .hasNext(false)
                .title(getString(dataType.pluralStringId))
                .description("${includeItems.size - 1} ${getString(dataType.pluralStringId)}")
                .subActions(includeItems)
                .build(),
        )
        // TODO excludes

        addStandardActions(actions, filterOption)
    }

    private fun createItemList(ids: List<String>): List<GuidedAction> =
        buildList {
            add(
                GuidedAction.Builder(requireContext())
                    .id(ADD_INCLUDE_ITEM)
                    .title("Add")
                    .build(),
            )
            addAll(
                ids.mapIndexed { index, id ->
                    val nameDesc =
                        viewModel.storedItems[CreateFilterViewModel.DataTypeId(dataType, id)]
                    GuidedAction.Builder(requireContext())
                        .id(INCLUDE_OFFSET + index)
                        .title(nameDesc?.name)
                        .description(nameDesc?.description)
                        .build()
                }.sortedBy { it.title.toString() },
            )
        }

    override fun onGuidedActionClicked(action: GuidedAction) {
        if (action.id == GuidedAction.ACTION_ID_FINISH) {
            viewModel.updateFilter(filterOption, curVal)
            parentFragmentManager.popBackStack()
        } else {
            onStandardActionClicked(action, filterOption)
        }
    }

    override fun onSubGuidedActionClicked(action: GuidedAction): Boolean {
        if (action.id >= MODIFIER_OFFSET) {
            val newModifier = CriterionModifier.entries[(action.id - MODIFIER_OFFSET).toInt()]
            curVal = curVal.copy(modifier = newModifier)
            findActionById(MODIFIER).description = newModifier.getString(requireContext())
            notifyActionChanged(findActionPositionById(MODIFIER))
            return true
        } else if (action.id >= EXCLUDE_OFFSET) {
            TODO()
        } else if (action.id >= INCLUDE_OFFSET) {
            val index = action.id - INCLUDE_OFFSET
            val list = curVal.value.getOrThrow()!!.toMutableList()
            list.removeAt(index.toInt())
            curVal = curVal.copy(value = Optional.present(list))

            val action = findActionById(INCLUDE_LIST)
            action.subActions = createItemList(list)
            action.description = "${list.size} ${getString(dataType.pluralStringId)}"
            notifyActionChanged(findActionPositionById(INCLUDE_LIST))
            return true
        } else if (action.id == ADD_INCLUDE_ITEM) {
            requireActivity().supportFragmentManager.commit {
                addToBackStack("picker")
                replace(
                    android.R.id.content,
                    SearchPickerFragment(dataType) { newItem ->
                        Log.v(TAG, "Adding ${newItem.id}")
                        viewModel.store(dataType, newItem)
                        val list = curVal.value.getOrNull()?.toMutableList() ?: ArrayList()
                        if (!list.contains(newItem.id)) {
                            list.add(newItem.id)
                            curVal = curVal.copy(value = Optional.present(list))
                            val action = findActionById(INCLUDE_LIST)
                            action.subActions = createItemList(list)
                            action.description =
                                "${list.size} ${getString(dataType.pluralStringId)}"
                            notifyActionChanged(findActionPositionById(INCLUDE_LIST))
                        }
                    },
                )
            }
        } else if (action.id == ADD_EXCLUDE_ITEM) {
            TODO()
        }
        return false
    }

    companion object {
        private const val TAG = "HierarchicalMultiCriterionFragment"

        private const val MODIFIER = 1L
        private const val INCLUDE_LIST = 2L
        private const val ADD_INCLUDE_ITEM = 3L
        private const val ADD_EXCLUDE_ITEM = 4L
        private const val SUBMIT = 5L

        private const val INCLUDE_OFFSET = 1_000_000L
        private const val EXCLUDE_OFFSET = 2_000_000L
    }
}
