package com.github.damontecres.stashapp.util

import kotlinx.serialization.Serializable

@Serializable
enum class PreferenceScreenOption {
    BASIC,
    ADVANCED,
    USER_INTERFACE,
    ;

    companion object {
        fun fromString(name: String?) = entries.firstOrNull { it.name == name } ?: BASIC
    }
}
