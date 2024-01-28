package com.github.damontecres.stashapp

import android.content.Context
import android.view.View
import androidx.core.content.ContextCompat

class StashOnFocusChangeListener(val context: Context) : View.OnFocusChangeListener {
    val mFocusedZoom =
        context.resources.getFraction(
            androidx.leanback.R.fraction.lb_search_orb_focused_zoom,
            1,
            1,
        )

    val mScaleDurationMs =
        context.resources.getInteger(
            androidx.leanback.R.integer.lb_search_orb_scale_duration_ms,
        )

    override fun onFocusChange(
        v: View,
        hasFocus: Boolean,
    ) {
        val zoom = if (hasFocus) mFocusedZoom else 1f
        v.animate().scaleX(zoom).scaleY(zoom).setDuration(mScaleDurationMs.toLong()).start()

        if (hasFocus) {
            v.setBackgroundColor(
                ContextCompat.getColor(
                    context,
                    R.color.selected_background,
                ),
            )
        } else {
            v.setBackgroundColor(
                ContextCompat.getColor(
                    context,
                    R.color.default_card_background,
                ),
            )
        }
    }
}
