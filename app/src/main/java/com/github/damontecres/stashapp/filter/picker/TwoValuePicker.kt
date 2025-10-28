package com.github.damontecres.stashapp.filter.picker

import android.os.Bundle
import android.util.Log
import android.widget.Toast
import androidx.core.content.ContextCompat
import androidx.leanback.widget.GuidanceStylist
import androidx.leanback.widget.GuidedAction
import com.github.damontecres.stashapp.R
import com.github.damontecres.stashapp.api.type.CriterionModifier
import com.github.damontecres.stashapp.api.type.StashDataFilter
import com.github.damontecres.stashapp.filter.CreateFilterGuidedStepFragment
import com.github.damontecres.stashapp.filter.FilterOption
import com.github.damontecres.stashapp.views.getString

abstract class TwoValuePicker<T, CriterionInput : Any>(
    val filterOption: FilterOption<StashDataFilter, CriterionInput>,
) : CreateFilterGuidedStepFragment() {
    protected var value1: T? = null
    protected var value2: T? = null
    protected var modifier: CriterionModifier = CriterionModifier.EQUALS

    /**
     * The available [CriterionModifier] options
     */
    protected open val modifierOptions: List<CriterionModifier>
        get() =
            listOf(
                CriterionModifier.EQUALS,
                CriterionModifier.NOT_EQUALS,
                CriterionModifier.GREATER_THAN,
                CriterionModifier.LESS_THAN,
                CriterionModifier.BETWEEN,
                CriterionModifier.NOT_BETWEEN,
            )

    override fun onCreateGuidance(savedInstanceState: Bundle?): GuidanceStylist.Guidance =
        GuidanceStylist.Guidance(
            getString(filterOption.nameStringId),
            "",
            null,
            ContextCompat.getDrawable(requireContext(), R.drawable.stash_logo),
        )

    protected open fun createActionList(actions: MutableList<GuidedAction> = mutableListOf()): List<GuidedAction> {
        Log.v(TAG, "createActionList: actions.size=${actions.size}")
        val modifierOptions = this.modifierOptions.map(::modifierAction)
        actions.add(
            GuidedAction
                .Builder(requireContext())
                .id(MODIFIER)
                .hasNext(false)
                .title("Modifier")
                .description(modifier.getString(requireContext()))
                .subActions(modifierOptions)
                .build(),
        )

        val valueText =
            if (modifier.hasTwoValues()) {
                getString(R.string.stashapp_criterion_greater_than)
            } else {
                getString(R.string.stashapp_criterion_value)
            }
        actions.add(
            GuidedAction
                .Builder(requireContext())
                .id(VALUE_1)
                .hasNext(true)
                .title(valueText)
                .descriptionEditable(true)
                .descriptionEditInputType(valueInputType)
                .description(formatDescription(value1))
                .build(),
        )

        if (modifier.hasTwoValues()) {
            actions.add(
                GuidedAction
                    .Builder(requireContext())
                    .id(VALUE_2)
                    .hasNext(true)
                    .title(getString(R.string.stashapp_criterion_less_than))
                    .descriptionEditable(true)
                    .descriptionEditInputType(valueInputType)
                    .description(formatDescription(value2))
                    .build(),
            )
        }

        addStandardActions(actions, filterOption)

        return actions
    }

    override fun onGuidedActionEditedAndProceed(action: GuidedAction): Long {
        Log.v(TAG, "onGuidedActionEditedAndProceed: ${action.id}")
        if (action.id == VALUE_1 || action.id == VALUE_2) {
            value1 = parseAction(if (action.id == VALUE_1) action else findActionById(VALUE_1))
            value2 = parseAction(if (action.id == VALUE_2) action else findActionById(VALUE_2))

            if (modifier.hasTwoValues()) {
                if (value1 != null && value2 != null) {
                    enableFinish(true)
                    return GuidedAction.ACTION_ID_CURRENT
                }
            } else if (value1 != null) {
                enableFinish(true)
                return GuidedAction.ACTION_ID_CURRENT
            } else {
                if (action.id == VALUE_1) {
                    Toast
                        .makeText(requireContext(), "Invalid value", Toast.LENGTH_SHORT)
                        .show()
                } else {
                    Toast
                        .makeText(requireContext(), "Invalid value2", Toast.LENGTH_SHORT)
                        .show()
                }
            }
            enableFinish(false)
            return GuidedAction.ACTION_ID_CURRENT
        }
        return GuidedAction.ACTION_ID_CURRENT
    }

    protected open fun parseAction(action: GuidedAction?): T? = parseValue(action?.description?.toString()?.ifBlank { null })

    override fun onSubGuidedActionClicked(action: GuidedAction): Boolean {
        Log.v(TAG, "onSubGuidedActionClicked: ${action.id}")
        if (action.id >= MODIFIER_OFFSET) {
            val newModifier = CriterionModifier.entries[(action.id - MODIFIER_OFFSET).toInt()]
            if (modifier != newModifier) {
                modifier = newModifier
                // Since the actions are going to be modified before this function returns to collapse the sub actions,
                // Manually collapse the sub actions, not doing this results in a weird UI reset
                collapseSubActions()
                setActionsDiffCallback(null)
                this.actions = createActionList()
                if (value1 == null || modifier.hasTwoValues() && value2 == null) {
                    enableFinish(false)
                } else {
                    enableFinish(true)
                }
            }
        }
        return true
    }

    override fun onGuidedActionClicked(action: GuidedAction) {
        Log.v(TAG, "onGuidedActionClicked: ${action.id}")
        if (action.id == GuidedAction.ACTION_ID_FINISH) {
            val newValue = createCriterionInput(value1, value2, modifier)
            viewModel.updateFilter(filterOption, newValue)
            parentFragmentManager.popBackStack()
        } else {
            onStandardActionClicked(action, filterOption)
        }
    }

    /**
     * The input type to use for values
     */
    protected abstract val valueInputType: Int

    /**
     * Parse a string into a value
     */
    protected abstract fun parseValue(v: String?): T?

    /**
     * Create the filter from the given values
     */
    protected abstract fun createCriterionInput(
        value1: T?,
        value2: T?,
        modifier: CriterionModifier,
    ): CriterionInput?

    /**
     * Format the description of a value. Defaults to using toString()
     */
    protected open fun formatDescription(v: T?): String? = v?.toString()

    companion object {
        private const val TAG = "TwoValuePicker"

        const val VALUE_1 = 1L
        const val VALUE_2 = 2L
        const val MODIFIER = 3L
    }
}
