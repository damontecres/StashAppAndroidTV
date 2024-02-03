package com.github.damontecres.stashapp.actions

import com.github.damontecres.stashapp.data.OCounter

interface StashActionClickedListener {
    fun onClicked(action: StashAction)

    fun incrementOCounter(counter: OCounter)
}
