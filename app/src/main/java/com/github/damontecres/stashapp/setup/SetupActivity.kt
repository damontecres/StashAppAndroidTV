package com.github.damontecres.stashapp.setup

import android.content.Intent
import android.os.Bundle
import androidx.fragment.app.FragmentActivity
import androidx.leanback.app.GuidedStepSupportFragment
import com.github.damontecres.stashapp.PinActivity
import com.github.damontecres.stashapp.R
import com.github.damontecres.stashapp.util.StashClient
import com.github.damontecres.stashapp.util.StashServer
import com.github.damontecres.stashapp.util.TestResult
import com.github.damontecres.stashapp.util.testStashConnection

class SetupActivity : FragmentActivity(R.layout.frame_layout) {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        if (savedInstanceState == null) {
            GuidedStepSupportFragment.addAsRoot(
                this,
                SetupStep0(),
                R.id.frame_fragment,
            )
        }
    }

    override fun onPause() {
        super.onPause()
        if (isFinishing && StashServer.getCurrentStashServer(this) != null) {
            val intent = Intent(this, PinActivity::class.java)
            startActivity(intent)
        }
    }

    companion object {
        const val ACTION_SERVER_URL = 98L
        const val ACTION_SERVER_API_KEY = 99L
    }

    open class SimpleGuidedStepSupportFragment : GuidedStepSupportFragment() {
        fun nextStep(step: GuidedStepSupportFragment) {
            add(requireActivity().supportFragmentManager, step, R.id.frame_fragment)
        }

        fun finishSetup(setupState: SetupState) {
            StashServer.addAndSwitchServer(requireContext(), setupState.stashServer) {
                if (setupState.pinCode != null) {
                    it.putString(
                        getString(R.string.pref_key_pin_code),
                        setupState.pinCode.toString(),
                    )
                    it.putBoolean(getString(R.string.pref_key_pin_code_auto), true)
                }
                it.putBoolean(getString(R.string.pref_key_trust_certs), setupState.trustCerts)
            }
            finishGuidedStepSupportFragments()
        }

        override fun onProvideTheme(): Int {
            return R.style.Theme_StashAppAndroidTV_GuidedStep
        }

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
}
