package com.github.damontecres.stashapp.util

import android.content.Context
import android.content.SharedPreferences
import androidx.core.content.edit
import androidx.preference.PreferenceManager
import com.github.damontecres.stashapp.SettingsFragment
import com.github.damontecres.stashapp.StashApplication
import com.github.damontecres.stashapp.StashExoPlayer
import java.util.concurrent.ConcurrentHashMap

/**
 * Represents a server
 */
data class StashServer(
    val url: String,
    val apiKey: String?,
) {
    /**
     * The server side preferences
     *
     * Note: needs to populated via [updateServerPrefs]!
     */
    val serverPreferences = ServerPreferences(this)

    /**
     * The server's version
     *
     * Depends on [serverPreferences] which depends on [updateServerPrefs]!
     */
    val version: Version
        get() {
            return serverPreferences.serverVersion
        }

    val okHttpClient = StashClient.createOkHttpClient(this, true, true)

    //    val streamingOkHttpClient =  StashClient.createOkHttpClient(this, true, false)
    val apolloClient = StashClient.createApolloClient(this)

    /**
     * Query the server for preferences
     */
    suspend fun updateServerPrefs(): ServerPreferences {
        val queryEngine = QueryEngine(this)
        val result = queryEngine.getServerConfiguration()
        serverPreferences.updatePreferences(result)
        return serverPreferences
    }

    override fun toString(): String = "StashServer(url=$url, apiKey?=${apiKey.isNotNullOrBlank()})"

    companion object {
        private const val SERVER_PREF_PREFIX = "server_"
        private const val SERVER_APIKEY_PREF_PREFIX = "apikey_"

        private val servers = ConcurrentHashMap<String, StashServer>()

        fun getCurrentServerVersion(): Version = ServerPreferences(requireCurrentServer()).serverVersion

        fun requireCurrentServer(): StashServer {
            if (StashApplication.currentServer == null) {
                val server =
                    findConfiguredStashServer(StashApplication.getApplication())
                        ?: throw QueryEngine.StashNotConfiguredException()
                setCurrentStashServer(StashApplication.getApplication(), server)
            }
            return StashApplication.requireCurrentServer()
        }

        fun getCurrentStashServer(): StashServer? = StashApplication.currentServer

        fun findConfiguredStashServer(context: Context): StashServer? {
            val manager = PreferenceManager.getDefaultSharedPreferences(context)
            val url = manager.getString(SettingsFragment.PREF_STASH_URL, null)
            val apiKey = manager.getString(SettingsFragment.PREF_STASH_API_KEY, null)
            return if (url.isNotNullOrBlank()) {
                servers.getOrPut(url) { StashServer(url, apiKey) }
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
            StashExoPlayer.releasePlayer()
            StashApplication.currentServer = server
        }

        fun removeStashServer(
            context: Context,
            server: StashServer,
        ) {
            val manager = PreferenceManager.getDefaultSharedPreferences(context)
            val serverKey = SERVER_PREF_PREFIX + server.url
            val apiKeyKey = SERVER_APIKEY_PREF_PREFIX + server.url
            manager.edit(true) {
                remove(serverKey)
                remove(apiKeyKey)
            }
            server.serverPreferences.preferences.edit(true) {
                clear()
            }
        }

        fun addAndSwitchServer(
            context: Context,
            newServer: StashServer,
            otherSettings: ((SharedPreferences.Editor) -> Unit)? = null,
        ) {
            val manager = PreferenceManager.getDefaultSharedPreferences(context)
            val current = findConfiguredStashServer(context)
            val currentServerKey = SERVER_PREF_PREFIX + current?.url
            val currentApiKeyKey =
                SERVER_APIKEY_PREF_PREFIX + current?.url
            val newServerKey = SERVER_PREF_PREFIX + newServer.url
            val newApiKeyKey =
                SERVER_APIKEY_PREF_PREFIX + newServer.url
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
            StashExoPlayer.releasePlayer()
        }

        fun getAll(context: Context): List<StashServer> {
            val manager = PreferenceManager.getDefaultSharedPreferences(context)
            val keys =
                manager.all.keys
                    .filter { it.startsWith(SERVER_PREF_PREFIX) }
                    .sorted()
                    .toList()
            return keys
                .map {
                    val url = it.replace(SERVER_PREF_PREFIX, "")
                    val apiKeyKey =
                        it.replace(
                            SERVER_PREF_PREFIX,
                            SERVER_APIKEY_PREF_PREFIX,
                        )
                    val apiKey =
                        manager.all[apiKeyKey]
                            ?.toString()
                            ?.replace(SERVER_APIKEY_PREF_PREFIX, "")
                    StashServer(url, apiKey)
                }.sortedBy { it.url }
        }
    }
}
