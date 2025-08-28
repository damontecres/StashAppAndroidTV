package com.github.damontecres.stashapp.setup

import android.os.Bundle
import android.view.View
import androidx.activity.addCallback
import androidx.core.content.ContextCompat
import androidx.fragment.app.activityViewModels
import androidx.leanback.app.GuidedStepSupportFragment
import androidx.leanback.widget.GuidanceStylist
import androidx.leanback.widget.GuidedAction
import com.github.damontecres.stashapp.R
import com.github.damontecres.stashapp.navigation.Destination
import com.github.damontecres.stashapp.util.StashServer
import com.github.damontecres.stashapp.util.getDestination
import com.github.damontecres.stashapp.util.readOnlyModeDisabled
import com.github.damontecres.stashapp.views.models.ServerViewModel
import kotlin.properties.Delegates

class ManageServersFragment : GuidedStepSupportFragment() {
    private val viewModel: ServerViewModel by activityViewModels()

    private var overrideReadOnly by Delegates.notNull<Boolean>()

    private var currentServer: StashServer? = null
    private lateinit var allServers: List<StashServer>
    private lateinit var otherServers: List<StashServer>

    override fun onCreate(savedInstanceState: Bundle?) {
        overrideReadOnly =
            requireArguments().getDestination<Destination.ManageServers>().overrideReadOnly
        super.onCreate(savedInstanceState)
    }

    override fun onViewCreated(
        view: View,
        savedInstanceState: Bundle?,
    ) {
        super.onViewCreated(view, savedInstanceState)
        val backCallback =
            requireActivity().onBackPressedDispatcher.addCallback(
                viewLifecycleOwner,
                enabled = false,
            ) {
                requireActivity().finish()
            }
        viewModel.currentServer.observe(viewLifecycleOwner) {
            // If no server is defined, then exit the app on back
            backCallback.isEnabled = it == null
        }
    }

    override fun onProvideTheme(): Int = R.style.Theme_StashAppAndroidTV_GuidedStep

    override fun onCreateGuidance(savedInstanceState: Bundle?): GuidanceStylist.Guidance {
        val server = viewModel.currentServer.value
        val current =
            if (server != null) {
                "\n\nCurrent:\n${server.url}"
            } else {
                ""
            }
        return GuidanceStylist.Guidance(
            "Manage servers",
            "Add, remove, or change servers$current",
            null,
            ContextCompat.getDrawable(requireContext(), R.drawable.stash_logo_small),
        )
    }

    override fun onCreateActions(
        actions: MutableList<GuidedAction>,
        savedInstanceState: Bundle?,
    ) {
        super.onCreateActions(actions, savedInstanceState)

        allServers = StashServer.getAll(requireContext())
        currentServer = viewModel.currentServer.value
        otherServers =
            if (currentServer != null) {
                val temp = allServers.toMutableList()
                temp.remove(currentServer)
                temp
            } else {
                allServers
            }

        if (otherServers.isNotEmpty()) {
            val switchActions =
                otherServers.mapIndexed { i, server ->
                    GuidedAction
                        .Builder(requireContext())
                        .id(ACTION_SWITCH_OFFSET + i)
                        .title(server.url)
                        .build()
                }
            actions.add(
                GuidedAction
                    .Builder(requireContext())
                    .id(ACTION_SWITCH_OFFSET - 1)
                    .title("Switch to another server")
                    .subActions(switchActions)
                    .build(),
            )
        }

        if (overrideReadOnly || readOnlyModeDisabled()) {
            actions.add(
                GuidedAction
                    .Builder(requireContext())
                    .id(ACTION_ADD_SERVER)
                    .title("Add a new server")
                    .hasNext(true)
                    .build(),
            )
            if (otherServers.isNotEmpty()) {
                val removeActions =
                    otherServers.mapIndexed { i, server ->
                        GuidedAction
                            .Builder(requireContext())
                            .id(ACTION_REMOVE_OFFSET + i)
                            .title(server.url)
                            .build()
                    }
                actions.add(
                    GuidedAction
                        .Builder(requireContext())
                        .id(ACTION_REMOVE_OFFSET - 1)
                        .title("Remove a server")
                        .subActions(removeActions)
                        .build(),
                )
            }
        }
    }

    override fun onGuidedActionClicked(action: GuidedAction) {
        if (action.id == ACTION_ADD_SERVER) {
            add(
                requireActivity().supportFragmentManager,
                ConfigureServerStep(),
            )
        }
    }

    override fun onSubGuidedActionClicked(action: GuidedAction): Boolean {
        if (action.id in ACTION_SWITCH_OFFSET..<ACTION_REMOVE_OFFSET) {
            // Switching servers
            val index = action.id - ACTION_SWITCH_OFFSET
            val server = otherServers[index.toInt()]
            viewModel.switchServer(server)
//            viewModel.navigationManager.goToMain()
            finishGuidedStepSupportFragments()
        } else if (action.id >= ACTION_REMOVE_OFFSET) {
            // Remove a server
            val index = action.id - ACTION_REMOVE_OFFSET
            val server = otherServers[index.toInt()]
            StashServer.removeStashServer(requireContext(), server)
//            viewModel.navigationManager.goToMain()
            finishGuidedStepSupportFragments()
        }
        return true
    }

    companion object {
        const val ACTION_ADD_SERVER = 1L
        const val ACTION_SWITCH_OFFSET = 10L
        const val ACTION_REMOVE_OFFSET = 1_000_000L
    }
}
