package com.github.damontecres.stashapp.filter.picker

import android.os.Bundle
import androidx.core.content.ContextCompat
import androidx.leanback.widget.GuidanceStylist
import androidx.leanback.widget.GuidedAction
import com.github.damontecres.stashapp.R
import com.github.damontecres.stashapp.api.type.SceneFilterType
import com.github.damontecres.stashapp.filter.CreateFilterActivity
import com.github.damontecres.stashapp.filter.FilterOption

class BooleanPickerFragment(
    val filterOption: FilterOption<SceneFilterType, Boolean>,
) : CreateFilterActivity.CreateFilterGuidedStepFragment() {
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
        actions.add(
            GuidedAction.Builder(requireContext())
                .id(1L)
                .hasNext(true)
                .title(getString(R.string.stashapp_true))
                .build(),
        )
        actions.add(
            GuidedAction.Builder(requireContext())
                .id(0L)
                .hasNext(true)
                .title(getString(R.string.stashapp_false))
                .build(),
        )
    }

    override fun onGuidedActionClicked(action: GuidedAction) {
        val newValue = action.id == 1L
        viewModel.updateFilter(filterOption, newValue)
        parentFragmentManager.popBackStack()
    }

    companion object {
        private const val TAG = "BooleanPickerFragment"
    }
}
