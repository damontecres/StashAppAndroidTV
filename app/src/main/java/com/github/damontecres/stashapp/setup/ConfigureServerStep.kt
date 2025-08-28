package com.github.damontecres.stashapp.setup

import android.content.DialogInterface
import android.os.Bundle
import android.text.InputType
import android.widget.Toast
import androidx.core.content.ContextCompat
import androidx.core.content.edit
import androidx.leanback.widget.GuidanceStylist
import androidx.leanback.widget.GuidedAction
import androidx.lifecycle.lifecycleScope
import androidx.preference.PreferenceManager
import com.github.damontecres.stashapp.R
import com.github.damontecres.stashapp.util.StashCoroutineExceptionHandler
import com.github.damontecres.stashapp.util.StashServer
import com.github.damontecres.stashapp.util.TestResult
import com.github.damontecres.stashapp.util.isNotNullOrBlank
import com.github.damontecres.stashapp.views.dialog.ConfirmationDialogFragment
import kotlinx.coroutines.launch

class ConfigureServerStep : SetupGuidedStepSupportFragment() {
    override fun onProvideTheme(): Int = R.style.Theme_StashAppAndroidTV_GuidedStep

    override fun onCreateGuidance(savedInstanceState: Bundle?): GuidanceStylist.Guidance =
        GuidanceStylist.Guidance(
            getString(R.string.setup_1_title),
            getString(R.string.setup_1_description),
            null,
            ContextCompat.getDrawable(requireContext(), R.drawable.stash_logo_small),
        )

    override fun onCreateActions(
        actions: MutableList<GuidedAction>,
        savedInstanceState: Bundle?,
    ) {
        super.onCreateActions(actions, savedInstanceState)

        actions.add(
            GuidedAction
                .Builder(requireContext())
                .id(SetupFragment.ACTION_SERVER_URL)
                .title("Stash Server URL")
                .descriptionEditable(true)
                .build(),
        )
        actions.add(createApiKeyAction())
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
                .title(getString(R.string.stashapp_actions_submit))
                .description("")
                .enabled(true)
                .focusable(true)
                .hasNext(true)
                .build(),
        )
    }

    override fun onGuidedActionEditCanceled(action: GuidedAction) {
        if (action.id == SetupFragment.ACTION_SERVER_API_KEY) {
            val serverApiKey = action.editDescription
            if (serverApiKey.isNotNullOrBlank()) {
                action.description = "API key set"
            } else {
                action.description = "API key not set"
            }
            notifyActionChanged(findActionPositionById(action.id))
        }
    }

    override fun onGuidedActionEditedAndProceed(action: GuidedAction): Long {
        val guidedActionsServerApiKey = findActionById(SetupFragment.ACTION_SERVER_API_KEY)!!
        if (action.id == SetupFragment.ACTION_SERVER_URL) {
            guidedActionsServerApiKey.editDescription = ""
            guidedActionsServerApiKey.description = "API key not set"
            val index = findActionPositionById(guidedActionsServerApiKey.id)
            notifyActionChanged(index)
        } else if (action.id == SetupFragment.ACTION_SERVER_API_KEY) {
            val serverApiKey = action.editDescription
            if (serverApiKey.isNotNullOrBlank()) {
                action.description = "API key set"
            } else {
                action.description = "API key not set"
            }
            notifyActionChanged(findActionPositionById(action.id))
        }

        val serverUrl = findActionById(SetupFragment.ACTION_SERVER_URL)!!.description?.toString()
        val apiKey =
            findActionById(SetupFragment.ACTION_SERVER_API_KEY)!!.editDescription?.toString()

        if (serverUrl.isNotNullOrBlank()) {
            viewLifecycleOwner.lifecycleScope.launch(StashCoroutineExceptionHandler()) {
                val trustCerts =
                    PreferenceManager
                        .getDefaultSharedPreferences(requireContext())
                        .getBoolean(getString(R.string.pref_key_trust_certs), false)
                val result = testConnection(serverUrl, apiKey, trustCerts)
                if (result is TestResult.SelfSignedCertRequired && !trustCerts) {
                    promptSelfSigned(false)
                }
            }
        }

        return GuidedAction.ACTION_ID_CURRENT
    }

    override fun onGuidedActionClicked(action: GuidedAction) {
        if (action.id == GuidedAction.ACTION_ID_OK) {
            testAndSubmit()
        } else if (action.id == SetupFragment.ACTION_PASSWORD_VISIBLE) {
            val newGuidedActionsServerApiKey = createApiKeyAction(action.isChecked)
            val newActions =
                listOf(
                    actions[0],
                    newGuidedActionsServerApiKey,
                    actions[2],
                    actions[3],
                )
            super.setActionsDiffCallback(null)
            actions = newActions
        }
    }

    private fun testAndSubmit() {
        val serverUrl = findActionById(SetupFragment.ACTION_SERVER_URL)!!.description?.toString()
        val apiKey =
            findActionById(SetupFragment.ACTION_SERVER_API_KEY)!!.editDescription?.toString()
        if (serverUrl.isNotNullOrBlank()) {
            viewLifecycleOwner.lifecycleScope.launch(StashCoroutineExceptionHandler()) {
                val trustCerts =
                    PreferenceManager
                        .getDefaultSharedPreferences(requireContext())
                        .getBoolean(getString(R.string.pref_key_trust_certs), false)
                val result =
                    testConnection(serverUrl, apiKey, trustCerts)
                if (result is TestResult.Success) {
                    val server = StashServer(serverUrl, apiKey)
                    // Persist values
                    StashServer.addAndSwitchServer(requireContext(), server)
                    serverViewModel.switchServer(server)
                    finishGuidedStepSupportFragments()
                } else if (result is TestResult.SelfSignedCertRequired && !trustCerts) {
                    promptSelfSigned(true)
                } else {
                    Toast
                        .makeText(
                            requireContext(),
                            "Cannot connect to server: ${result.message}",
                            Toast.LENGTH_LONG,
                        ).show()
                }
            }
        } else {
            Toast.makeText(requireContext(), "Enter a server URL", Toast.LENGTH_SHORT).show()
        }
    }

    private fun promptSelfSigned(testAndSubmitAfter: Boolean) {
        ConfirmationDialogFragment(
            "Server may be using a self-signed certificate.\n\nDo you want to trust all SSL/TLS certificates, including self-signed?",
        ) { dialog, which ->
            if (which == DialogInterface.BUTTON_POSITIVE) {
                PreferenceManager.getDefaultSharedPreferences(requireContext()).edit {
                    putBoolean(getString(R.string.pref_key_trust_certs), true)
                }
                Toast
                    .makeText(
                        requireContext(),
                        "Please restart the app after adding the server!",
                        Toast.LENGTH_LONG,
                    ).show()
                if (testAndSubmitAfter) {
                    testAndSubmit()
                }
            }
        }.show(childFragmentManager, "cert")
    }

    private fun createApiKeyAction(isChecked: Boolean = false): GuidedAction {
        val passwordInputType =
            if (isChecked) {
                InputType.TYPE_TEXT_VARIATION_VISIBLE_PASSWORD
            } else {
                InputType.TYPE_TEXT_VARIATION_PASSWORD
            }
        val apiKey =
            findActionById(SetupFragment.ACTION_SERVER_API_KEY)?.editDescription?.toString()

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
            .hasNext(true)
            .enabled(true)
            .focusable(true)
            .build()
    }
}
