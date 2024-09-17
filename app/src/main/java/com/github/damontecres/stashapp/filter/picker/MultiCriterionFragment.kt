package com.github.damontecres.stashapp.filter.picker

import android.os.Bundle
import android.util.Log
import androidx.core.content.ContextCompat
import androidx.fragment.app.commit
import androidx.leanback.widget.GuidanceStylist
import androidx.leanback.widget.GuidedAction
import com.apollographql.apollo.api.Optional
import com.github.damontecres.stashapp.R
import com.github.damontecres.stashapp.api.fragment.StashData
import com.github.damontecres.stashapp.api.type.CriterionModifier
import com.github.damontecres.stashapp.api.type.MultiCriterionInput
import com.github.damontecres.stashapp.api.type.SceneFilterType
import com.github.damontecres.stashapp.data.DataType
import com.github.damontecres.stashapp.filter.CreateFilterActivity
import com.github.damontecres.stashapp.filter.CreateFilterActivity.Companion.MODIFIER_OFFSET
import com.github.damontecres.stashapp.filter.FilterOption
import com.github.damontecres.stashapp.filter.extractDescription
import com.github.damontecres.stashapp.filter.extractTitle
import com.github.damontecres.stashapp.views.getString

class MultiCriterionFragment(
    val dataType: DataType,
    val filterOption: FilterOption<SceneFilterType, MultiCriterionInput>,
    items: Map<String, StashData>,
) : CreateFilterActivity.CreateFilterGuidedStepFragment() {
    val mutableItems = items.toMutableMap()

    override fun onCreateGuidance(savedInstanceState: Bundle?): GuidanceStylist.Guidance {
//        val text =
//            """
//            Include:
//            ${includeNames.joinToString("\n") { "- $it" }}
//
//            Exclude:
//            ${excludeNames.joinToString("\n") { "- $it" }}
//
//            """.trimIndent()

        return GuidanceStylist.Guidance(
            getString(filterOption.nameStringId),
            "",
            null,
            ContextCompat.getDrawable(requireContext(), R.mipmap.stash_logo),
        )
    }

    override fun onCreateActions(
        actions: MutableList<GuidedAction>,
        savedInstanceState: Bundle?,
    ) {
        val curVal =
            filterOption.getter.invoke(
                viewModel.filter.value!!,
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

        actions.add(
            GuidedAction.Builder(requireContext())
                .id(SUBMIT)
                .hasNext(true)
                .title(getString(R.string.stashapp_actions_finish))
                .build(),
        )
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
                    val item = mutableItems[id]!!
                    val title = extractTitle(item)
                    val desc = extractDescription(item)
                    GuidedAction.Builder(requireContext())
                        .id(INCLUDE_OFFSET + index)
                        .title(title)
                        .description(desc)
                        .build()
                }.sortedBy { it.title.toString() },
            )
        }

    override fun onGuidedActionClicked(action: GuidedAction) {
        if (action.id == SUBMIT) {
            parentFragmentManager.popBackStack()
        }
    }

    override fun onSubGuidedActionClicked(action: GuidedAction): Boolean {
        val curVal = filterOption.getter(viewModel.filter.value!!)
        if (action.id >= MODIFIER_OFFSET) {
            val newModifier = CriterionModifier.entries[(action.id - MODIFIER_OFFSET).toInt()]
            val newInput =
                curVal.getOrNull()?.copy(modifier = newModifier) ?: MultiCriterionInput(modifier = newModifier)
            viewModel.updateFilter(filterOption, newInput)
            findActionById(MODIFIER).description = newModifier.getString(requireContext())
            notifyActionChanged(findActionPositionById(MODIFIER))
            return true
        } else if (action.id >= EXCLUDE_OFFSET) {
            TODO()
        } else if (action.id >= INCLUDE_OFFSET) {
            val currentInput = curVal.getOrThrow()!!
            val index = action.id - INCLUDE_OFFSET
            val list = currentInput.value.getOrThrow()!!.toMutableList()
            list.removeAt(index.toInt())
            val newInput = currentInput.copy(value = Optional.present(list))
            viewModel.updateFilter(filterOption, newInput)

            val action = findActionById(INCLUDE_LIST)
            action.subActions = createItemList(list)
            action.description = "${list.size} ${getString(dataType.pluralStringId)}"
            notifyActionChanged(findActionPositionById(INCLUDE_LIST))
            return true
        } else {
            when (action.id) {
                ADD_INCLUDE_ITEM -> {
                    requireActivity().supportFragmentManager.commit {
                        addToBackStack("picker")
                        replace(
                            android.R.id.content,
                            SearchPickerFragment(dataType) { newItem ->
                                Log.v(TAG, "Adding ${newItem.id}")
                                mutableItems[newItem.id] = newItem
                                val currentInput =
                                    curVal.getOrNull()
                                        ?: MultiCriterionInput(
                                            modifier = CriterionModifier.INCLUDES_ALL,
                                        )
                                val list = currentInput.value.getOrNull()?.toMutableList() ?: ArrayList()
                                if (!list.contains(newItem.id)) {
                                    list.add(newItem.id)
                                    val newInput = currentInput.copy(value = Optional.present(list))
                                    Log.v(TAG, "newInput=$newInput")
                                    viewModel.updateFilter(filterOption, newInput)
                                    val action = findActionById(INCLUDE_LIST)
                                    action.subActions = createItemList(list)
                                    action.description =
                                        "${list.size} ${getString(dataType.pluralStringId)}"
                                    notifyActionChanged(findActionPositionById(INCLUDE_LIST))
                                }
                            },
                        )
                    }
                }
                ADD_EXCLUDE_ITEM -> {
                    TODO()
                }
            }
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
