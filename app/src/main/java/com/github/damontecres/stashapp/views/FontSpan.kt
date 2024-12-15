
package com.github.damontecres.stashapp.views

import android.graphics.Typeface
import android.text.TextPaint
import android.text.style.MetricAffectingSpan

/**
 * Apply a font to a span
 */
class FontSpan(
    private val font: Typeface,
) : MetricAffectingSpan() {
    override fun updateMeasureState(textPaint: TextPaint) = setFont(textPaint)

    override fun updateDrawState(textPaint: TextPaint) = setFont(textPaint)

    private fun setFont(textPaint: TextPaint) {
        textPaint.apply {
            // Set the font and use the current style if available
            typeface = Typeface.create(font, typeface?.style ?: Typeface.NORMAL)
        }
    }
}
