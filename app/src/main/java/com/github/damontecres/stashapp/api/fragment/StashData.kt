package com.github.damontecres.stashapp.api.fragment

sealed interface StashData {
    val id: String

    override fun equals(other: Any?): Boolean
}
