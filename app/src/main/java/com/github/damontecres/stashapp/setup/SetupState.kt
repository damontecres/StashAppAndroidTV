package com.github.damontecres.stashapp.setup

import com.github.damontecres.stashapp.util.StashServer

data class SetupState(
    val serverUrl: String,
    val apiKey: String?,
    val trustCerts: Boolean = false,
    val pinCode: Int? = null,
) {
    constructor(serverUrl: CharSequence) : this(serverUrl.toString(), null)

    val stashServer get() = StashServer(serverUrl, apiKey)
}
