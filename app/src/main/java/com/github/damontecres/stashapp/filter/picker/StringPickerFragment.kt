package com.github.damontecres.stashapp.filter.picker

import android.os.Bundle
import android.text.InputType
import androidx.core.content.ContextCompat
import androidx.leanback.widget.GuidanceStylist
import androidx.leanback.widget.GuidedAction
import com.github.damontecres.stashapp.R
import com.github.damontecres.stashapp.api.type.CriterionModifier
import com.github.damontecres.stashapp.api.type.StashDataFilter
import com.github.damontecres.stashapp.api.type.StringCriterionInput
import com.github.damontecres.stashapp.filter.CreateFilterGuidedStepFragment
import com.github.damontecres.stashapp.filter.FilterOption
import com.github.damontecres.stashapp.util.isNotNullOrBlank
import com.github.damontecres.stashapp.views.getString

class StringPickerFragment(
    private val filterOption: FilterOption<StashDataFilter, StringCriterionInput>,
) : CreateFilterGuidedStepFragment() {
    private var curVal: StringCriterionInput? = null

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
        val currentString = curVal?.value
        val curModifier = curVal?.modifier ?: CriterionModifier.EQUALS

        actions.add(
            GuidedAction.Builder(requireContext())
                .id(VALUE)
                .hasNext(true)
                .title(getString(R.string.stashapp_criterion_value))
                .descriptionEditable(true)
                .descriptionEditInputType(InputType.TYPE_CLASS_TEXT)
                .description(currentString)
                .enabled(curModifier != CriterionModifier.IS_NULL && curModifier != CriterionModifier.NOT_NULL)
                .build(),
        )

        val modifierOptions =
            buildList {
                add(modifierAction(CriterionModifier.EQUALS))
                add(modifierAction(CriterionModifier.NOT_EQUALS))
                add(modifierAction(CriterionModifier.INCLUDES))
                add(modifierAction(CriterionModifier.EXCLUDES))
                add(modifierAction(CriterionModifier.IS_NULL))
                add(modifierAction(CriterionModifier.NOT_NULL))
                add(modifierAction(CriterionModifier.MATCHES_REGEX))
                add(modifierAction(CriterionModifier.NOT_MATCHES_REGEX))
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

        addStandardActions(actions, filterOption)
    }

    override fun onSubGuidedActionClicked(action: GuidedAction): Boolean {
        if (action.id >= MODIFIER_OFFSET) {
            val newModifier = CriterionModifier.entries[(action.id - MODIFIER_OFFSET).toInt()]
            curVal = curVal?.copy(modifier = newModifier) ?: StringCriterionInput(
                value = "",
                modifier = newModifier,
            )
            findActionById(MODIFIER).description = newModifier.getString(requireContext())
            notifyActionChanged(findActionPositionById(MODIFIER))
            if (newModifier == CriterionModifier.IS_NULL || newModifier == CriterionModifier.NOT_NULL) {
                findActionById(VALUE).isEnabled = false
                notifyActionChanged(findActionPositionById(VALUE))
            } else {
                findActionById(VALUE).isEnabled = true
                notifyActionChanged(findActionPositionById(VALUE))
            }
        }
        return true
    }

    override fun onGuidedActionClicked(action: GuidedAction) {
        if (action.id == GuidedAction.ACTION_ID_FINISH) {
            val newString = findActionById(VALUE).description?.toString()
            val modifier = curVal?.modifier ?: CriterionModifier.EQUALS
            val newValue =
                if (newString.isNotNullOrBlank()) {
                    StringCriterionInput(value = newString, modifier = modifier)
                } else if (modifier == CriterionModifier.IS_NULL || modifier == CriterionModifier.NOT_NULL) {
                    StringCriterionInput(value = "", modifier = modifier)
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
        private const val TAG = "StringPickerFragment"
        private const val VALUE = 1L
        private const val MODIFIER = 2L
    }
}
