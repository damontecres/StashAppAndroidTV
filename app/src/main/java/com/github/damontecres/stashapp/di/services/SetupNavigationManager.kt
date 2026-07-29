package com.github.damontecres.stashapp.di.services

import androidx.compose.runtime.mutableStateListOf
import com.github.damontecres.stashapp.navigation.SetupDestination
import org.acra.ACRA
import org.koin.core.annotation.Single
import timber.log.Timber

@Single
class SetupNavigationManager(
    private val navigationManager: NavigationManager,
) {
    var backStack: MutableList<SetupDestination> = mutableStateListOf(SetupDestination.Loading)

    /**
     * Go to the specified [SetupDestination]
     */
    fun navigateTo(destination: SetupDestination) {
        backStack[0] = destination
        log()
        if (destination !is SetupDestination.AppContent) {
            navigationManager.reloadMain()
        }
    }

    private fun log() {
        val dest = backStack.lastOrNull().toString()
        Timber.i("Current setup destination: %s", dest)
        ACRA.errorReporter.putCustomData("setupDestination", dest)
    }
}
