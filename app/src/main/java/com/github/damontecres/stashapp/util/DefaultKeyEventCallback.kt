package com.github.damontecres.stashapp.util

import android.view.KeyEvent

interface DefaultKeyEventCallback : KeyEvent.Callback {
    override fun onKeyDown(
        keyCode: Int,
        event: KeyEvent,
    ): Boolean {
        return false
    }

    override fun onKeyUp(
        keyCode: Int,
        event: KeyEvent,
    ): Boolean {
        return false
    }

    override fun onKeyLongPress(
        keyCode: Int,
        event: KeyEvent,
    ): Boolean {
        return false
    }

    override fun onKeyMultiple(
        keyCode: Int,
        count: Int,
        event: KeyEvent,
    ): Boolean {
        return false
    }
}
