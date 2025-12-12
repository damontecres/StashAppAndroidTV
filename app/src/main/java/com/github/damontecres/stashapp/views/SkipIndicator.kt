package com.github.damontecres.stashapp.views

import android.annotation.SuppressLint
import android.content.Context
import android.graphics.drawable.Drawable
import android.util.AttributeSet
import android.view.View
import android.widget.FrameLayout
import android.widget.ImageView
import android.widget.TextView
import androidx.core.content.res.ResourcesCompat
import com.github.damontecres.stashapp.R
import kotlin.math.abs

/**
 * A visual indicator to show when skipping forward/back during a scene
 */
class SkipIndicator(
    context: Context,
    attrs: AttributeSet?,
    defStyleAttr: Int,
) : FrameLayout(context, attrs, defStyleAttr) {
    constructor(context: Context) : this(context, null, 0)
    constructor(context: Context, attrs: AttributeSet?) : this(context, attrs, 0)

    private val image: ImageView
    private val text: TextView
    private val drawable: Drawable

    private var currentSkip = 0L

    init {
        inflate(context, R.layout.skip_indicator, this)
        image = findViewById(R.id.duration_image)
        text = findViewById(R.id.duration_text)
        drawable = ResourcesCompat.getDrawable(resources, R.drawable.circular_arrow_right, null)!!
    }

    /**
     * Update the text duration by an amount and restart the animation's duration
     */
    @SuppressLint("SetTextI18n")
    fun update(delta: Long) {
        // If switching from fast forward to back (or vice versa), reset the value
        if ((currentSkip > 0 && delta < 0) || (currentSkip < 0 && delta > 0)) {
            currentSkip = 0
        }
        currentSkip += (delta / 1000)

        val rotation = if (delta > 0) 180f else -180f
        if (currentSkip > 0) {
            image.setImageDrawable(drawable)
            image.scaleX = 1f
        } else {
            // Mirror if skipping back
            image.setImageDrawable(drawable)
            image.scaleX = -1f
        }
        visibility = View.VISIBLE
        text.text = abs(currentSkip).toString()
        image
            .animate()
            .setDuration(800L)
            .rotationBy(rotation)
            .withEndAction {
                visibility = View.GONE
                image.setImageDrawable(null)
                image.rotation = 0f
                currentSkip = 0
            }
    }
}
