package com.github.damontecres.stashapp.util

import android.view.KeyEvent

interface DefaultKeyEventCallback : KeyEvent.Callback {
    override fun onKeyDown(
        keyCode: Int,
        event: KeyEvent,
    ): Boolean = false

    override fun onKeyUp(
        keyCode: Int,
        event: KeyEvent,
    ): Boolean = false

    override fun onKeyLongPress(
        keyCode: Int,
        event: KeyEvent,
    ): Boolean = false

    override fun onKeyMultiple(
        keyCode: Int,
        count: Int,
        event: KeyEvent,
    ): Boolean = false
}

interface DelegateKeyEventCallback : KeyEvent.Callback {
    val keyEventDelegate: KeyEvent.Callback?

    override fun onKeyDown(
        keyCode: Int,
        event: KeyEvent,
    ): Boolean = keyEventDelegate?.onKeyDown(keyCode, event) ?: false

    override fun onKeyUp(
        keyCode: Int,
        event: KeyEvent,
    ): Boolean = keyEventDelegate?.onKeyUp(keyCode, event) ?: false

    override fun onKeyLongPress(
        keyCode: Int,
        event: KeyEvent,
    ): Boolean = keyEventDelegate?.onKeyLongPress(keyCode, event) ?: false

    override fun onKeyMultiple(
        keyCode: Int,
        count: Int,
        event: KeyEvent,
    ): Boolean = keyEventDelegate?.onKeyMultiple(keyCode, count, event) ?: false
}
