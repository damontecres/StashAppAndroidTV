package com.github.damontecres.stashapp.views

import android.content.Context
import android.graphics.drawable.Drawable
import android.util.AttributeSet
import android.util.Log
import androidx.annotation.AttrRes
import com.otaliastudios.zoom.ZoomImageView

class StashZoomImageView private constructor(
    context: Context,
    attrs: AttributeSet? = null,
    @AttrRes defStyleAttr: Int = 0,
) : ZoomImageView(context, attrs, defStyleAttr) {
    constructor(
        context: Context,
        attrs: AttributeSet? = null,
    ) : this(context, attrs, 0)

    override fun setImageDrawable(drawable: Drawable?) {
        Log.v(TAG, "setImageDrawable: drawable=$drawable")
        super.setImageDrawable(drawable)
        moveToCenter(1.0f, false)
    }

    companion object {
        private const val TAG = "StashZoomImageView"
    }
}
