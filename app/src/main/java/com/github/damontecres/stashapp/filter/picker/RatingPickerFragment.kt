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
import com.github.damontecres.stashapp.api.type.IntCriterionInput
import com.github.damontecres.stashapp.api.type.StashDataFilter
import com.github.damontecres.stashapp.filter.CreateFilterGuidedStepFragment
import com.github.damontecres.stashapp.filter.FilterOption
import com.github.damontecres.stashapp.views.getString

class RatingPickerFragment(
    private val filterOption: FilterOption<StashDataFilter, IntCriterionInput>,
) : CreateFilterGuidedStepFragment() {
    private var curVal: IntCriterionInput? = null

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
        val curModifier = curVal?.modifier ?: CriterionModifier.EQUALS

        val current =
            if (viewModel.server.value!!.serverPreferences.ratingsAsStars) {
                curVal?.value?.div(20.0)
            } else {
                curVal?.value?.div(10.0)
            }

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

        // TODO show second value for between
        actions.add(
            GuidedAction.Builder(requireContext())
                .id(VALUE)
                .hasNext(true)
                .title(getString(R.string.stashapp_criterion_value))
                .descriptionEditable(true)
                .descriptionEditInputType(InputType.TYPE_CLASS_NUMBER or InputType.TYPE_NUMBER_FLAG_DECIMAL)
                .description(current?.toString())
                .build(),
        )

        actions.add(
            GuidedAction.Builder(requireContext())
                .id(GuidedAction.ACTION_ID_FINISH)
                .hasNext(true)
                .enabled(false)
                .title(getString(R.string.stashapp_actions_save))
                .build(),
        )

        if (viewModel.getValue(filterOption) != null) {
            actions.add(
                GuidedAction.Builder(requireContext())
                    .id(ACTION_ID_REMOVE)
                    .hasNext(true)
                    .title(getString(R.string.stashapp_actions_remove))
                    .build(),
            )
        }

        actions.add(
            GuidedAction.Builder(requireContext())
                .id(GuidedAction.ACTION_ID_CANCEL)
                .hasNext(true)
                .title(getString(R.string.stashapp_actions_cancel))
                .build(),
        )
    }

    override fun onGuidedActionEditedAndProceed(action: GuidedAction): Long {
        if (action.id == VALUE) {
            val desc = action.description
            if (desc != null) {
                val newValue = desc.toString().toDoubleOrNull()
                if (newValue != null) {
                    val rating100 =
                        if (viewModel.server.value!!.serverPreferences.ratingsAsStars) {
                            (newValue.times(20)).toInt()
                        } else {
                            (newValue.times(10)).toInt()
                        }
                    if (rating100 in 0..100) {
                        enableFinish(true)
                        return GuidedAction.ACTION_ID_NEXT
                    } else {
                        Toast.makeText(requireContext(), "Invalid rating!", Toast.LENGTH_SHORT)
                            .show()
                    }
                }
            }
            enableFinish(false)
            return GuidedAction.ACTION_ID_CURRENT
        }
        return GuidedAction.ACTION_ID_CURRENT
    }

    override fun onSubGuidedActionClicked(action: GuidedAction): Boolean {
        if (action.id >= MODIFIER_OFFSET) {
            val newModifier = CriterionModifier.entries[(action.id - MODIFIER_OFFSET).toInt()]
            curVal = curVal?.copy(modifier = newModifier) ?: IntCriterionInput(
                value = curVal?.value ?: 0,
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
            val newInt = findActionById(VALUE).description?.toString()?.toDouble()
            val modifier = curVal?.modifier ?: CriterionModifier.EQUALS

            val rating100 =
                if (viewModel.server.value!!.serverPreferences.ratingsAsStars) {
                    (newInt?.times(20))?.toInt()
                } else {
                    (newInt?.times(10))?.toInt()
                }
            val newValue =
                if (rating100 != null) {
                    IntCriterionInput(value = rating100, modifier = modifier)
                } else if (modifier == CriterionModifier.IS_NULL || modifier == CriterionModifier.NOT_NULL) {
                    IntCriterionInput(value = 0, modifier = modifier)
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
        private const val TAG = "RatingPickerFragment"
        private const val VALUE = 1L
        private const val MODIFIER = 2L
    }
}
