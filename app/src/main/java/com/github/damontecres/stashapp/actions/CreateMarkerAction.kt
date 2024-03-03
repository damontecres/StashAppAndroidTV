package com.github.damontecres.stashapp.actions

data class CreateMarkerAction(val position: Long) {
    // This is kind of hacky
    val id get() = StashAction.CREATE_MARKER.id
}
