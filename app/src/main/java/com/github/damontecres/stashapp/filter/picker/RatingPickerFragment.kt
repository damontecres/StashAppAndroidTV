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
import kotlin.properties.Delegates

class RatingPickerFragment(
    private val filterOption: FilterOption<StashDataFilter, IntCriterionInput>,
) : CreateFilterGuidedStepFragment() {
    private var curVal: IntCriterionInput? = null
    private var ratingsAsStars by Delegates.notNull<Boolean>()

    override fun onCreate(savedInstanceState: Bundle?) {
        curVal = filterOption.getter(viewModel.objectFilter.value!!).getOrNull()
        ratingsAsStars = viewModel.server.value!!.serverPreferences.ratingsAsStars
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
            if (ratingsAsStars) {
                curVal?.value?.div(20.0)
            } else {
                curVal?.value?.div(10.0)
            }
        val current2 =
            if (ratingsAsStars) {
                curVal?.value2?.getOrNull()?.div(20.0)
            } else {
                curVal?.value2?.getOrNull()?.div(10.0)
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
                .id(VALUE_2)
                .hasNext(true)
                .title(getString(R.string.stashapp_criterion_value))
                .descriptionEditable(true)
                .descriptionEditInputType(InputType.TYPE_CLASS_NUMBER or InputType.TYPE_NUMBER_FLAG_DECIMAL)
                .description(current2?.toString())
                .enabled(curModifier == CriterionModifier.BETWEEN || curModifier == CriterionModifier.NOT_BETWEEN)
                .build(),
        )

        addStandardActions(actions, filterOption)
    }

    private fun calcRating100(value: Double?): Int? {
        if (value == null) {
            return null
        }
        return if (ratingsAsStars) {
            (value.times(20)).toInt()
        } else {
            (value.times(10)).toInt()
        }
    }

    private fun validateValue(desc: CharSequence?): Boolean {
        if (desc != null) {
            val newValue = desc.toString().toDoubleOrNull()
            if (newValue != null) {
                val rating100 = calcRating100(newValue)
                if (rating100 in 0..100) {
                    return true
                } else {
                    Toast.makeText(requireContext(), "Invalid rating!", Toast.LENGTH_SHORT)
                        .show()
                }
            }
        }
        return false
    }

    override fun onGuidedActionEditedAndProceed(action: GuidedAction): Long {
        if (action.id == VALUE || action.id == VALUE_2) {
            val desc = action.description
            val valid = validateValue(desc)

            val curModifier = curVal?.modifier ?: CriterionModifier.EQUALS
            if (curModifier == CriterionModifier.BETWEEN || curModifier == CriterionModifier.NOT_BETWEEN) {
                val otherDesc =
                    if (action.id == VALUE) {
                        findActionById(VALUE_2).description
                    } else {
                        findActionById(VALUE).description
                    }
                val otherValid = validateValue(otherDesc)
                if (valid && otherValid) {
                    enableFinish(true)
                } else {
                    enableFinish(false)
                }
            } else {
                enableFinish(valid)
            }
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
            val value = findActionById(VALUE).description?.toString()?.toDoubleOrNull()
            val value2 = findActionById(VALUE_2).description?.toString()?.toDoubleOrNull()
            val modifier = curVal?.modifier ?: CriterionModifier.EQUALS

            val rating100 = calcRating100(value)
            val rating1002 = calcRating100(value2)
            val newValue =
                if (rating100 != null) {
                    IntCriterionInput(
                        value = rating100,
                        value2 = Optional.presentIfNotNull(rating1002),
                        modifier = modifier,
                    )
                } else if (modifier == CriterionModifier.IS_NULL || modifier == CriterionModifier.NOT_NULL) {
                    IntCriterionInput(value = 0, value2 = Optional.absent(), modifier = modifier)
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
        private const val VALUE_2 = 2L
        private const val MODIFIER = 3L
    }
}
