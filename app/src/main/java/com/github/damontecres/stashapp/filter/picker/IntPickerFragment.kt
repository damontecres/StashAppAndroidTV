package com.github.damontecres.stashapp.filter.picker

import android.os.Bundle
import android.text.InputType
import androidx.core.content.ContextCompat
import androidx.leanback.widget.GuidanceStylist
import androidx.leanback.widget.GuidedAction
import com.apollographql.apollo.api.Optional
import com.github.damontecres.stashapp.R
import com.github.damontecres.stashapp.api.type.CriterionModifier
import com.github.damontecres.stashapp.api.type.IntCriterionInput
import com.github.damontecres.stashapp.api.type.StashDataFilter
import com.github.damontecres.stashapp.filter.CreateFilterActivity
import com.github.damontecres.stashapp.filter.CreateFilterActivity.Companion.MODIFIER_OFFSET
import com.github.damontecres.stashapp.filter.FilterOption
import com.github.damontecres.stashapp.views.getString

class IntPickerFragment(
    val filterOption: FilterOption<StashDataFilter, IntCriterionInput>,
) : CreateFilterActivity.CreateFilterGuidedStepFragment() {
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
        val curVal = filterOption.getter(viewModel.filter.value!!).getOrNull()
        val curInt = curVal?.value
        val curModifier = curVal?.modifier ?: CriterionModifier.EQUALS

        // TODO show second value for between
        actions.add(
            GuidedAction.Builder(requireContext())
                .id(1L)
                .hasNext(true)
                .title(getString(R.string.stashapp_criterion_value))
                .descriptionEditable(true)
                .descriptionEditInputType(InputType.TYPE_CLASS_NUMBER or InputType.TYPE_NUMBER_FLAG_SIGNED)
                .editDescription(curInt?.toString())
                .build(),
        )

        val modifierOptions =
            buildList {
                add(modifierAction(CriterionModifier.EQUALS))
                add(modifierAction(CriterionModifier.NOT_EQUALS))
                add(modifierAction(CriterionModifier.GREATER_THAN))
                add(modifierAction(CriterionModifier.LESS_THAN))
                // TODO: between
            }
        actions.add(
            GuidedAction.Builder(requireContext())
                .id(MODIFIER)
                .hasNext(false)
                .title("Modifier")
                .description(curModifier.getString(requireContext()))
                .subActions(modifierOptions)
                .build(),
        )

        actions.add(
            GuidedAction.Builder(requireContext())
                .id(GuidedAction.ACTION_ID_FINISH)
                .hasNext(true)
                .title(getString(R.string.stashapp_actions_finish))
                .build(),
        )
    }

    override fun onSubGuidedActionClicked(action: GuidedAction): Boolean {
        val curVal = filterOption.getter(viewModel.filter.value!!).getOrNull()
        if (action.id >= MODIFIER_OFFSET) {
            val newModifier = CriterionModifier.entries[(action.id - MODIFIER_OFFSET).toInt()]
            val newInput =
                curVal?.copy(modifier = newModifier) ?: IntCriterionInput(
                    value = curVal?.value ?: 0,
                    value2 = curVal?.value2 ?: Optional.absent(),
                    modifier = newModifier,
                )
            viewModel.updateFilter(filterOption, newInput)
            findActionById(MODIFIER).description = newModifier.getString(requireContext())
            notifyActionChanged(findActionPositionById(MODIFIER))
        }
        return true
    }

    override fun onGuidedActionClicked(action: GuidedAction) {
        if (action.id == GuidedAction.ACTION_ID_FINISH) {
            val curVal = filterOption.getter(viewModel.filter.value!!)
            val newInt = findActionById(1L).description?.toString()?.toInt()
            val modifier = curVal.getOrNull()?.modifier ?: CriterionModifier.EQUALS
            val newValue =
                if (newInt != null) {
                    IntCriterionInput(value = newInt, modifier = modifier)
                } else if (modifier == CriterionModifier.IS_NULL || modifier == CriterionModifier.NOT_NULL) {
                    IntCriterionInput(value = 0, modifier = modifier)
                } else {
                    null
                }

            viewModel.updateFilter(filterOption, newValue)
            parentFragmentManager.popBackStack()
        }
    }

    companion object {
        private const val TAG = "StringPickerFragment"
        private const val MODIFIER = 2L
    }
}
