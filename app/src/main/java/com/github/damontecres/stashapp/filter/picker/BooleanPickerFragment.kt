package com.github.damontecres.stashapp.filter.picker

import android.os.Bundle
import androidx.core.content.ContextCompat
import androidx.leanback.widget.GuidanceStylist
import androidx.leanback.widget.GuidedAction
import com.github.damontecres.stashapp.R
import com.github.damontecres.stashapp.api.type.StashDataFilter
import com.github.damontecres.stashapp.filter.CreateFilterGuidedStepFragment
import com.github.damontecres.stashapp.filter.FilterOption

/**
 * Select a boolean for a filter or remove it altogether
 */
class BooleanPickerFragment(
    private val filterOption: FilterOption<StashDataFilter, Boolean>,
) : CreateFilterGuidedStepFragment() {
    override fun onCreateGuidance(savedInstanceState: Bundle?): GuidanceStylist.Guidance =
        GuidanceStylist.Guidance(
            getString(filterOption.nameStringId),
            "",
            null,
            ContextCompat.getDrawable(requireContext(), R.drawable.stash_logo),
        )

    override fun onCreateActions(
        actions: MutableList<GuidedAction>,
        savedInstanceState: Bundle?,
    ) {
        actions.add(
            GuidedAction
                .Builder(requireContext())
                .id(0L)
                .hasNext(true)
                .title(getString(R.string.stashapp_true))
                .build(),
        )
        actions.add(
            GuidedAction
                .Builder(requireContext())
                .id(1L)
                .hasNext(true)
                .title(getString(R.string.stashapp_false))
                .build(),
        )
        if (viewModel.getValue(filterOption) != null) {
            actions.add(
                GuidedAction
                    .Builder(requireContext())
                    .id(2L)
                    .hasNext(true)
                    .title(getString(R.string.stashapp_actions_remove))
                    .build(),
            )
        }
        actions.add(
            GuidedAction
                .Builder(requireContext())
                .id(GuidedAction.ACTION_ID_CANCEL)
                .hasNext(true)
                .title(getString(R.string.stashapp_actions_cancel))
                .build(),
        )
    }

    override fun onGuidedActionClicked(action: GuidedAction) {
        if (action.id != GuidedAction.ACTION_ID_CANCEL) {
            val newValue =
                when (action.id) {
                    0L -> true
                    1L -> false
                    else -> null
                }
            viewModel.updateFilter(filterOption, newValue)
        }
        parentFragmentManager.popBackStack()
    }

    companion object {
        private const val TAG = "BooleanPickerFragment"
    }
}
