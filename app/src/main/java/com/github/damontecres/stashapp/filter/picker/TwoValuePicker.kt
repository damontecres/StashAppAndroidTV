package com.github.damontecres.stashapp.filter.picker

import android.os.Bundle
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
) :
    CreateFilterGuidedStepFragment() {
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

    override fun onCreateGuidance(savedInstanceState: Bundle?): GuidanceStylist.Guidance {
        return GuidanceStylist.Guidance(
            getString(filterOption.nameStringId),
            "",
            null,
            ContextCompat.getDrawable(requireContext(), R.mipmap.stash_logo),
        )
    }

    protected open fun createActionList(actions: MutableList<GuidedAction>) {
        val modifierOptions = this.modifierOptions.map(::modifierAction)
        actions.add(
            GuidedAction.Builder(requireContext())
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
            GuidedAction.Builder(requireContext())
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
                GuidedAction.Builder(requireContext())
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
    }

    override fun onGuidedActionEditedAndProceed(action: GuidedAction): Long {
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
                    Toast.makeText(requireContext(), "Invalid value", Toast.LENGTH_SHORT)
                        .show()
                } else {
                    Toast.makeText(requireContext(), "Invalid value2", Toast.LENGTH_SHORT)
                        .show()
                }
            }
            enableFinish(false)
            return GuidedAction.ACTION_ID_CURRENT
        }
        return GuidedAction.ACTION_ID_CURRENT
    }

    protected open fun parseAction(action: GuidedAction?): T? {
        return parseValue(action?.description?.toString()?.ifBlank { null })
    }

    override fun onSubGuidedActionClicked(action: GuidedAction): Boolean {
        if (action.id >= MODIFIER_OFFSET) {
            modifier = CriterionModifier.entries[(action.id - MODIFIER_OFFSET).toInt()]
            collapseSubActions()
            val actions = mutableListOf<GuidedAction>()
            createActionList(actions)
            this.actions = actions
        }
        return true
    }

    override fun onGuidedActionClicked(action: GuidedAction) {
        if (action.id == GuidedAction.ACTION_ID_FINISH) {
            val value1 = parseAction(findActionById(VALUE_1))
            val value2 = parseAction(findActionById(VALUE_2))
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
    protected open fun formatDescription(v: T?): String? {
        return v?.toString()
    }

    companion object {
        const val VALUE_1 = 1L
        const val VALUE_2 = 2L
        const val MODIFIER = 3L
    }
}
