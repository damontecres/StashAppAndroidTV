package com.github.damontecres.stashapp.util

import android.view.KeyEvent

/**
 * Fragments that implement this will receiving the events from the root activity
 */
interface KeyEventDispatcher {
    fun dispatchKeyEvent(event: KeyEvent): Boolean
}
