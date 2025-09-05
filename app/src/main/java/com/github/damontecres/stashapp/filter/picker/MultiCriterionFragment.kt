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
import java.util.Locale

class MultiCriterionFragment(
    val dataType: DataType,
    val filterOption: FilterOption<StashDataFilter, MultiCriterionInput>,
) : CreateFilterGuidedStepFragment() {
    private var curVal = MultiCriterionInput(modifier = CriterionModifier.INCLUDES_ALL)

    override fun onCreateGuidance(savedInstanceState: Bundle?): GuidanceStylist.Guidance =
        GuidanceStylist.Guidance(
            getString(filterOption.nameStringId),
            "Click to remove an item",
            null,
            ContextCompat.getDrawable(requireContext(), R.drawable.stash_logo),
        )

    override fun onCreateActions(
        actions: MutableList<GuidedAction>,
        savedInstanceState: Bundle?,
    ) {
        val modifiers =
            filterOption.allowedModifiers ?: listOf(
                CriterionModifier.INCLUDES_ALL,
                CriterionModifier.INCLUDES,
                CriterionModifier.IS_NULL,
                CriterionModifier.NOT_NULL,
            )

        curVal =
            filterOption.getter
                .invoke(
                    viewModel.objectFilter.value!!,
                ).getOrNull() ?: MultiCriterionInput(modifier = modifiers[0])

        val modifierOptions = modifiers.map { modifierAction(it) }

        actions.add(
            GuidedAction
                .Builder(requireContext())
                .id(MODIFIER)
                .hasNext(false)
                .title("Modifier")
                .description(curVal.modifier.getString(requireContext()))
                .subActions(modifierOptions)
                .build(),
        )

        val includeItems = createItemList(curVal.value.getOrNull().orEmpty(), true)
        actions.add(
            GuidedAction
                .Builder(requireContext())
                .id(INCLUDE_LIST)
                .hasNext(false)
                .title(getString(dataType.pluralStringId))
                .description("${includeItems.size - 1} ${getString(dataType.pluralStringId)}")
                .subActions(includeItems)
                .build(),
        )
        val excludeItems = createItemList(curVal.excludes.getOrNull().orEmpty(), false)
        actions.add(
            GuidedAction
                .Builder(requireContext())
                .id(EXCLUDE_LIST)
                .hasNext(false)
                .title(
                    getString(R.string.stashapp_criterion_modifier_excludes).replaceFirstChar {
                        if (it.isLowerCase()) {
                            it.titlecase(
                                Locale.getDefault(),
                            )
                        } else {
                            it.toString()
                        }
                    },
                ).description("${excludeItems.size - 1} ${getString(dataType.pluralStringId)}")
                .subActions(excludeItems)
                .build(),
        )

        addStandardActions(actions, filterOption)
    }

    private fun createItemList(
        ids: List<String>,
        include: Boolean,
    ): List<GuidedAction> =
        buildList {
            add(
                GuidedAction
                    .Builder(requireContext())
                    .id(if (include) ADD_INCLUDE_ITEM else ADD_EXCLUDE_ITEM)
                    .title("Add")
                    .build(),
            )
            addAll(
                ids
                    .mapIndexed { index, id ->
                        val nameDesc =
                            viewModel.storedItems[CreateFilterViewModel.DataTypeId(dataType, id)]
                        GuidedAction
                            .Builder(requireContext())
                            .id(index + if (include) INCLUDE_OFFSET else EXCLUDE_OFFSET)
                            .title(nameDesc?.name)
                            .description(nameDesc?.description)
                            .build()
                    }.sortedBy { it.title.toString() },
            )
        }

    private fun refreshItemList(
        items: List<String>,
        include: Boolean,
    ) {
        val action = findActionById(if (include) INCLUDE_LIST else EXCLUDE_LIST)!!
        action.subActions = createItemList(items, include)
        action.description = "${items.size} ${getString(dataType.pluralStringId)}"
        notifyActionChanged(findActionPositionById(if (include) INCLUDE_LIST else EXCLUDE_LIST))
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
            findActionById(MODIFIER)!!.description = newModifier.getString(requireContext())
            notifyActionChanged(findActionPositionById(MODIFIER))
        } else if (action.id >= EXCLUDE_OFFSET) {
            val index = action.id - EXCLUDE_OFFSET
            val list = curVal.excludes.getOrThrow()!!.toMutableList()
            list.removeAt(index.toInt())
            curVal = curVal.copy(excludes = Optional.present(list))
            refreshItemList(list, false)
        } else if (action.id >= INCLUDE_OFFSET) {
            val index = action.id - INCLUDE_OFFSET
            val list = curVal.value.getOrThrow()!!.toMutableList()
            list.removeAt(index.toInt())
            curVal = curVal.copy(value = Optional.present(list))
            refreshItemList(list, true)
        } else if (action.id == ADD_INCLUDE_ITEM) {
            requireActivity().supportFragmentManager.commit {
                addToBackStack("picker")
                replace(
                    R.id.root_fragment,
                    SearchPickerFragment(dataType) { newItem ->
                        Log.v(TAG, "Adding ${newItem.id}")
                        viewModel.store(dataType, newItem)
                        val list = curVal.value.getOrNull()?.toMutableList() ?: ArrayList()
                        if (!list.contains(newItem.id)) {
                            list.add(newItem.id)
                            curVal = curVal.copy(value = Optional.present(list))
                            refreshItemList(list, true)
                        }
                        checkFinish()
                    },
                )
            }
        } else if (action.id == ADD_EXCLUDE_ITEM) {
            requireActivity().supportFragmentManager.commit {
                addToBackStack("picker")
                replace(
                    R.id.root_fragment,
                    SearchPickerFragment(dataType) { newItem ->
                        Log.v(TAG, "Adding ${newItem.id} to exclude")
                        viewModel.store(dataType, newItem)
                        val list = curVal.excludes.getOrNull()?.toMutableList() ?: ArrayList()
                        if (!list.contains(newItem.id)) {
                            list.add(newItem.id)
                            curVal = curVal.copy(excludes = Optional.present(list))
                            refreshItemList(list, false)
                        }
                        checkFinish()
                    },
                )
            }
        }
        checkFinish()
        return true
    }

    private fun checkFinish() {
        if (curVal.value.getOrNull()?.isNotEmpty() == true ||
            curVal.excludes.getOrNull()?.isNotEmpty() == true
        ) {
            enableFinish(true)
        } else {
            enableFinish(false)
        }
    }

    companion object {
        private const val TAG = "MultiCriterionFragment"

        private const val MODIFIER = 1L
        private const val INCLUDE_LIST = 2L
        private const val EXCLUDE_LIST = 3L
        private const val ADD_INCLUDE_ITEM = 4L
        private const val ADD_EXCLUDE_ITEM = 5L

        private const val INCLUDE_OFFSET = 1_000_000L
        private const val EXCLUDE_OFFSET = 2_000_000L
    }
}
