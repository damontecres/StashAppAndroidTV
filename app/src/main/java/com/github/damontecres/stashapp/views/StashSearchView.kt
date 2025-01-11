package com.github.damontecres.stashapp.views

import android.content.Context
import android.util.AttributeSet
import android.view.KeyEvent
import android.widget.SearchView

class StashSearchView(
    context: Context,
    attrs: AttributeSet,
    defStyle: Int,
) : SearchView(context, attrs, defStyle) {
    constructor(
        context: Context,
        attrs: AttributeSet,
    ) : this(context, attrs, android.R.attr.searchViewStyle)

    override fun onKeyDown(
        keyCode: Int,
        event: KeyEvent,
    ): Boolean {
        if (keyCode == KeyEvent.KEYCODE_DPAD_CENTER || keyCode == KeyEvent.KEYCODE_ENTER || keyCode == KeyEvent.KEYCODE_NUMPAD_ENTER) {
            if (isIconified) {
                isIconified = false
            }
        }
        return super.onKeyDown(keyCode, event)
    }

    companion object {
        private const val TAG = "StashSearchView"
    }
}
