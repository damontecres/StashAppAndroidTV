package com.github.damontecres.stashapp.setup

import androidx.fragment.app.activityViewModels
import androidx.leanback.app.GuidedStepSupportFragment
import com.github.damontecres.stashapp.util.StashClient
import com.github.damontecres.stashapp.util.StashServer
import com.github.damontecres.stashapp.util.TestResult
import com.github.damontecres.stashapp.util.testStashConnection
import com.github.damontecres.stashapp.views.models.ServerViewModel

open class SetupGuidedStepSupportFragment : GuidedStepSupportFragment() {
    protected val serverViewModel: ServerViewModel by activityViewModels()

    fun nextStep(step: GuidedStepSupportFragment) {
        add(requireActivity().supportFragmentManager, step, android.R.id.content)
    }

    fun finishSetup(setupState: SetupState) {
        StashServer.addAndSwitchServer(requireContext(), setupState.stashServer) {
            if (setupState.pinCode != null) {
                it.putString(
                    getString(com.github.damontecres.stashapp.R.string.pref_key_pin_code),
                    setupState.pinCode,
                )
                it.putBoolean(
                    getString(com.github.damontecres.stashapp.R.string.pref_key_pin_code_auto),
                    true,
                )
            }
            it.putBoolean(
                getString(com.github.damontecres.stashapp.R.string.pref_key_trust_certs),
                setupState.trustCerts,
            )
        }
        serverViewModel.switchServer(setupState.stashServer)
        serverViewModel.navigationManager.goBack()
        serverViewModel.navigationManager.goToMain()
    }

    override fun onProvideTheme(): Int = com.github.damontecres.stashapp.R.style.Theme_StashAppAndroidTV_GuidedStep

    protected suspend fun testConnection(
        serverUrl: String,
        apiKey: String?,
        trustCerts: Boolean,
    ): TestResult {
        val apolloClient =
            StashClient.createTestApolloClient(
                requireContext(),
                StashServer(serverUrl, apiKey),
                trustCerts,
            )
        return testStashConnection(
            requireContext(),
            true,
            apolloClient,
        )
    }
}
