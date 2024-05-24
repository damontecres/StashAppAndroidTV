package com.github.damontecres.stashapp.setup

import android.os.Bundle
import androidx.core.content.ContextCompat
import androidx.leanback.app.GuidedStepSupportFragment
import androidx.leanback.widget.GuidanceStylist
import androidx.leanback.widget.GuidedAction
import androidx.preference.PreferenceManager
import com.github.damontecres.stashapp.R
import com.github.damontecres.stashapp.SettingsFragment
import com.github.damontecres.stashapp.util.StashServer

class ManageServersFragment : GuidedStepSupportFragment() {
    private var currentServer: StashServer? = null
    private lateinit var allServers: List<StashServer>
    private lateinit var otherServers: List<StashServer>

    override fun onProvideTheme(): Int {
        return R.style.Theme_StashAppAndroidTV_GuidedStep
    }

    override fun onCreateGuidance(savedInstanceState: Bundle?): GuidanceStylist.Guidance {
        return GuidanceStylist.Guidance(
            "Manage servers",
            "Add, remove, or change servers",
            null,
            ContextCompat.getDrawable(requireContext(), R.mipmap.stash_logo),
        )
    }

    fun getStashServers(): List<StashServer> {
        val manager = PreferenceManager.getDefaultSharedPreferences(requireContext())
        val keys =
            manager.all.keys.filter { it.startsWith(SettingsFragment.PreferencesFragment.SERVER_PREF_PREFIX) }.sorted().toList()
        return keys.map {
            val url = it.replace(SettingsFragment.PreferencesFragment.SERVER_PREF_PREFIX, "")
            val apiKeyKey =
                it.replace(
                    SettingsFragment.PreferencesFragment.SERVER_PREF_PREFIX,
                    SettingsFragment.PreferencesFragment.SERVER_APIKEY_PREF_PREFIX,
                )
            val apiKey = manager.all[apiKeyKey]?.toString()?.replace(SettingsFragment.PreferencesFragment.SERVER_APIKEY_PREF_PREFIX, "")
            StashServer(url, apiKey)
        }.sortedBy { it.url }
    }

    override fun onCreateActions(
        actions: MutableList<GuidedAction>,
        savedInstanceState: Bundle?,
    ) {
        super.onCreateActions(actions, savedInstanceState)

        allServers = getStashServers()
        currentServer = StashServer.getCurrentStashServer(requireContext())
        otherServers =
            if (currentServer != null) {
                val temp = allServers.toMutableList()
                temp.remove(currentServer)
                temp
            } else {
                allServers
            }

        actions.add(
            GuidedAction.Builder(requireContext())
                .id(ACTION_ADD_SERVER)
                .title("Add a new server")
                .hasNext(true)
                .build(),
        )

        if (otherServers.isNotEmpty()) {
            val switchActions =
                otherServers.mapIndexed { i, server ->
                    GuidedAction.Builder(requireContext())
                        .id(ACTION_SWITCH_OFFSET + i)
                        .title(server.url)
//                .hasNext(true)
                        .build()
                }
            actions.add(
                GuidedAction.Builder(requireContext())
                    .id(ACTION_SWITCH_OFFSET - 1)
                    .title("Switch to another server")
                    .subActions(switchActions)
                    .build(),
            )

            val removeActions =
                otherServers.mapIndexed { i, server ->
                    GuidedAction.Builder(requireContext())
                        .id(ACTION_REMOVE_OFFSET + i)
                        .title(server.url)
//                .hasNext(true)
                        .build()
                }
            actions.add(
                GuidedAction.Builder(requireContext())
                    .id(ACTION_REMOVE_OFFSET - 1)
                    .title("Remove a server")
                    .subActions(removeActions)
                    .build(),
            )
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
            StashServer.setCurrentStashServer(requireContext(), server)
            finishGuidedStepSupportFragments()
        } else if (action.id >= ACTION_REMOVE_OFFSET) {
            // Remove a server
            val index = action.id - ACTION_REMOVE_OFFSET
            val server = otherServers[index.toInt()]
            StashServer.removeStashServer(requireContext(), server)
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
