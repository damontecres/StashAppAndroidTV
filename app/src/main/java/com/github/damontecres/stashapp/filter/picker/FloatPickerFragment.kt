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
import com.github.damontecres.stashapp.api.type.FloatCriterionInput
import com.github.damontecres.stashapp.api.type.StashDataFilter
import com.github.damontecres.stashapp.filter.CreateFilterActivity
import com.github.damontecres.stashapp.filter.FilterOption
import com.github.damontecres.stashapp.views.getString

/**
 * Pick a decimal/float value
 */
class FloatPickerFragment(
    private val filterOption: FilterOption<StashDataFilter, FloatCriterionInput>,
) : CreateFilterActivity.CreateFilterGuidedStepFragment() {
    private var curVal: FloatCriterionInput? = null

    override fun onCreate(savedInstanceState: Bundle?) {
        curVal = filterOption.getter(viewModel.objectFilter.value!!).getOrNull()
        super.onCreate(savedInstanceState)
    }

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
        val curInt = curVal?.value
        val curModifier = curVal?.modifier ?: CriterionModifier.EQUALS

        // TODO show second value for between
        actions.add(
            GuidedAction.Builder(requireContext())
                .id(1L)
                .hasNext(true)
                .title(getString(R.string.stashapp_criterion_value))
                .descriptionEditable(true)
                // TODO handle signed vs not?
                .descriptionEditInputType(
                    InputType.TYPE_CLASS_NUMBER or
                        InputType.TYPE_NUMBER_FLAG_SIGNED or
                        InputType.TYPE_NUMBER_FLAG_DECIMAL,
                )
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

        addStandardActions(actions, filterOption)
    }

    override fun onGuidedActionEditedAndProceed(action: GuidedAction): Long {
        if (action.id == 1L) {
            val desc = action.description
            try {
                if (desc != null) {
                    desc.toString().toDouble()
                    enableFinish(true)
                    return GuidedAction.ACTION_ID_NEXT
                }
            } catch (ex: Exception) {
                Toast.makeText(requireContext(), "Invalid decimal: $desc", Toast.LENGTH_SHORT)
                    .show()
            }
            enableFinish(false)
            return GuidedAction.ACTION_ID_NEXT
        }
        return GuidedAction.ACTION_ID_CURRENT
    }

    override fun onSubGuidedActionClicked(action: GuidedAction): Boolean {
        if (action.id >= MODIFIER_OFFSET) {
            val newModifier = CriterionModifier.entries[(action.id - MODIFIER_OFFSET).toInt()]
            curVal = curVal?.copy(modifier = newModifier) ?: FloatCriterionInput(
                value = curVal?.value ?: 0.0,
                value2 = curVal?.value2 ?: Optional.absent(),
                modifier = newModifier,
            )
            findActionById(MODIFIER).description = newModifier.getString(requireContext())
            notifyActionChanged(findActionPositionById(MODIFIER))
        }
        return true
    }

    override fun onGuidedActionClicked(action: GuidedAction) {
        if (action.id == GuidedAction.ACTION_ID_FINISH) {
            val newInt = findActionById(1L).description?.toString()?.toDouble()
            val modifier = curVal?.modifier ?: CriterionModifier.EQUALS
            val newValue =
                if (newInt != null) {
                    FloatCriterionInput(value = newInt, modifier = modifier)
                } else if (modifier == CriterionModifier.IS_NULL || modifier == CriterionModifier.NOT_NULL) {
                    FloatCriterionInput(value = 0.0, modifier = modifier)
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
        private const val TAG = "FloatPickerFragment"
        private const val MODIFIER = 2L
    }
}
