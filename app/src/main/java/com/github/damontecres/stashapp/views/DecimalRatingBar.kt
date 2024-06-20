package com.github.damontecres.stashapp.views

import android.content.Context
import android.util.AttributeSet
import android.view.KeyEvent
import androidx.appcompat.widget.AppCompatSeekBar

class DecimalRatingBar(context: Context, attrs: AttributeSet?) : AppCompatSeekBar(context, attrs) {
    override fun onKeyDown(
        keyCode: Int,
        event: KeyEvent?,
    ): Boolean {
        if (super.isEnabled()) {
            if ((keyCode == KeyEvent.KEYCODE_DPAD_LEFT || keyCode == KeyEvent.KEYCODE_MINUS) && progress <= 0) {
                progress = 100
                return true
            } else if ((keyCode == KeyEvent.KEYCODE_DPAD_RIGHT || keyCode == KeyEvent.KEYCODE_PLUS) && progress >= 100) {
                progress = 0
                return true
            }
        }
        return super.onKeyDown(keyCode, event)
    }
}
