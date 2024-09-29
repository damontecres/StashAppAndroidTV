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
import com.github.damontecres.stashapp.filter.CreateFilterGuidedStepFragment
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
    private val filterOption: FilterOption<StashDataFilter, DateCriterionInput>,
) : CreateFilterGuidedStepFragment() {
    private var curVal: DateCriterionInput? = null

    private val format = SimpleDateFormat("yyyy-MM-dd", Locale.US)

    override fun onCreate(savedInstanceState: Bundle?) {
        curVal = viewModel.getValue(filterOption)
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
        val currDateStr = curVal?.value?.ifBlank { null }
        val currDateStr2 = curVal?.value2?.getOrNull()?.ifBlank { null }
        val curModifier = curVal?.modifier ?: CriterionModifier.EQUALS

        val dateLong =
            if (currDateStr != null) {
                try {
                    format.parse(currDateStr)?.time ?: Date().time
                } catch (ex: ParseException) {
                    Log.w(TAG, "Parse error ($currDateStr)", ex)
                    Date().time
                }
            } else {
                Date().time
            }

        val dateLong2 =
            if (currDateStr2 != null) {
                try {
                    format.parse(currDateStr2)?.time ?: Date().time
                } catch (ex: ParseException) {
                    Log.w(TAG, "Parse error ($currDateStr2)", ex)
                    Date().time
                }
            } else {
                Date().time
            }

        val modifierOptions =
            buildList {
                add(modifierAction(CriterionModifier.EQUALS))
                add(modifierAction(CriterionModifier.NOT_EQUALS))
                add(modifierAction(CriterionModifier.GREATER_THAN))
                add(modifierAction(CriterionModifier.LESS_THAN))
                add(modifierAction(CriterionModifier.BETWEEN))
                add(modifierAction(CriterionModifier.NOT_BETWEEN))
                add(modifierAction(CriterionModifier.IS_NULL))
                add(modifierAction(CriterionModifier.NOT_NULL))
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
            GuidedDatePickerAction.Builder(requireContext())
                .id(VALUE)
                .hasNext(true)
                .title(getString(R.string.stashapp_criterion_value))
                .date(dateLong)
                .build(),
        )

        actions.add(
            GuidedDatePickerAction.Builder(requireContext())
                .id(VALUE_2)
                .hasNext(true)
                .title(getString(R.string.stashapp_criterion_value))
                .date(dateLong2)
                .enabled(curModifier == CriterionModifier.BETWEEN || curModifier == CriterionModifier.NOT_BETWEEN)
                .build(),
        )

        addStandardActions(actions, filterOption)
    }

    override fun onSubGuidedActionClicked(action: GuidedAction): Boolean {
        val curDate = Date((findActionById(VALUE) as GuidedDatePickerAction).date)
        if (action.id >= MODIFIER_OFFSET) {
            val newModifier = CriterionModifier.entries[(action.id - MODIFIER_OFFSET).toInt()]
            curVal = curVal?.copy(modifier = newModifier) ?: DateCriterionInput(
                value = curVal?.value ?: format.format(curDate),
                value2 = curVal?.value2 ?: Optional.absent(),
                modifier = newModifier,
            )
            findActionById(MODIFIER).description = newModifier.getString(requireContext())
            notifyActionChanged(findActionPositionById(MODIFIER))

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
            val curDate = Date((findActionById(VALUE) as GuidedDatePickerAction).date)
            val dateStr = format.format(curDate)
            val curDate2 = Date((findActionById(VALUE_2) as GuidedDatePickerAction).date)
            val dateStr2 = format.format(curDate2)

            val modifier = curVal?.modifier ?: CriterionModifier.EQUALS
            val newValue =
                if (modifier == CriterionModifier.IS_NULL || modifier == CriterionModifier.NOT_NULL) {
                    DateCriterionInput(value = "", modifier = modifier)
                } else if (modifier == CriterionModifier.BETWEEN || modifier == CriterionModifier.NOT_BETWEEN) {
                    DateCriterionInput(
                        value = dateStr,
                        value2 = Optional.present(dateStr2),
                        modifier = modifier,
                    )
                } else {
                    DateCriterionInput(value = dateStr, modifier = modifier)
                }

            viewModel.updateFilter(filterOption, newValue)
            parentFragmentManager.popBackStack()
        } else {
            onStandardActionClicked(action, filterOption)
        }
    }

    companion object {
        private const val TAG = "FloatPickerFragment"
        private const val VALUE = 1L
        private const val VALUE_2 = 2L
        private const val MODIFIER = 3L
    }
}
