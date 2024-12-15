package com.github.damontecres.stashapp.setup

import android.os.Bundle
import android.util.Log
import androidx.core.content.ContextCompat
import androidx.leanback.widget.GuidanceStylist
import androidx.leanback.widget.GuidedAction
import androidx.lifecycle.lifecycleScope
import com.github.damontecres.stashapp.R
import com.github.damontecres.stashapp.util.StashCoroutineExceptionHandler
import com.github.damontecres.stashapp.util.TestResultStatus
import kotlinx.coroutines.launch

class SetupStep2Ssl(
    private val setupState: SetupState,
) : SetupActivity.SimpleGuidedStepSupportFragment() {
    override fun onCreateGuidance(savedInstanceState: Bundle?): GuidanceStylist.Guidance =
        GuidanceStylist.Guidance(
            "HTTPS Certificate",
            "Allow self-signed certificates?",
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
                when (result.status) {
                    TestResultStatus.AUTH_REQUIRED -> {
                        nextStep(SetupStep3ApiKey(newState))
                    }

                    TestResultStatus.SELF_SIGNED_REQUIRED -> {
                        Log.w(TAG, "Trusting certs, but still error")
                    }

                    TestResultStatus.SUCCESS, TestResultStatus.UNSUPPORTED_VERSION -> {
                        nextStep(SetupStep4Pin(newState))
                    }

                    TestResultStatus.ERROR, TestResultStatus.SSL_REQUIRED -> {
                        requireActivity().supportFragmentManager.popBackStack()
                    }
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
