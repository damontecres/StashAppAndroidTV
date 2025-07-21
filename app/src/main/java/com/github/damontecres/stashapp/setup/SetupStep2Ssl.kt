package com.github.damontecres.stashapp.setup

import android.os.Bundle
import android.util.Log
import androidx.core.content.ContextCompat
import androidx.leanback.widget.GuidanceStylist
import androidx.leanback.widget.GuidedAction
import androidx.lifecycle.lifecycleScope
import com.github.damontecres.stashapp.R
import com.github.damontecres.stashapp.util.StashCoroutineExceptionHandler
import com.github.damontecres.stashapp.util.TestResult
import kotlinx.coroutines.launch

class SetupStep2Ssl(
    private val setupState: SetupState,
) : SetupGuidedStepSupportFragment() {
    override fun onCreateGuidance(savedInstanceState: Bundle?): GuidanceStylist.Guidance =
        GuidanceStylist.Guidance(
            "HTTPS Certificate",
            "Allow self-signed certificates?\n\nNote: if enabled, the app must be restarted/force stopped after completing setup!",
            null,
            ContextCompat.getDrawable(requireContext(), R.mipmap.stash_logo),
        )

    override fun onCreateActions(
        actions: MutableList<GuidedAction>,
        savedInstanceState: Bundle?,
    ) {
        actions.add(
            GuidedAction
                .Builder(requireContext())
                .id(GuidedAction.ACTION_ID_YES)
                .title("Yes")
                .description("Allow self-signed certs")
                .build(),
        )
        actions.add(
            GuidedAction
                .Builder(requireContext())
                .id(GuidedAction.ACTION_ID_NO)
                .title("No")
                .description("Enter a different URL")
                .build(),
        )
    }

    override fun onGuidedActionClicked(action: GuidedAction) {
        if (action.id == GuidedAction.ACTION_ID_YES) {
            val newState = setupState.copy(trustCerts = true)
            viewLifecycleOwner.lifecycleScope.launch(StashCoroutineExceptionHandler()) {
                val result = testConnection(newState.serverUrl, null, true)
                when (result) {
                    TestResult.AuthRequired -> nextStep(SetupStep3ApiKey(newState))

                    is TestResult.Error,
                    TestResult.SslRequired,
                    -> requireActivity().supportFragmentManager.popBackStack()

                    TestResult.SelfSignedCertRequired -> {
                        Log.w(TAG, "Trusting certs, but still error")
                    }

                    is TestResult.Success,
                    is TestResult.UnsupportedVersion,
                    -> nextStep(SetupStep4Pin(newState))
                }
            }
        } else {
            requireActivity().supportFragmentManager.popBackStack()
        }
    }

    companion object {
        const val TAG = "SetupStep5Ssl"
    }
}
