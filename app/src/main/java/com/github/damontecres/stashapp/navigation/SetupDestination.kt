package com.github.damontecres.stashapp.navigation

import androidx.navigation3.runtime.NavKey
import com.github.damontecres.stashapp.di.server.StashServer
import kotlinx.serialization.Serializable

@Serializable
sealed interface SetupDestination : NavKey {
    @Serializable
    data object Loading : SetupDestination

    @Serializable
    data object InitialSetup : SetupDestination

    @Serializable
    data object ServerList : SetupDestination

    @Serializable
    data object PinRequired : SetupDestination

    @Serializable
    data class AppContent(
        val server: StashServer,
    ) : SetupDestination
}
