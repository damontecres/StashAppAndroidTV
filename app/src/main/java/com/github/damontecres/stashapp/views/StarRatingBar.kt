package com.github.damontecres.stashapp.views

import android.content.Context
import android.util.AttributeSet
import android.view.KeyEvent
import androidx.appcompat.widget.AppCompatRatingBar

class StarRatingBar(context: Context, attrs: AttributeSet?) : AppCompatRatingBar(context, attrs) {
    override fun onKeyDown(
        keyCode: Int,
        event: KeyEvent?,
    ): Boolean {
        if (super.isEnabled()) {
            if ((keyCode == KeyEvent.KEYCODE_DPAD_LEFT || keyCode == KeyEvent.KEYCODE_MINUS) && rating < .05f) {
                rating = 5f
                return true
            } else if ((keyCode == KeyEvent.KEYCODE_DPAD_RIGHT || keyCode == KeyEvent.KEYCODE_PLUS) && rating > 4.95f) {
                rating = 0f
                return true
            }
        }
        return super.onKeyDown(keyCode, event)
    }
}
