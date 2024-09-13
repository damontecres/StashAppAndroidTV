package com.github.damontecres.stashapp.util

import android.content.Context
import android.content.SharedPreferences
import androidx.core.content.edit
import androidx.preference.PreferenceManager
import com.github.damontecres.stashapp.SettingsFragment
import com.github.damontecres.stashapp.StashApplication
import com.github.damontecres.stashapp.StashExoPlayer

data class StashServer(val url: String, val apiKey: String?) {
    val serverPreferences = ServerPreferences(this)

    companion object {
        fun getCurrentServerVersion(): Version {
            return ServerPreferences(requireCurrentServer()).serverVersion
        }

        fun requireCurrentServer(): StashServer {
            return getCurrentStashServer() ?: throw QueryEngine.StashNotConfiguredException()
        }

        fun getCurrentStashServer(): StashServer? {
            return getCurrentStashServer(StashApplication.getApplication())
        }

        fun getCurrentStashServer(context: Context): StashServer? {
            val manager = PreferenceManager.getDefaultSharedPreferences(context)
            val url = manager.getString(SettingsFragment.PREF_STASH_URL, null)
            val apiKey = manager.getString(SettingsFragment.PREF_STASH_API_KEY, null)
            return if (url.isNotNullOrBlank()) {
                StashServer(url, apiKey)
            } else {
                null
            }
        }

        fun setCurrentStashServer(
            context: Context,
            server: StashServer,
        ) {
            val manager = PreferenceManager.getDefaultSharedPreferences(context)
            manager.edit(true) {
                putString(SettingsFragment.PREF_STASH_URL, server.url)
                putString(SettingsFragment.PREF_STASH_API_KEY, server.apiKey)
            }
            StashClient.invalidate()
            StashExoPlayer.releasePlayer()
        }

        fun removeStashServer(
            context: Context,
            server: StashServer,
        ) {
            val manager = PreferenceManager.getDefaultSharedPreferences(context)
            val serverKey = SettingsFragment.PreferencesFragment.SERVER_PREF_PREFIX + server.url
            val apiKeyKey = SettingsFragment.PreferencesFragment.SERVER_APIKEY_PREF_PREFIX + server.url
            manager.edit(true) {
                remove(serverKey)
                remove(apiKeyKey)
            }
        }

        fun addAndSwitchServer(
            context: Context,
            newServer: StashServer,
            otherSettings: ((SharedPreferences.Editor) -> Unit)? = null,
        ) {
            val manager = PreferenceManager.getDefaultSharedPreferences(context)
            val current = getCurrentStashServer(context)
            val currentServerKey = SettingsFragment.PreferencesFragment.SERVER_PREF_PREFIX + current?.url
            val currentApiKeyKey =
                SettingsFragment.PreferencesFragment.SERVER_APIKEY_PREF_PREFIX + current?.url
            val newServerKey = SettingsFragment.PreferencesFragment.SERVER_PREF_PREFIX + newServer.url
            val newApiKeyKey =
                SettingsFragment.PreferencesFragment.SERVER_APIKEY_PREF_PREFIX + newServer.url
            manager.edit(true) {
                if (current != null) {
                    putString(currentServerKey, current.url)
                    putString(currentApiKeyKey, current.apiKey)
                }
                putString(newServerKey, newServer.url)
                putString(newApiKeyKey, newServer.apiKey)
                putString(SettingsFragment.PREF_STASH_URL, newServer.url)
                putString(SettingsFragment.PREF_STASH_API_KEY, newServer.apiKey)
                if (otherSettings != null) {
                    otherSettings(this)
                }
            }
            StashClient.invalidate()
            StashExoPlayer.releasePlayer()
        }
    }
}
