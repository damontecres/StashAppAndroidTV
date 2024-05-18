package com.github.damontecres.stashapp.setup

import android.os.Bundle
import androidx.core.content.ContextCompat
import androidx.leanback.widget.GuidanceStylist
import androidx.leanback.widget.GuidedAction
import com.github.damontecres.stashapp.R

class SetupStep0 : SetupActivity.SimpleGuidedStepSupportFragment() {
    override fun onCreateGuidance(savedInstanceState: Bundle?): GuidanceStylist.Guidance {
        return GuidanceStylist.Guidance(
            "Stash for Android TV",
            "First time setup",
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
                .title(androidx.leanback.R.string.lb_guidedaction_continue_title)
                .build(),
        )
    }

    override fun onGuidedActionClicked(action: GuidedAction) {
        nextStep(SetupStep1ServerUrl())
    }
}
