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
import kotlinx.coroutines.launch

class SetupStep3ApiKey(private val setupState: SetupState) :
    SetupActivity.SimpleGuidedStepSupportFragment() {
    override fun onCreateGuidance(savedInstanceState: Bundle?): GuidanceStylist.Guidance {
        return GuidanceStylist.Guidance(
            "API Key",
            "Set the API key",
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
                .id(SetupActivity.ACTION_SERVER_API_KEY)
                .title("Server API Key")
                .description("API key not set")
                .editDescription("")
                .descriptionEditable(true)
                .hasNext(true)
                .build(),
        )
    }

    override fun onGuidedActionEditedAndProceed(action: GuidedAction): Long {
        if (action.id == SetupActivity.ACTION_SERVER_API_KEY) {
            val apiKey = action.editDescription
            if (apiKey.isNotNullOrBlank()) {
                action.description = "API key set"
            } else {
                action.description = "API key not set"
            }
            if (apiKey.isNotNullOrBlank()) {
                viewLifecycleOwner.lifecycleScope.launch(StashCoroutineExceptionHandler()) {
                    val result =
                        testConnection(
                            setupState.serverUrl,
                            apiKey.toString(),
                            setupState.trustCerts,
                        )
                    when (result.status) {
                        TestResultStatus.AUTH_REQUIRED -> {
                            // Wrong API key, no-op
                        }

                        TestResultStatus.SELF_SIGNED_REQUIRED -> {
                            // This should not happen
                            nextStep(SetupStep2Ssl(setupState))
                        }

                        TestResultStatus.SUCCESS -> {
                            nextStep(SetupStep4Pin(setupState.copy(apiKey = apiKey.toString())))
                        }

                        TestResultStatus.ERROR, TestResultStatus.SSL_REQUIRED -> {
                            // no-op
                        }
                    }
                }
            }
        }
        return GuidedAction.ACTION_ID_CURRENT
    }
}
