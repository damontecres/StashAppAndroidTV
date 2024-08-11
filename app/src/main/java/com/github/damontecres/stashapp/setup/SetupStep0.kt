package com.github.damontecres.stashapp.setup

import android.os.Bundle
import androidx.core.content.ContextCompat
import androidx.leanback.widget.GuidanceStylist
import androidx.leanback.widget.GuidedAction
import com.github.damontecres.stashapp.R

class SetupStep0 : SetupActivity.SimpleGuidedStepSupportFragment() {
    override fun onCreateGuidance(savedInstanceState: Bundle?): GuidanceStylist.Guidance {
        return GuidanceStylist.Guidance(
            getString(R.string.app_name_long),
            getString(R.string.first_time_setup),
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
                .id(GuidedAction.ACTION_ID_CONTINUE)
                .hasNext(true)
                .title(R.string.stashapp_actions_continue)
                .build(),
        )
    }

    override fun onGuidedActionClicked(action: GuidedAction) {
        nextStep(SetupStep1ServerUrl())
    }
}
