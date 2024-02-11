package com.github.damontecres.stashapp.actions

enum class StashAction(val id: Long, val actionName: String) {
    ADD_TAG(1L, "Add Tag"),
    ADD_PERFORMER(2L, "Add Performer"),
    FORCE_TRANSCODE(3L, "Play with Transcoding"),
    CREATE_MARKER(4L, "Create Marker from current position"),
    ;

    companion object {
        val SEARCH_FOR_ACTIONS = setOf(ADD_TAG, ADD_PERFORMER, CREATE_MARKER)
    }
}
