package com.github.damontecres.stashapp.di.services

import androidx.datastore.core.DataStore
import com.github.damontecres.stashapp.di.server.ServerRepository
import com.github.damontecres.stashapp.proto.StashPreferences
import com.github.damontecres.stashapp.util.plugin.CompanionPluginService
import kotlinx.coroutines.flow.first
import org.koin.core.annotation.Single

@Single
class ServerLogger(
    private val preferences: DataStore<StashPreferences>,
    private val serverRepository: ServerRepository,
    private val companionPluginService: CompanionPluginService,
) {
    suspend fun logException(
        exception: Throwable,
        message: String? = null,
    ) {
        if (preferences.data
                .first()
                .advancedPreferences.logErrorsToServer
        ) {
            if (serverRepository.currentServer.value.serverPreferences.companionPluginInstalled) {
                companionPluginService.sendLogMessage(
                    "mesg=$message\n${exception.stackTraceToString()}",
                    true,
                )
            }
        }
    }

    suspend fun <T, R> T.run(block: T.() -> R): Result<R> =
        runCatching {
            block.invoke(this)
        }.onFailure { ex ->
            logException(ex, null)
        }
}
