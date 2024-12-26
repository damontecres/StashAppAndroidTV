package com.github.damontecres.stashapp.util

import android.view.KeyEvent

interface KeyEventDispatcher {
    fun dispatchKeyEvent(event: KeyEvent): Boolean
}
