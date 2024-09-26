package com.github.damontecres.stashapp.actions

/**
 * Represents an action a user can take
 *
 * This is sort of a catch-all, but [com.github.damontecres.stashapp.presenters.ActionPresenter] can render them
 */
enum class StashAction(val id: Long, val actionName: String) {
    ADD_TAG(1L, "Add Tag"),
    ADD_PERFORMER(2L, "Add Performer"),
    FORCE_TRANSCODE(3L, "Play with Transcoding"),
    CREATE_MARKER(4L, "Create Marker"),
    FORCE_DIRECT_PLAY(5L, "Play directly"),
    CREATE_NEW(6L, "Create new"),
    SET_STUDIO(7L, "Set Studio"),
    SHIFT_MARKERS(8L, "Shift marker timestamp"),
    ADD_GALLERY(9L, "Add Gallery"),
    ;

    companion object {
        // Actions that require searching for something
        val SEARCH_FOR_ACTIONS =
            setOf(ADD_TAG, ADD_PERFORMER, CREATE_MARKER, SET_STUDIO, ADD_GALLERY)
    }
}
