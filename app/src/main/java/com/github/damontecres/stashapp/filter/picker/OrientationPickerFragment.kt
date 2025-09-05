package com.github.damontecres.stashapp.filter.picker

import android.os.Bundle
import androidx.core.content.ContextCompat
import androidx.leanback.widget.GuidanceStylist
import androidx.leanback.widget.GuidedAction
import com.github.damontecres.stashapp.R
import com.github.damontecres.stashapp.api.type.OrientationCriterionInput
import com.github.damontecres.stashapp.api.type.OrientationEnum
import com.github.damontecres.stashapp.api.type.StashDataFilter
import com.github.damontecres.stashapp.filter.CreateFilterGuidedStepFragment
import com.github.damontecres.stashapp.filter.FilterOption
import com.github.damontecres.stashapp.filter.displayName

class OrientationPickerFragment(
    val filterOption: FilterOption<StashDataFilter, OrientationCriterionInput>,
) : CreateFilterGuidedStepFragment() {
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
        val curVal =
            filterOption.getter
                .invoke(viewModel.objectFilter.value!!)
                .getOrNull()
                ?.value
                .orEmpty()

        OrientationEnum.entries
            .mapIndexedNotNull { index, orientation ->
                if (orientation != OrientationEnum.UNKNOWN__) {
                    val name = displayName(orientation)
                    val action =
                        GuidedAction
                            .Builder(requireContext())
                            .id(ORIENTATION_OFFSET + index)
                            .hasNext(false)
                            .title(name)
                            .checked(curVal.contains(orientation))
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

    override fun onSubGuidedActionClicked(action: GuidedAction): Boolean = true

    override fun onGuidedActionClicked(action: GuidedAction) {
        val values =
            actions
                .filter { it.id >= ORIENTATION_OFFSET && it.isChecked }
                .map { OrientationEnum.entries[(it.id - ORIENTATION_OFFSET).toInt()] }
        if (action.id == GuidedAction.ACTION_ID_FINISH) {
            val newFilter = OrientationCriterionInput(value = values)
            viewModel.updateFilter(filterOption, newFilter)
            parentFragmentManager.popBackStack()
        } else if (action.id >= ORIENTATION_OFFSET) {
            enableFinish(values.isNotEmpty())
        } else {
            onStandardActionClicked(action, filterOption)
        }
    }

    companion object {
        private const val TAG = "OrientationPickerFragment"

        private const val ORIENTATION_OFFSET = 1_000_000L
    }
}
