package com.github.damontecres.stashapp.setup

import android.os.Bundle
import androidx.core.content.ContextCompat
import androidx.leanback.widget.GuidanceStylist
import androidx.leanback.widget.GuidedAction
import androidx.lifecycle.lifecycleScope
import com.github.damontecres.stashapp.R
import com.github.damontecres.stashapp.util.StashCoroutineExceptionHandler
import com.github.damontecres.stashapp.util.TestResultStatus
import com.github.damontecres.stashapp.util.isNotNullOrBlank
import com.github.damontecres.stashapp.util.testStashConnection
import kotlinx.coroutines.launch

class SetupStep1ServerUrl : SetupActivity.SimpleGuidedStepSupportFragment() {
    override fun onCreateGuidance(savedInstanceState: Bundle?): GuidanceStylist.Guidance {
        return GuidanceStylist.Guidance(
            "Stash Server URL",
            "Enter the URL for your Stash server, including the port if needed",
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
                .id(SetupActivity.ACTION_SERVER_URL)
                .title("Server URL")
                .descriptionEditable(true)
                .hasNext(true)
                .build(),
        )
    }

    override fun onGuidedActionEditedAndProceed(action: GuidedAction): Long {
        if (action.id == SetupActivity.ACTION_SERVER_URL) {
            val serverUrl = action.description
            testServerUrl(serverUrl)
        }
        return GuidedAction.ACTION_ID_CURRENT
    }

    private fun testServerUrl(serverUrl: CharSequence?) {
        if (serverUrl.isNotNullOrBlank()) {
            val state = SetupState(serverUrl)
            viewLifecycleOwner.lifecycleScope.launch(StashCoroutineExceptionHandler()) {
                val result =
                    testStashConnection(
                        requireContext(),
                        true,
                        serverUrl.toString(),
                        null,
                    )
                when (result.status) {
                    TestResultStatus.AUTH_REQUIRED -> {
                        nextStep(SetupStep3ApiKey(state))
                    }

                    TestResultStatus.SELF_SIGNED_REQUIRED -> {
                        nextStep(SetupStep2Ssl(state))
                    }

                    TestResultStatus.SUCCESS -> {
                        nextStep(SetupStep4Pin(state))
                    }

                    TestResultStatus.ERROR, TestResultStatus.SSL_REQUIRED -> {
                        // no-op
                    }
                }
            }
        }
    }
}
