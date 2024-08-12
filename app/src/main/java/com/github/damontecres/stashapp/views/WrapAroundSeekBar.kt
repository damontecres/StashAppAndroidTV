package com.github.damontecres.stashapp.views

import android.content.Context
import android.os.Build
import android.util.AttributeSet
import android.view.KeyEvent
import androidx.appcompat.widget.AppCompatSeekBar

class WrapAroundSeekBar(context: Context, attrs: AttributeSet?) : AppCompatSeekBar(context, attrs) {
    override fun onKeyDown(
        keyCode: Int,
        event: KeyEvent?,
    ): Boolean {
        if (super.isEnabled()) {
            val minimum = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) min else 0
            if ((keyCode == KeyEvent.KEYCODE_DPAD_LEFT || keyCode == KeyEvent.KEYCODE_MINUS) && progress <= minimum) {
                progress = max
                return true
            } else if ((keyCode == KeyEvent.KEYCODE_DPAD_RIGHT || keyCode == KeyEvent.KEYCODE_PLUS) && progress >= max) {
                progress = minimum
                return true
            }
        }
        return super.onKeyDown(keyCode, event)
    }
}
