package com.github.damontecres.stashapp.setup

import android.os.Bundle
import android.widget.Toast
import androidx.core.content.ContextCompat
import androidx.leanback.widget.GuidanceStylist
import androidx.leanback.widget.GuidedAction
import androidx.lifecycle.lifecycleScope
import androidx.preference.PreferenceManager
import com.github.damontecres.stashapp.R
import com.github.damontecres.stashapp.util.StashCoroutineExceptionHandler
import com.github.damontecres.stashapp.util.StashServer
import com.github.damontecres.stashapp.util.TestResultStatus
import com.github.damontecres.stashapp.util.addAndSwitchServer
import com.github.damontecres.stashapp.util.isNotNullOrBlank
import kotlinx.coroutines.launch

class ConfigureServerStep : SetupActivity.SimpleGuidedStepSupportFragment() {
    private var serverUrl: CharSequence? = null
    private var serverApiKey: CharSequence? = null

    private lateinit var guidedActionsServerUrl: GuidedAction
    private lateinit var guidedActionsServerApiKey: GuidedAction
    private lateinit var guidedActionSubmit: GuidedAction

    override fun onCreate(savedInstanceState: Bundle?) {
        if (savedInstanceState == null) {
            guidedActionsServerUrl =
                GuidedAction.Builder(requireContext())
                    .id(SetupActivity.ACTION_SERVER_URL)
                    .title("Stash Server URL")
                    .descriptionEditable(true)
                    .build()

            guidedActionsServerApiKey =
                GuidedAction.Builder(requireContext())
                    .id(SetupActivity.ACTION_SERVER_API_KEY)
                    .title("Stash Server API Key")
                    .description("API key not set")
                    .editDescription("")
                    .descriptionEditable(true)
                    .enabled(false)
                    .focusable(false)
                    .build()

            guidedActionSubmit =
                GuidedAction.Builder(requireContext())
                    .id(GuidedAction.ACTION_ID_OK)
                    .title("Submit")
                    .description("")
                    .enabled(false)
                    .focusable(false)
                    .hasNext(true)
                    .build()
        }
        // Call super.onCreate last because it calls other setup steps
        super.onCreate(savedInstanceState)
    }

    override fun onProvideTheme(): Int {
        return R.style.Theme_StashAppAndroidTV_GuidedStep
    }

    override fun onCreateGuidance(savedInstanceState: Bundle?): GuidanceStylist.Guidance {
        return GuidanceStylist.Guidance(
            getString(R.string.setup_1_title),
            getString(R.string.setup_1_description),
            null,
            ContextCompat.getDrawable(requireContext(), R.mipmap.stash_logo),
        )
    }

    override fun onCreateActions(
        actions: MutableList<GuidedAction>,
        savedInstanceState: Bundle?,
    ) {
        super.onCreateActions(actions, savedInstanceState)

        actions.add(guidedActionsServerUrl)
        actions.add(guidedActionsServerApiKey)
        actions.add(guidedActionSubmit)
    }

    override fun onGuidedActionEditedAndProceed(action: GuidedAction): Long {
        if (action.id == SetupActivity.ACTION_SERVER_URL) {
            serverUrl = action.description
            guidedActionsServerApiKey.editDescription = ""
            guidedActionsServerApiKey.description = "API key not set"
            val index = findActionPositionById(guidedActionsServerApiKey.id)
            notifyActionChanged(index)
        } else if (action.id == SetupActivity.ACTION_SERVER_API_KEY) {
            serverApiKey = action.editDescription
            if (serverApiKey.isNotNullOrBlank()) {
                action.description = "API key set"
            } else {
                action.description = "API key not set"
            }
        }
        if (serverUrl != null) {
            val testServerUrl = serverUrl!!
            viewLifecycleOwner.lifecycleScope.launch(StashCoroutineExceptionHandler()) {
                val trustCerts =
                    PreferenceManager.getDefaultSharedPreferences(requireContext())
                        .getBoolean(getString(R.string.pref_key_trust_certs), false)
                val result =
                    testConnection(testServerUrl.toString(), serverApiKey?.toString(), trustCerts)
                when (result.status) {
                    TestResultStatus.SUCCESS -> {
                        guidedActionSubmit.isEnabled = true
                        guidedActionSubmit.isFocusable = true
                        val index = findActionPositionById(guidedActionSubmit.id)
                        notifyActionChanged(index)
                    }

                    TestResultStatus.AUTH_REQUIRED -> {
                        guidedActionsServerApiKey.isEnabled = true
                        guidedActionsServerApiKey.isFocusable = true
                        val index = findActionPositionById(guidedActionsServerApiKey.id)
                        notifyActionChanged(index)
                    }

                    TestResultStatus.ERROR, TestResultStatus.SSL_REQUIRED -> {
                        guidedActionSubmit.isEnabled = false
                        guidedActionSubmit.isFocusable = false
                        val submitIndex = findActionPositionById(guidedActionSubmit.id)
                        notifyActionChanged(submitIndex)
                    }

                    TestResultStatus.SELF_SIGNED_REQUIRED -> {
                        // no-op
                    }
                }
            }
        }

        return GuidedAction.ACTION_ID_CURRENT
    }

    override fun onGuidedActionClicked(action: GuidedAction) {
        if (action.id == GuidedAction.ACTION_ID_OK && serverUrl != null) {
            viewLifecycleOwner.lifecycleScope.launch(StashCoroutineExceptionHandler()) {
                val trustCerts =
                    PreferenceManager.getDefaultSharedPreferences(requireContext())
                        .getBoolean(getString(R.string.pref_key_trust_certs), false)
                val result =
                    testConnection(serverUrl!!.toString(), serverApiKey?.toString(), trustCerts)
                if (result.status == TestResultStatus.SUCCESS) {
                    // Persist values
                    addAndSwitchServer(
                        requireContext(),
                        StashServer(serverUrl.toString(), serverApiKey?.toString()),
                    )
                    finishGuidedStepSupportFragments()
                } else {
                    Toast.makeText(
                        requireContext(),
                        "Cannot connect to server.",
                        Toast.LENGTH_LONG,
                    ).show()
                }
            }
        }
    }
}
