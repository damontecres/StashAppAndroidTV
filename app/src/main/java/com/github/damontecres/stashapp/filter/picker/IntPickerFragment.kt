package com.github.damontecres.stashapp.filter.picker

import android.os.Bundle
import android.text.InputType
import android.widget.Toast
import androidx.core.content.ContextCompat
import androidx.leanback.widget.GuidanceStylist
import androidx.leanback.widget.GuidedAction
import com.apollographql.apollo.api.Optional
import com.github.damontecres.stashapp.R
import com.github.damontecres.stashapp.api.type.CriterionModifier
import com.github.damontecres.stashapp.api.type.IntCriterionInput
import com.github.damontecres.stashapp.api.type.StashDataFilter
import com.github.damontecres.stashapp.filter.CreateFilterGuidedStepFragment
import com.github.damontecres.stashapp.filter.FilterOption
import com.github.damontecres.stashapp.views.getString

class IntPickerFragment(
    val filterOption: FilterOption<StashDataFilter, IntCriterionInput>,
) : CreateFilterGuidedStepFragment() {
    private var curVal: IntCriterionInput? = null

    override fun onCreateGuidance(savedInstanceState: Bundle?): GuidanceStylist.Guidance {
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
        curVal = filterOption.getter(viewModel.objectFilter.value!!).getOrNull()
        val curInt = curVal?.value
        val curModifier = curVal?.modifier ?: CriterionModifier.EQUALS

        // TODO show second value for between
        actions.add(
            GuidedAction.Builder(requireContext())
                .id(VALUE_1)
                .hasNext(true)
                .title(getString(R.string.stashapp_criterion_value))
                .descriptionEditable(true)
                .descriptionEditInputType(InputType.TYPE_CLASS_NUMBER or InputType.TYPE_NUMBER_FLAG_SIGNED)
                .editDescription(curInt?.toString())
                .build(),
        )

        actions.add(
            GuidedAction.Builder(requireContext())
                .id(VALUE_2)
                .hasNext(true)
                .title(getString(R.string.stashapp_criterion_value))
                .descriptionEditable(true)
                .descriptionEditInputType(InputType.TYPE_CLASS_NUMBER or InputType.TYPE_NUMBER_FLAG_SIGNED)
                .editDescription(curInt?.toString())
                .enabled(curModifier == CriterionModifier.BETWEEN || curModifier == CriterionModifier.NOT_BETWEEN)
                .build(),
        )

        val modifierOptions =
            buildList {
                add(modifierAction(CriterionModifier.EQUALS))
                add(modifierAction(CriterionModifier.NOT_EQUALS))
                add(modifierAction(CriterionModifier.GREATER_THAN))
                add(modifierAction(CriterionModifier.LESS_THAN))
                add(modifierAction(CriterionModifier.BETWEEN))
                add(modifierAction(CriterionModifier.NOT_BETWEEN))
            }
        actions.add(
            GuidedAction.Builder(requireContext())
                .id(MODIFIER_OFFSET)
                .hasNext(false)
                .title("Modifier")
                .description(curModifier.getString(requireContext()))
                .subActions(modifierOptions)
                .build(),
        )

        addStandardActions(actions, filterOption)
    }

    override fun onGuidedActionEditedAndProceed(action: GuidedAction): Long {
        if (action.id == VALUE_1) {
            // The value was changed, so check if it valid or not
            val desc = action.description
            try {
                if (desc != null) {
                    desc.toString().toInt()
                    enableFinish(true)
                    return GuidedAction.ACTION_ID_NEXT
                }
            } catch (ex: Exception) {
                Toast.makeText(requireContext(), "Invalid int: $desc", Toast.LENGTH_SHORT).show()
            }
            enableFinish(false)
            return GuidedAction.ACTION_ID_NEXT
        }
        return GuidedAction.ACTION_ID_CURRENT
    }

    override fun onSubGuidedActionClicked(action: GuidedAction): Boolean {
        if (action.id >= MODIFIER_OFFSET) {
            val newModifier = CriterionModifier.entries[(action.id - MODIFIER_OFFSET).toInt()]
            curVal = curVal?.copy(modifier = newModifier) ?: IntCriterionInput(
                value = curVal?.value ?: 0,
                value2 = curVal?.value2 ?: Optional.absent(),
                modifier = newModifier,
            )
            findActionById(MODIFIER_OFFSET).description = newModifier.getString(requireContext())
            notifyActionChanged(findActionPositionById(MODIFIER_OFFSET))

            val value2Action = findActionById(VALUE_2)
            if (newModifier == CriterionModifier.BETWEEN || newModifier == CriterionModifier.NOT_BETWEEN) {
                value2Action.isEnabled = true
            } else {
                value2Action.isEnabled = false
            }
            notifyActionChanged(findActionPositionById(VALUE_2))
        }
        return true
    }

    override fun onGuidedActionClicked(action: GuidedAction) {
        if (action.id == GuidedAction.ACTION_ID_FINISH) {
            val newValue1 = findActionById(VALUE_1).description?.toString()?.toInt()
            val newValue2 = findActionById(VALUE_2).description?.toString()?.toInt()
            val modifier = curVal?.modifier ?: CriterionModifier.EQUALS
            val newValue =
                if (newValue1 != null) {
                    IntCriterionInput(
                        value = newValue1,
                        value2 = Optional.presentIfNotNull(newValue2),
                        modifier = modifier,
                    )
                } else if (modifier == CriterionModifier.IS_NULL || modifier == CriterionModifier.NOT_NULL) {
                    IntCriterionInput(value = 0, modifier = modifier)
                } else {
                    null
                }

            viewModel.updateFilter(filterOption, newValue)
            parentFragmentManager.popBackStack()
        } else {
            onStandardActionClicked(action, filterOption)
        }
    }

    companion object {
        private const val TAG = "IntPickerFragment"

        private const val VALUE_1 = 1L
        private const val VALUE_2 = 2L
    }
}
