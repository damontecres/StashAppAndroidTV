package com.github.damontecres.stashapp.views

import android.content.Context
import android.view.View
import androidx.annotation.FractionRes

/**
 * A [View.OnFocusChangeListener] which slightly zooms the view
 */
open class StashOnFocusChangeListener(
    val context: Context,
    @FractionRes fraction: Int = androidx.leanback.R.fraction.lb_search_orb_focused_zoom,
) : View.OnFocusChangeListener {
    private val mFocusedZoom = context.resources.getFraction(fraction, 1, 1)

    private val mScaleDurationMs =
        context.resources.getInteger(
            androidx.leanback.R.integer.lb_search_orb_scale_duration_ms,
        )

    override fun onFocusChange(
        v: View,
        hasFocus: Boolean,
    ) {
        val zoom = if (hasFocus) mFocusedZoom else 1f
        v.animate().scaleX(zoom).scaleY(zoom).setDuration(mScaleDurationMs.toLong()).start()
    }
}
