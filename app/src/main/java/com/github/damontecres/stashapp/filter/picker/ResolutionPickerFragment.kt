package com.github.damontecres.stashapp.filter.picker

import android.os.Bundle
import androidx.core.content.ContextCompat
import androidx.leanback.widget.GuidanceStylist
import androidx.leanback.widget.GuidedAction
import com.github.damontecres.stashapp.R
import com.github.damontecres.stashapp.api.type.CriterionModifier
import com.github.damontecres.stashapp.api.type.ResolutionCriterionInput
import com.github.damontecres.stashapp.api.type.ResolutionEnum
import com.github.damontecres.stashapp.api.type.StashDataFilter
import com.github.damontecres.stashapp.filter.CreateFilterGuidedStepFragment
import com.github.damontecres.stashapp.filter.FilterOption
import com.github.damontecres.stashapp.filter.resolutionName
import com.github.damontecres.stashapp.views.getString

class ResolutionPickerFragment(val filterOption: FilterOption<StashDataFilter, ResolutionCriterionInput>) :
    CreateFilterGuidedStepFragment() {
    private var curVal = ResolutionCriterionInput(value = ResolutionEnum.FULL_HD, modifier = CriterionModifier.EQUALS)

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
            ).getOrNull() ?: ResolutionCriterionInput(value = ResolutionEnum.FULL_HD, modifier = CriterionModifier.EQUALS)

        val modifierOptions =
            buildList {
                add(modifierAction(CriterionModifier.EQUALS))
                add(modifierAction(CriterionModifier.NOT_EQUALS))
                add(modifierAction(CriterionModifier.GREATER_THAN))
                add(modifierAction(CriterionModifier.LESS_THAN))
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

        val options =
            ResolutionEnum.entries.mapIndexed { index, res ->
                GuidedAction.Builder(requireContext())
                    .id(RESOLUTION_OFFSET + index)
                    .hasNext(false)
                    .title(resolutionName(res))
                    .description(resolutionName(curVal.value))
                    .build()
            }

        actions.add(
            GuidedAction.Builder(requireContext())
                .id(RESOLUTION)
                .hasNext(true)
                .title(getString(R.string.stashapp_criterion_value))
                .subActions(options)
                .build(),
        )

        addStandardActions(actions, filterOption)
    }

    override fun onSubGuidedActionClicked(action: GuidedAction): Boolean {
        if (action.id >= MODIFIER_OFFSET) {
            val newModifier = CriterionModifier.entries[(action.id - MODIFIER_OFFSET).toInt()]
            curVal = curVal.copy(modifier = newModifier)
            findActionById(MODIFIER).description = newModifier.getString(requireContext())
            notifyActionChanged(findActionPositionById(MODIFIER))
        } else if (action.id >= RESOLUTION_OFFSET) {
            val newResolution = ResolutionEnum.entries[(action.id - RESOLUTION_OFFSET).toInt()]
            curVal = curVal.copy(value = newResolution)
            findActionById(RESOLUTION).description = resolutionName(newResolution)
            notifyActionChanged(findActionPositionById(RESOLUTION))
        }
        return true
    }

    override fun onGuidedActionClicked(action: GuidedAction) {
        if (action.id == GuidedAction.ACTION_ID_FINISH) {
            viewModel.updateFilter(filterOption, curVal)
            parentFragmentManager.popBackStack()
        } else {
            onStandardActionClicked(action, filterOption)
        }
    }

    companion object {
        private const val TAG = "ResolutionPickerFragment"

        private const val MODIFIER = 1L

        private const val RESOLUTION = 2L

        private const val RESOLUTION_OFFSET = 1_000_000L
    }
}
