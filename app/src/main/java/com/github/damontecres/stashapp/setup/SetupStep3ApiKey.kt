package com.github.damontecres.stashapp.setup

import android.os.Bundle
import android.text.InputType
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
    private var apiKey: String? = null

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
            createApiKeyAction(),
        )
        actions.add(
            GuidedAction.Builder(requireContext())
                .id(SetupActivity.ACTION_PASSWORD_VISIBLE)
                .title("API Key visible")
                .checked(false)
                .checkSetId(GuidedAction.CHECKBOX_CHECK_SET_ID)
                .build(),
        )
    }

    override fun onGuidedActionEditedAndProceed(action: GuidedAction): Long {
        if (action.id == SetupActivity.ACTION_SERVER_API_KEY) {
            apiKey = action.editDescription?.toString()
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

                        TestResultStatus.SUCCESS, TestResultStatus.UNSUPPORTED_VERSION -> {
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

    override fun onGuidedActionClicked(action: GuidedAction) {
        if (action.id == SetupActivity.ACTION_PASSWORD_VISIBLE) {
            val newGuidedActionsServerApiKey = createApiKeyAction(action.isChecked)
            val newActions = listOf(newGuidedActionsServerApiKey, actions[1])
            setActionsDiffCallback(null)
            actions = newActions
        }
    }

    private fun createApiKeyAction(isChecked: Boolean = false): GuidedAction {
        val passwordInputType =
            if (isChecked) {
                InputType.TYPE_TEXT_VARIATION_VISIBLE_PASSWORD
            } else {
                InputType.TYPE_TEXT_VARIATION_PASSWORD
            }

        return GuidedAction.Builder(requireContext())
            .id(SetupActivity.ACTION_SERVER_API_KEY)
            .title("Stash Server API Key")
            .description(
                if (apiKey.isNotNullOrBlank()) {
                    "API key set"
                } else {
                    "API key not set"
                },
            )
            .editDescription(apiKey ?: "")
            .descriptionEditable(true)
            .descriptionEditInputType(
                InputType.TYPE_CLASS_TEXT or
                    InputType.TYPE_TEXT_FLAG_MULTI_LINE or
                    passwordInputType,
            )
            .multilineDescription(true)
            .enabled(true)
            .focusable(true)
            .build()
    }
}
