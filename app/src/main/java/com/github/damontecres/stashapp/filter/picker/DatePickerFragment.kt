package com.github.damontecres.stashapp.filter.picker

import android.os.Bundle
import android.util.Log
import androidx.core.content.ContextCompat
import androidx.leanback.widget.GuidanceStylist
import androidx.leanback.widget.GuidedAction
import androidx.leanback.widget.GuidedDatePickerAction
import com.apollographql.apollo.api.Optional
import com.github.damontecres.stashapp.R
import com.github.damontecres.stashapp.api.type.CriterionModifier
import com.github.damontecres.stashapp.api.type.DateCriterionInput
import com.github.damontecres.stashapp.api.type.StashDataFilter
import com.github.damontecres.stashapp.filter.FilterOption
import com.github.damontecres.stashapp.views.getString
import java.text.ParseException
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

/**
 * Pick a date value
 */
class DatePickerFragment(
    filterOption: FilterOption<StashDataFilter, DateCriterionInput>,
) : TwoValuePicker<String, DateCriterionInput>(filterOption) {
    private val format = SimpleDateFormat("yyyy-MM-dd", Locale.US)

    override val modifierOptions: List<CriterionModifier>
        get() =
            super.modifierOptions +
                listOf(
                    CriterionModifier.IS_NULL,
                    CriterionModifier.NOT_NULL,
                )

    override fun onCreateGuidance(savedInstanceState: Bundle?): GuidanceStylist.Guidance =
        GuidanceStylist.Guidance(
            getString(filterOption.nameStringId),
            "",
            null,
            ContextCompat.getDrawable(requireContext(), R.mipmap.stash_logo),
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
        val dateLong =
            if (value1 != null) {
                try {
                    format.parse(value1!!)?.time ?: Date().time
                } catch (ex: ParseException) {
                    Log.w(TAG, "Parse error ($value1)", ex)
                    Date().time
                }
            } else {
                Date().time
            }

        val dateLong2 =
            if (value2 != null) {
                try {
                    format.parse(value2!!)?.time ?: Date().time
                } catch (ex: ParseException) {
                    Log.w(TAG, "Parse error ($value2)", ex)
                    Date().time
                }
            } else {
                Date().time
            }

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
            GuidedDatePickerAction
                .Builder(requireContext())
                .id(VALUE_1)
                .hasNext(true)
                .title(valueText)
                .date(dateLong)
                .build(),
        )

        if (modifier.hasTwoValues()) {
            actions.add(
                GuidedDatePickerAction
                    .Builder(requireContext())
                    .id(VALUE_2)
                    .hasNext(true)
                    .title(getString(R.string.stashapp_criterion_less_than))
                    .date(dateLong2)
                    .build(),
            )
        }

        addStandardActions(actions, filterOption)

        return actions
    }

    override fun parseAction(action: GuidedAction?): String? {
        if (action is GuidedDatePickerAction) {
            return format.format(action.date)
        }
        return null
    }

    override fun createCriterionInput(
        value1: String?,
        value2: String?,
        modifier: CriterionModifier,
    ): DateCriterionInput? =
        if (value1 != null) {
            DateCriterionInput(
                value = value1,
                value2 = Optional.presentIfNotNull(value2),
                modifier = modifier,
            )
        } else if (modifier.isNullModifier()) {
            DateCriterionInput(value = "", modifier = modifier)
        } else {
            null
        }

    override val valueInputType: Int
        get() = throw IllegalStateException("Should not call valueInputType")

    override fun parseValue(v: String?): String = throw IllegalStateException("Should not call parseValue")

    companion object {
        private const val TAG = "DatePickerFragment"
    }
}
