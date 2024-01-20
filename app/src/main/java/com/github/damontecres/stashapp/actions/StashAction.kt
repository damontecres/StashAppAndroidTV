package com.github.damontecres.stashapp.actions

import android.content.Context

interface StashAction {
    val name: String

    fun execute(context: Context)
}
