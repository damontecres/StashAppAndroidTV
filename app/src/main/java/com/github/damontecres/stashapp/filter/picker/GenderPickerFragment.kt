package com.github.damontecres.stashapp.filter.picker

import android.os.Bundle
import androidx.core.content.ContextCompat
import androidx.leanback.widget.GuidanceStylist
import androidx.leanback.widget.GuidedAction
import com.apollographql.apollo.api.Optional
import com.github.damontecres.stashapp.R
import com.github.damontecres.stashapp.api.type.CriterionModifier
import com.github.damontecres.stashapp.api.type.GenderCriterionInput
import com.github.damontecres.stashapp.api.type.GenderEnum
import com.github.damontecres.stashapp.api.type.StashDataFilter
import com.github.damontecres.stashapp.filter.CreateFilterGuidedStepFragment
import com.github.damontecres.stashapp.filter.FilterOption
import com.github.damontecres.stashapp.filter.displayName
import com.github.damontecres.stashapp.views.getString

class GenderPickerFragment(
    val filterOption: FilterOption<StashDataFilter, GenderCriterionInput>,
) : CreateFilterGuidedStepFragment() {
    private var currentModifier = CriterionModifier.INCLUDES

    override fun onCreateGuidance(savedInstanceState: Bundle?): GuidanceStylist.Guidance =
        GuidanceStylist.Guidance(
            getString(filterOption.nameStringId),
            null,
            null,
            ContextCompat.getDrawable(requireContext(), R.drawable.stash_logo_small),
        )

    override fun onCreateActions(
        actions: MutableList<GuidedAction>,
        savedInstanceState: Bundle?,
    ) {
        val curVal = viewModel.getValue(filterOption)
        val values =
            if (curVal?.value_list?.getOrNull() != null) {
                curVal.value_list.getOrNull()!!
            } else if (curVal?.value?.getOrNull() != null) {
                listOf(curVal.value.getOrNull()!!)
            } else {
                emptyList()
            }

        if (curVal?.modifier != null) {
            currentModifier = curVal.modifier
        }

        val modifierOptions =
            buildList {
                add(modifierAction(CriterionModifier.INCLUDES))
                add(modifierAction(CriterionModifier.EXCLUDES))
                add(modifierAction(CriterionModifier.IS_NULL))
                add(modifierAction(CriterionModifier.NOT_NULL))
            }
        actions.add(
            GuidedAction
                .Builder(requireContext())
                .id(MODIFIER)
                .hasNext(false)
                .title("Modifier")
                .description(currentModifier.getString(requireContext()))
                .subActions(modifierOptions)
                .build(),
        )

        GenderEnum.entries
            .mapIndexedNotNull { index, gender ->
                if (gender != GenderEnum.UNKNOWN__) {
                    val name = displayName(requireContext(), gender)
                    val action =
                        GuidedAction
                            .Builder(requireContext())
                            .id(GENDER_OFFSET + index)
                            .hasNext(false)
                            .title(name)
                            .checked(values.contains(gender))
                            .checkSetId(GuidedAction.CHECKBOX_CHECK_SET_ID)
                            .build()
                    Pair(name, action)
                } else {
                    null
                }
            }.sortedBy { it.first }
            .forEach { actions.add(it.second) }

        addStandardActions(actions, filterOption)
    }

    override fun onSubGuidedActionClicked(action: GuidedAction): Boolean {
        if (action.id >= MODIFIER_OFFSET) {
            currentModifier = CriterionModifier.entries[(action.id - MODIFIER_OFFSET).toInt()]
            findActionById(MODIFIER)!!.description = currentModifier.getString(requireContext())
            notifyActionChanged(findActionPositionById(MODIFIER))
            setFinish()
        }
        return true
    }

    override fun onGuidedActionClicked(action: GuidedAction) {
        if (action.id == GuidedAction.ACTION_ID_FINISH) {
            val values = getValues()
            val curVal =
                GenderCriterionInput(
                    value_list = Optional.present(values),
                    modifier = currentModifier,
                )
            viewModel.updateFilter(filterOption, curVal)
            parentFragmentManager.popBackStack()
        } else if (action.id in GENDER_OFFSET..<MODIFIER_OFFSET) {
            setFinish()
        } else {
            onStandardActionClicked(action, filterOption)
        }
    }

    private fun getValues(): List<GenderEnum>? =
        actions
            .filter { it.id >= GENDER_OFFSET && it.isChecked }
            .map { GenderEnum.entries[(it.id - GENDER_OFFSET).toInt()] }
            .ifEmpty { null }

    private fun setFinish() {
        if (currentModifier.isNullModifier() || getValues() != null) {
            enableFinish(true)
        } else {
            enableFinish(false)
        }
    }

    companion object {
        private const val TAG = "GenderPickerFragment"

        private const val MODIFIER = 1L

        private const val GENDER_OFFSET = 1_000_000L
    }
}
