package com.github.damontecres.stashapp.di.server

import android.content.Context
import androidx.core.content.edit
import androidx.datastore.core.DataStore
import androidx.preference.PreferenceManager
import com.github.damontecres.stashapp.proto.StashPreferences
import com.github.damontecres.stashapp.util.isNotNullOrBlank
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.update
import org.koin.core.annotation.Single
import java.util.concurrent.ConcurrentHashMap

@Single
class ServerRepository(
    private val context: Context,
    private val api: StashApi,
    private val preferences: DataStore<StashPreferences>,
) {
    val currentServer = MutableStateFlow(CurrentServer.UNSET)
    val currentServerVersion get() = currentServer.value.serverPreferences.version

    private val servers = ConcurrentHashMap<String, StashServer>()

    suspend fun restore(): Boolean {
        val manager = PreferenceManager.getDefaultSharedPreferences(context)
        val url = manager.getString(PREF_STASH_URL, null)
        val apiKey = manager.getString(PREF_STASH_API_KEY, null)
        return if (url.isNotNullOrBlank()) {
            addAndSwitchServer(StashServer(url, apiKey))
            true
        } else {
            false
        }
    }

    suspend fun findConfiguredStashServer(context: Context): StashServer? {
        val manager = PreferenceManager.getDefaultSharedPreferences(context)
        val url = manager.getString(PREF_STASH_URL, null)
        val apiKey = manager.getString(PREF_STASH_API_KEY, null)
        return if (url.isNotNullOrBlank()) {
            servers.getOrPut(url) { StashServer(url, apiKey) }
        } else {
            null
        }
    }

    suspend fun setCurrentStashServer(server: StashServer) {
        addAndSwitchServer(server)
    }

    suspend fun removeStashServer(server: StashServer) {
        val manager = PreferenceManager.getDefaultSharedPreferences(context)
        val serverKey = SERVER_PREF_PREFIX + server.url
        val apiKeyKey = SERVER_APIKEY_PREF_PREFIX + server.url
        manager.edit(true) {
            remove(serverKey)
            remove(apiKeyKey)
        }
    }

    suspend fun addServer(newServer: StashServer) {
        val manager = PreferenceManager.getDefaultSharedPreferences(context)
        val newServerKey = SERVER_PREF_PREFIX + newServer.url
        val newApiKeyKey = SERVER_APIKEY_PREF_PREFIX + newServer.url
        manager.edit(true) {
            putString(newServerKey, newServer.url)
            putString(newApiKeyKey, newServer.apiKey)
        }
    }

    suspend fun addAndSwitchServer(newServer: StashServer) {
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
            putString(PREF_STASH_URL, newServer.url)
            putString(PREF_STASH_API_KEY, newServer.apiKey)
        }
        api.changeServer(preferences.data.first(), newServer)
        val config = QueryEngine(api).getServerConfiguration()
        val serverPreferences = ServerPreferences.createServerPreferences(config)
        currentServer.update {
            CurrentServer(newServer, serverPreferences)
        }
    }

    suspend fun updateServerPreferences() {
        val config = QueryEngine(api).getServerConfiguration()
        val serverPreferences = ServerPreferences.createServerPreferences(config)
        currentServer.update {
            it.copy(serverPreferences = serverPreferences)
        }
    }

    suspend fun getAll(): List<StashServer> {
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

    companion object {
        private const val SERVER_PREF_PREFIX = "server_"
        private const val SERVER_APIKEY_PREF_PREFIX = "apikey_"

        const val PREF_STASH_URL = "stashUrl"
        const val PREF_STASH_API_KEY = "stashApiKey"
    }
}

data class CurrentServer(
    val server: StashServer,
    val serverPreferences: ServerPreferences,
) {
    companion object {
        val UNSET =
            CurrentServer(
                StashServer.UNSET,
                ServerPreferences(),
            )
    }
}
