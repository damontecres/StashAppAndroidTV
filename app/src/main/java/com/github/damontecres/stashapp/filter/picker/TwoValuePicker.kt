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

    abstract val valueInputType: Int

    override fun onCreateGuidance(savedInstanceState: Bundle?): GuidanceStylist.Guidance {
        return GuidanceStylist.Guidance(
            getString(filterOption.nameStringId),
            "",
            null,
            ContextCompat.getDrawable(requireContext(), R.mipmap.stash_logo),
        )
    }

    protected fun createActionList(actions: MutableList<GuidedAction>) {
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
                .description(value1?.toString())
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
                    .description(value2?.toString())
                    .build(),
            )
        }

        addStandardActions(actions, filterOption)
    }

    override fun onGuidedActionEditedAndProceed(action: GuidedAction): Long {
        if (action.id == VALUE_1 || action.id == VALUE_2) {
            val desc1 = findActionById(VALUE_1)?.description?.toString()?.ifBlank { null }
            value1 = parseValue(desc1)
            val desc2 = findActionById(VALUE_2)?.description?.toString()?.ifBlank { null }
            value2 = parseValue(desc2)

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
                    Toast.makeText(requireContext(), "Invalid value: $desc1", Toast.LENGTH_SHORT)
                        .show()
                } else {
                    Toast.makeText(requireContext(), "Invalid value2: $desc2", Toast.LENGTH_SHORT)
                        .show()
                }
            }
            enableFinish(false)
            return GuidedAction.ACTION_ID_CURRENT
        }
        return GuidedAction.ACTION_ID_CURRENT
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
            val value1 =
                parseValue(findActionById(VALUE_1)?.description.toString().ifBlank { null })
            val value2 =
                parseValue(findActionById(VALUE_2)?.description?.toString()?.ifBlank { null })
            val newValue = createCriterionInput(value1, value2, modifier)

            viewModel.updateFilter(filterOption, newValue)
            parentFragmentManager.popBackStack()
        } else {
            onStandardActionClicked(action, filterOption)
        }
    }

    protected abstract fun parseValue(v: String?): T?

    protected abstract fun createCriterionInput(
        value1: T?,
        value2: T?,
        modifier: CriterionModifier,
    ): CriterionInput?

    companion object {
        const val VALUE_1 = 1L
        const val VALUE_2 = 2L
        const val MODIFIER = 3L
    }
}
