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
import com.github.damontecres.stashapp.filter.CreateFilterActivity
import com.github.damontecres.stashapp.filter.CreateFilterActivity.Companion.MODIFIER_OFFSET
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
) : CreateFilterActivity.CreateFilterGuidedStepFragment() {
    private var curVal: DateCriterionInput? = null

    private val format = SimpleDateFormat("yyyy-MM-dd", Locale.US)

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
        val currDateStr = curVal?.value?.ifBlank { null }
        val curModifier = curVal?.modifier ?: CriterionModifier.EQUALS

        val dateLong =
            if (currDateStr != null) {
                try {
                    format.parse(currDateStr)?.time ?: Date().time
                } catch (ex: ParseException) {
                    Log.w(TAG, "Parse error: $ex")
                    Date().time
                }
            } else {
                Date().time
            }

        // TODO show second value for between
        actions.add(
            GuidedDatePickerAction.Builder(requireContext())
                .id(1L)
                .hasNext(true)
                .title(getString(R.string.stashapp_criterion_value))
                .date(dateLong)
                .build(),
        )

        val modifierOptions =
            buildList {
                add(modifierAction(CriterionModifier.EQUALS))
                add(modifierAction(CriterionModifier.NOT_EQUALS))
                add(modifierAction(CriterionModifier.GREATER_THAN))
                add(modifierAction(CriterionModifier.LESS_THAN))
                add(modifierAction(CriterionModifier.IS_NULL))
                add(modifierAction(CriterionModifier.NOT_NULL))
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
                .title(getString(R.string.stashapp_actions_save))
                .build(),
        )

        actions.add(
            GuidedAction.Builder(requireContext())
                .id(GuidedAction.ACTION_ID_CANCEL)
                .hasNext(true)
                .title(getString(R.string.stashapp_actions_cancel))
                .build(),
        )
    }

    override fun onSubGuidedActionClicked(action: GuidedAction): Boolean {
        val curDate = Date((findActionById(1L) as GuidedDatePickerAction).date)
        if (action.id >= MODIFIER_OFFSET) {
            val newModifier = CriterionModifier.entries[(action.id - MODIFIER_OFFSET).toInt()]
            curVal = curVal?.copy(modifier = newModifier) ?: DateCriterionInput(
                value = curVal?.value ?: format.format(curDate),
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
            val curDate = Date((findActionById(1L) as GuidedDatePickerAction).date)
            val dateStr = format.format(curDate)
            val modifier = curVal?.modifier ?: CriterionModifier.EQUALS
            val newValue =
                if (modifier == CriterionModifier.IS_NULL || modifier == CriterionModifier.NOT_NULL) {
                    DateCriterionInput(value = "", modifier = modifier)
                } else {
                    DateCriterionInput(value = dateStr, modifier = modifier)
                }

            viewModel.updateFilter(filterOption, newValue)
            parentFragmentManager.popBackStack()
        } else if (action.id == GuidedAction.ACTION_ID_CANCEL) {
            parentFragmentManager.popBackStack()
        }
    }

    companion object {
        private const val TAG = "FloatPickerFragment"
        private const val MODIFIER = 2L
    }
}
