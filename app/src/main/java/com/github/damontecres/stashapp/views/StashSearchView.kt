package com.github.damontecres.stashapp.views

import android.content.Context
import android.util.AttributeSet
import android.util.Log
import android.view.KeyEvent
import android.view.ViewGroup
import android.widget.SearchView
import androidx.core.view.get

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
            Log.v(TAG, "dpad center")
            if (isIconified) {
                isIconified = false
            } else {
                isIconified = false
                (((getChildAt(0) as ViewGroup)[2] as ViewGroup)[0] as ViewGroup)[1].performClick()
//                findViewById<View>(R.id.search_src_text)
            }
        }
        return super.onKeyDown(keyCode, event)
    }

    companion object {
        private const val TAG = "StashSearchView"
    }
}
