package com.github.damontecres.stashapp.filter.picker

import android.os.Bundle
import android.util.Log
import androidx.core.content.ContextCompat
import androidx.leanback.widget.GuidanceStylist
import androidx.leanback.widget.GuidedAction
import com.apollographql.apollo.api.Optional
import com.github.damontecres.stashapp.R
import com.github.damontecres.stashapp.api.type.CriterionModifier
import com.github.damontecres.stashapp.api.type.IntCriterionInput
import com.github.damontecres.stashapp.api.type.StashDataFilter
import com.github.damontecres.stashapp.filter.FilterOption
import com.github.damontecres.stashapp.views.getString

/**
 * Pick a duration value with [com.github.damontecres.stashapp.views.DurationPicker]
 */
class DurationPickerFragment(
    filterOption: FilterOption<StashDataFilter, IntCriterionInput>,
) : TwoValuePicker<Int, IntCriterionInput>(filterOption) {
    override fun onCreateGuidance(savedInstanceState: Bundle?): GuidanceStylist.Guidance =
        GuidanceStylist.Guidance(
            getString(filterOption.nameStringId),
            "",
            null,
            ContextCompat.getDrawable(requireContext(), R.drawable.stash_logo),
        )

    override fun onCreateActions(
        actions: MutableList<GuidedAction>,
        savedInstanceState: Bundle?,
    ) {
        val curVal = filterOption.getter(viewModel.objectFilter.value!!).getOrNull()
        value1 = curVal?.value
        value2 = curVal?.value2?.getOrNull()
        modifier = curVal?.modifier ?: CriterionModifier.EQUALS
        createActionList(actions)
    }

    override fun createActionList(actions: MutableList<GuidedAction>): List<GuidedAction> {
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
            GuidedDurationPickerAction
                .Builder(requireContext())
                .id(VALUE_1)
                .hasNext(true)
                .title(valueText)
                .duration(value1 ?: 0)
                .build(),
        )

        if (modifier.hasTwoValues()) {
            actions.add(
                GuidedDurationPickerAction
                    .Builder(requireContext())
                    .id(VALUE_2)
                    .hasNext(true)
                    .title(getString(R.string.stashapp_criterion_less_than))
                    .duration(value2 ?: 0)
                    .build(),
            )
        }

        addStandardActions(actions, filterOption)

        return actions
    }

    override fun parseAction(action: GuidedAction?): Int? {
        if (action is GuidedDurationPickerAction) {
            return action.duration
        }
        return null
    }

    override fun createCriterionInput(
        value1: Int?,
        value2: Int?,
        modifier: CriterionModifier,
    ): IntCriterionInput? =
        if (value1 != null) {
            IntCriterionInput(
                value = value1,
                value2 = Optional.presentIfNotNull(value2),
                modifier = modifier,
            )
        } else {
            null
        }

    override val valueInputType: Int
        get() = throw IllegalStateException("Should not call valueInputType")

    override fun parseValue(v: String?): Int? = v?.toIntOrNull()

    companion object {
        private const val TAG = "DatePickerFragment"
    }
}
