package com.github.damontecres.stashapp.filter.picker

import android.os.Bundle
import androidx.core.content.ContextCompat
import androidx.leanback.widget.GuidanceStylist
import androidx.leanback.widget.GuidedAction
import com.apollographql.apollo.api.Optional
import com.github.damontecres.stashapp.R
import com.github.damontecres.stashapp.api.type.CircumcisionCriterionInput
import com.github.damontecres.stashapp.api.type.CircumisedEnum
import com.github.damontecres.stashapp.api.type.CriterionModifier
import com.github.damontecres.stashapp.api.type.StashDataFilter
import com.github.damontecres.stashapp.filter.CreateFilterGuidedStepFragment
import com.github.damontecres.stashapp.filter.FilterOption
import com.github.damontecres.stashapp.views.getString

class CircumcisionPickerFragment(
    val filterOption: FilterOption<StashDataFilter, CircumcisionCriterionInput>,
) : CreateFilterGuidedStepFragment() {
    private var currentModifier = CriterionModifier.INCLUDES

    override fun onCreateGuidance(savedInstanceState: Bundle?): GuidanceStylist.Guidance =
        GuidanceStylist.Guidance(
            getString(filterOption.nameStringId),
            null,
            null,
            ContextCompat.getDrawable(requireContext(), R.drawable.stash_logo),
        )

    override fun onCreateActions(
        actions: MutableList<GuidedAction>,
        savedInstanceState: Bundle?,
    ) {
        val curVal = viewModel.getValue(filterOption)
        val values = curVal?.value?.getOrNull().orEmpty()

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

        CircumisedEnum.entries
            .mapIndexedNotNull { index, circ ->
                if (circ != CircumisedEnum.UNKNOWN__) {
                    val name =
                        when (circ) {
                            CircumisedEnum.CUT -> getString(R.string.stashapp_circumcised_types_CUT)
                            CircumisedEnum.UNCUT -> getString(R.string.stashapp_circumcised_types_UNCUT)
                            CircumisedEnum.UNKNOWN__ -> "Unknown"
                        }
                    val action =
                        GuidedAction
                            .Builder(requireContext())
                            .id(CIRC_OFFSET + index)
                            .hasNext(false)
                            .title(name)
                            .checked(values.contains(circ))
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
            val newFilter =
                CircumcisionCriterionInput(
                    value = Optional.presentIfNotNull(values),
                    modifier = currentModifier,
                )
            viewModel.updateFilter(filterOption, newFilter)
            parentFragmentManager.popBackStack()
        } else if (action.id in CIRC_OFFSET..<MODIFIER_OFFSET) {
            setFinish()
        } else {
            onStandardActionClicked(action, filterOption)
        }
    }

    private fun getValues(): List<CircumisedEnum>? =
        actions
            .filter { it.id >= CIRC_OFFSET && it.isChecked }
            .map { CircumisedEnum.entries[(it.id - CIRC_OFFSET).toInt()] }
            .ifEmpty { null }

    private fun setFinish() {
        if (currentModifier.isNullModifier() || getValues() != null) {
            enableFinish(true)
        } else {
            enableFinish(false)
        }
    }

    companion object {
        private const val TAG = "HierarchicalMultiCriterionFragment"

        private const val MODIFIER = 1L

        private const val CIRC_OFFSET = 1_000_000L
    }
}
