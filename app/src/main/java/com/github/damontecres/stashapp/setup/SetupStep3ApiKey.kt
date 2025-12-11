package com.github.damontecres.stashapp.setup

import android.os.Bundle
import android.text.InputType
import android.util.Log
import androidx.core.content.ContextCompat
import androidx.leanback.widget.GuidanceStylist
import androidx.leanback.widget.GuidedAction
import androidx.leanback.widget.GuidedActionEditText
import androidx.lifecycle.lifecycleScope
import com.github.damontecres.stashapp.R
import com.github.damontecres.stashapp.util.StashCoroutineExceptionHandler
import com.github.damontecres.stashapp.util.TestResult
import com.github.damontecres.stashapp.util.isNotNullOrBlank
import kotlinx.coroutines.launch

class SetupStep3ApiKey(
    private val setupState: SetupState,
) : SetupGuidedStepSupportFragment() {
    private var apiKey: String? = ""

    override fun onCreateGuidance(savedInstanceState: Bundle?): GuidanceStylist.Guidance =
        GuidanceStylist.Guidance(
            "API Key",
            "Set the API key",
            null,
            ContextCompat.getDrawable(requireContext(), R.drawable.stash_logo),
        )

    override fun onCreateActions(
        actions: MutableList<GuidedAction>,
        savedInstanceState: Bundle?,
    ) {
        actions.add(
            createApiKeyAction(),
        )
        actions.add(
            GuidedAction
                .Builder(requireContext())
                .id(SetupFragment.ACTION_PASSWORD_VISIBLE)
                .title("API Key visible")
                .checked(false)
                .checkSetId(GuidedAction.CHECKBOX_CHECK_SET_ID)
                .build(),
        )
        actions.add(
            GuidedAction
                .Builder(requireContext())
                .id(GuidedAction.ACTION_ID_OK)
                .title(R.string.stashapp_actions_submit)
                .hasNext(true)
                .enabled(true)
                .build(),
        )
    }

    override fun onGuidedActionEditedAndProceed(action: GuidedAction): Long {
        if (action.id == SetupFragment.ACTION_SERVER_API_KEY) {
            updateApiKey(action)
            testApiKey()
            action.description =
                if (apiKey.isNotNullOrBlank()) {
                    "API key set"
                } else {
                    "API key not set"
                }
            notifyActionChanged(findActionPositionById(SetupFragment.ACTION_SERVER_API_KEY))
        }
        return GuidedAction.ACTION_ID_CURRENT
    }

    override fun onGuidedActionEditCanceled(action: GuidedAction) {
        if (action.id == SetupFragment.ACTION_SERVER_API_KEY) {
            updateApiKey(action)
            action.description =
                if (apiKey.isNotNullOrBlank()) {
                    "API key set"
                } else {
                    "API key not set"
                }
            notifyActionChanged(findActionPositionById(SetupFragment.ACTION_SERVER_API_KEY))
        }
    }

    override fun onGuidedActionClicked(action: GuidedAction) {
        if (action.id == SetupFragment.ACTION_PASSWORD_VISIBLE) {
            val newGuidedActionsServerApiKey = createApiKeyAction(action.isChecked)
            val newActions = listOf(newGuidedActionsServerApiKey, actions[1], actions[2])
            setActionsDiffCallback(null)
            actions = newActions
        } else if (action.id == GuidedAction.ACTION_ID_OK) {
            val apiAction = findActionById(SetupFragment.ACTION_SERVER_API_KEY)!!
            updateApiKey(apiAction)
            testApiKey()
        }
    }

    private fun updateApiKey(action: GuidedAction) {
        apiKey = action.editDescription?.toString()
        if (apiKey.isNullOrBlank()) {
            // Work around in weird cases where the editDescription isn't updated, but the text field has text
            // This attempts to find that text field and get its value
            try {
                val view = getActionItemView(findActionPositionById(action.id))!!
                val descView =
                    view.findViewById<GuidedActionEditText>(androidx.leanback.R.id.guidedactions_item_description)
                apiKey = descView.text?.toString()
            } catch (ex: Exception) {
                Log.w(TAG, "Exception getting view", ex)
            }
        }
    }

    private fun testApiKey() {
        if (apiKey.isNotNullOrBlank()) {
            viewLifecycleOwner.lifecycleScope.launch(StashCoroutineExceptionHandler()) {
                val result =
                    testConnection(
                        setupState.serverUrl,
                        apiKey.toString(),
                        setupState.trustCerts,
                    )
                when (result) {
                    is TestResult.Error,
                    TestResult.SslRequired,
                    TestResult.AuthRequired,
                    -> {
                        // no-op
                    }

                    TestResult.SelfSignedCertRequired -> {
                        // This should not happen
                        nextStep(SetupStep2Ssl(setupState))
                    }

                    is TestResult.Success,

                    is TestResult.UnsupportedVersion,
                    -> {
                        nextStep(SetupStep4Pin(setupState.copy(apiKey = apiKey.toString())))
                    }
                }
            }
        }
    }

    private fun createApiKeyAction(isChecked: Boolean = false): GuidedAction {
        val passwordInputType =
            if (isChecked) {
                InputType.TYPE_TEXT_VARIATION_VISIBLE_PASSWORD
            } else {
                InputType.TYPE_TEXT_VARIATION_PASSWORD
            }

        return GuidedAction
            .Builder(requireContext())
            .id(SetupFragment.ACTION_SERVER_API_KEY)
            .title("Stash Server API Key")
            .description(
                if (apiKey.isNotNullOrBlank()) {
                    "API key set"
                } else {
                    "API key not set"
                },
            ).editDescription(apiKey)
            .descriptionEditable(true)
            .descriptionEditInputType(
                InputType.TYPE_CLASS_TEXT or
//                    InputType.TYPE_TEXT_FLAG_MULTI_LINE or
                    passwordInputType,
            )
//            .multilineDescription(true)
            .enabled(true)
            .focusable(true)
            .hasNext(true)
            .build()
    }

    companion object {
        private const val TAG = "SetupStep3ApiKey"
    }
}
