package com.github.damontecres.stashapp.actions

import com.github.damontecres.stashapp.data.OCounter

/**
 * Respond to clicks on a card for a [StashAction]
 */
interface StashActionClickedListener {
    fun onClicked(action: StashAction)

    fun incrementOCounter(counter: OCounter)
}
