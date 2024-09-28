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
import com.github.damontecres.stashapp.api.type.HierarchicalMultiCriterionInput
import com.github.damontecres.stashapp.api.type.StashDataFilter
import com.github.damontecres.stashapp.data.DataType
import com.github.damontecres.stashapp.filter.CreateFilterGuidedStepFragment
import com.github.damontecres.stashapp.filter.CreateFilterViewModel
import com.github.damontecres.stashapp.filter.FilterOption
import com.github.damontecres.stashapp.views.getString

class HierarchicalMultiCriterionFragment(
    private val dataType: DataType,
    private val filterOption: FilterOption<StashDataFilter, HierarchicalMultiCriterionInput>,
) : CreateFilterGuidedStepFragment() {
    private var curVal = HierarchicalMultiCriterionInput(modifier = CriterionModifier.INCLUDES_ALL)

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
        curVal = filterOption.getter.invoke(
            viewModel.objectFilter.value!!,
        ).getOrNull() ?: HierarchicalMultiCriterionInput(modifier = CriterionModifier.INCLUDES_ALL)

        val modifierOptions =
            buildList {
                add(
                    GuidedAction.Builder(requireContext())
                        .id(MODIFIER_OFFSET + CriterionModifier.INCLUDES.ordinal.toLong())
                        .hasNext(false)
                        .title(CriterionModifier.INCLUDES.getString(requireContext()))
                        .build(),
                )
                add(
                    GuidedAction.Builder(requireContext())
                        .id(MODIFIER_OFFSET + CriterionModifier.INCLUDES_ALL.ordinal.toLong())
                        .hasNext(false)
                        .title(CriterionModifier.INCLUDES_ALL.getString(requireContext()))
                        .build(),
                )
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

        val subValueTitle =
            when (dataType) {
                DataType.TAG -> getString(R.string.stashapp_include_sub_tags)
                DataType.STUDIO -> getString(R.string.stashapp_include_sub_studios)
                DataType.MOVIE -> TODO() // v0.27
                else -> throw IllegalStateException("$dataType not supported")
            }

        actions.add(
            GuidedAction.Builder(requireContext())
                .id(INCLUDE_SUB_VALUES)
                .hasNext(false)
                .title(subValueTitle)
                .checked(curVal.depth.getOrNull() == -1)
                .checkSetId(GuidedAction.CHECKBOX_CHECK_SET_ID)
                .build(),
        )

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
            val subValuesAction = findActionById(INCLUDE_SUB_VALUES)
            if (subValuesAction.isChecked) {
                curVal = curVal.copy(depth = Optional.present(-1))
            }
            viewModel.updateFilter(filterOption, curVal)
            parentFragmentManager.popBackStack()
        } else {
            onStandardActionClicked(action, filterOption)
        }
    }

    override fun onSubGuidedActionClicked(action: GuidedAction): Boolean {
        if (action.id >= MODIFIER_OFFSET) {
            // Update the modifier
            val newModifier = CriterionModifier.entries[(action.id - MODIFIER_OFFSET).toInt()]
            curVal = curVal.copy(modifier = newModifier)
            findActionById(MODIFIER).description = newModifier.getString(requireContext())
            notifyActionChanged(findActionPositionById(MODIFIER))
            return true
        } else if (action.id >= EXCLUDE_OFFSET) {
            TODO()
        } else if (action.id >= INCLUDE_OFFSET) {
            // Item was clicked, so remove it
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
            // Add a new item
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
        private const val INCLUDE_SUB_VALUES = 5L

        private const val INCLUDE_OFFSET = 1_000_000L
        private const val EXCLUDE_OFFSET = 2_000_000L
    }
}
