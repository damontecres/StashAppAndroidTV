package com.github.damontecres.stashapp.setup

import android.os.Bundle
import android.widget.Toast
import androidx.core.content.ContextCompat
import androidx.fragment.app.FragmentActivity
import androidx.leanback.app.GuidedStepSupportFragment
import androidx.leanback.widget.GuidanceStylist
import androidx.leanback.widget.GuidedAction
import androidx.leanback.widget.GuidedActionsStylist
import androidx.lifecycle.lifecycleScope
import com.github.damontecres.stashapp.R
import com.github.damontecres.stashapp.util.StashCoroutineExceptionHandler
import com.github.damontecres.stashapp.util.StashServer
import com.github.damontecres.stashapp.util.TestResultStatus
import com.github.damontecres.stashapp.util.addAndSwitchServer
import com.github.damontecres.stashapp.util.isNotNullOrBlank
import com.github.damontecres.stashapp.util.testStashConnection
import kotlinx.coroutines.launch
import kotlin.properties.Delegates

class SetupActivity : FragmentActivity(R.layout.frame_layout) {
    private var firstTimeSetup by Delegates.notNull<Boolean>()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        firstTimeSetup = intent.getBooleanExtra(INTENT_SETUP_FIRST_TIME, false)

        GuidedStepSupportFragment.addAsRoot(this, ConfigureServerStep(firstTimeSetup), R.id.frame_fragment)
    }

    companion object {
        val INTENT_SETUP_FIRST_TIME = SetupActivity::class.java.name + ".firstTimeSetup"

        const val ACTION_SERVER_URL = 1L
        const val ACTION_SERVER_API_KEY = 2L
    }

    class ConfigureServerStep(private val firstTimeSetup: Boolean) : GuidedStepSupportFragment() {
        private var serverUrl: CharSequence? = null
        private var serverApiKey: CharSequence? = null

        private lateinit var guidedActionsServerUrl: GuidedAction
        private lateinit var guidedActionsServerApiKey: GuidedAction
        private lateinit var guidedActionSubmit: GuidedAction

        override fun onCreate(savedInstanceState: Bundle?) {
            if (savedInstanceState == null) {
                guidedActionsServerUrl =
                    GuidedAction.Builder(requireContext())
                        .id(ACTION_SERVER_URL)
                        .title("Stash Server URL")
//                    .description("The stash's server URL")
//                    .editDescription("")
                        .descriptionEditable(true)
                        .build()

                guidedActionsServerApiKey =
                    GuidedAction.Builder(requireContext())
                        .id(ACTION_SERVER_API_KEY)
                        .title("Stash Server API Key")
                        .description("API key not set")
                        .editDescription("")
                        .descriptionEditable(true)
                        .enabled(false)
                        .build()

                guidedActionSubmit =
                    GuidedAction.Builder(requireContext())
                        .id(GuidedAction.ACTION_ID_OK)
                        .title("Submit")
                        .description("")
                        .enabled(false)
                        .hasNext(true)
                        .build()
            }
            // Call super.onCreate last because it calls other setup steps
            super.onCreate(savedInstanceState)
        }

        override fun onCreateActionsStylist(): GuidedActionsStylist {
            return StashGuidedActionsStylist(requireContext())
        }

        override fun onCreateButtonActionsStylist(): GuidedActionsStylist {
            return super.onCreateButtonActionsStylist()
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
            if (action.id == ACTION_SERVER_URL) {
                serverUrl = action.description
            } else if (action.id == ACTION_SERVER_API_KEY) {
                serverApiKey = action.editDescription
                if (serverApiKey.isNotNullOrBlank()) {
                    action.description = "API key set"
                } else {
                    action.description = "API key not set"
                }
            }
            viewLifecycleOwner.lifecycleScope.launch(StashCoroutineExceptionHandler()) {
                val result =
                    testStashConnection(
                        requireContext(),
                        true,
                        serverUrl?.toString(),
                        serverApiKey?.toString(),
                    )
                when (result.status) {
                    TestResultStatus.SUCCESS -> {
                        guidedActionSubmit.isEnabled = true
                        val index = findActionPositionById(guidedActionSubmit.id)
                        notifyActionChanged(index)
                    }

                    TestResultStatus.AUTH_REQUIRED -> {
                        guidedActionsServerApiKey.isEnabled = true
                        val index = findActionPositionById(guidedActionsServerApiKey.id)
                        notifyActionChanged(index)
                    }
                    TestResultStatus.ERROR -> {}
                }
            }

            return GuidedAction.ACTION_ID_CURRENT
        }

        override fun onGuidedActionClicked(action: GuidedAction) {
            if (action.id == GuidedAction.ACTION_ID_OK) {
                if (firstTimeSetup) {
                    // TODO ask if user wants a PIN
                } else {
                    viewLifecycleOwner.lifecycleScope.launch(StashCoroutineExceptionHandler()) {
                        val result = testStashConnection(requireContext(), false, serverUrl?.toString(), serverApiKey?.toString())
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
    }
}
