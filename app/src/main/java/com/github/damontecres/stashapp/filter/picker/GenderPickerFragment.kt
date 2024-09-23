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

class GenderPickerFragment(val filterOption: FilterOption<StashDataFilter, GenderCriterionInput>) : CreateFilterGuidedStepFragment() {
    private var curVal = GenderCriterionInput(modifier = CriterionModifier.INCLUDES)

    override fun onCreateGuidance(savedInstanceState: Bundle?): GuidanceStylist.Guidance {
        return GuidanceStylist.Guidance(
            getString(filterOption.nameStringId),
            null,
            null,
            ContextCompat.getDrawable(requireContext(), R.mipmap.stash_logo),
        )
    }

    override fun onCreateActions(
        actions: MutableList<GuidedAction>,
        savedInstanceState: Bundle?,
    ) {
        curVal =
            filterOption.getter.invoke(
                viewModel.objectFilter.value!!,
            ).getOrNull() ?: GenderCriterionInput(modifier = CriterionModifier.INCLUDES)

        val modifierOptions =
            buildList {
                add(modifierAction(CriterionModifier.INCLUDES))
                add(modifierAction(CriterionModifier.EXCLUDES))
                add(modifierAction(CriterionModifier.IS_NULL))
                add(modifierAction(CriterionModifier.NOT_NULL))
            }
        actions.add(
            GuidedAction.Builder(requireContext())
                .id(MODIFIER)
                .hasNext(false)
                .title("Modifier")
                .description(curVal.modifier.getString(requireContext()))
                .subActions(modifierOptions)
                .build(),
        )

        GenderEnum.entries
            .mapIndexedNotNull { index, gender ->
                if (gender != GenderEnum.UNKNOWN__) {
                    val name = displayName(requireContext(), gender)
                    val action =
                        GuidedAction.Builder(requireContext())
                            .id(GENDER_OFFSET + index)
                            .hasNext(false)
                            .title(name)
                            .checked(curVal.value_list.getOrNull()?.contains(gender) ?: false)
                            .checkSetId(GuidedAction.CHECKBOX_CHECK_SET_ID)
                            .build()
                    Pair(name, action)
                } else {
                    null
                }
            }
            .sortedBy { it.first }
            .forEach { actions.add(it.second) }

        addStandardActions(actions, filterOption)
    }

    override fun onSubGuidedActionClicked(action: GuidedAction): Boolean {
        if (action.id >= MODIFIER_OFFSET) {
            val newModifier = CriterionModifier.entries[(action.id - MODIFIER_OFFSET).toInt()]
            curVal = curVal.copy(modifier = newModifier)
            findActionById(MODIFIER).description = newModifier.getString(requireContext())
            notifyActionChanged(findActionPositionById(MODIFIER))
        }
        return true
    }

    override fun onGuidedActionClicked(action: GuidedAction) {
        if (action.id == GuidedAction.ACTION_ID_FINISH) {
            val values =
                actions.filter { it.id >= GENDER_OFFSET && it.isChecked }
                    .map { GenderEnum.entries[(it.id - GENDER_OFFSET).toInt()] }
            curVal = curVal.copy(value_list = Optional.present(values))
            viewModel.updateFilter(filterOption, curVal)
            parentFragmentManager.popBackStack()
        } else {
            onStandardActionClicked(action, filterOption)
        }
    }

    companion object {
        private const val TAG = "HierarchicalMultiCriterionFragment"

        private const val MODIFIER = 1L

        private const val GENDER_OFFSET = 1_000_000L
    }
}
