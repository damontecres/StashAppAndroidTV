package com.github.damontecres.stashapp.di.services

import androidx.navigation3.runtime.NavBackStack
import co.touchlab.kermit.Logger
import com.github.damontecres.stashapp.navigation.Destination
import org.acra.ACRA
import org.koin.core.annotation.Single

@Single
class NavigationManager {
    var backStack: MutableList<Destination> = NavBackStack(Destination.Main)

    /**
     * Go to the specified [Destination]
     */
    fun navigate(destination: Destination) {
        backStack.add(destination)
        log()
    }

    /**
     * Go to the specified [Destination], but reset the back stack to Home first
     */
    fun navigateToFromDrawer(destination: Destination) {
        goToMain()
        backStack.add(destination)
        log()
    }

    /**
     * Go to the previous page
     */
    fun goBack() {
        synchronized(this) {
            if (backStack.size > 1) {
                backStack.removeLastOrNull()
            }
        }
        log()
    }

    /**
     * Go all the way back to the home page
     */
    fun goToMain() {
        while (backStack.size > 1) {
            backStack.removeLastOrNull()
        }
        if (backStack[0] !is Destination.Main) {
            backStack[0] = Destination.Main
        }
        log()
    }

    /**
     * Go all the way back to the home page, and reload it from scratch
     */
    fun reloadHome() {
        goToMain()
        // TODO
//        val id = (backStack[0] as Destination.Main).id + 1
//        backStack[0] = Destination.Main(id)
//        log()
    }

    /**
     * Resets the backstack to the specified destination
     */
    fun replace(destination: Destination) {
        while (backStack.size > 1) {
            backStack.removeLastOrNull()
        }
        if (backStack.isEmpty()) {
            backStack.add(0, destination)
        } else {
            backStack[0] = destination
        }
        log()
    }

    private fun log() {
        val dest = backStack.lastOrNull().toString()
        Logger.i { "Current Destination: $dest" }
        ACRA.errorReporter.putCustomData("destination", dest)
    }
}
