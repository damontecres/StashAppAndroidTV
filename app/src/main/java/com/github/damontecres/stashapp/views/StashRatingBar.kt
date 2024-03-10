package com.github.damontecres.stashapp.views

import android.annotation.SuppressLint
import android.content.Context
import android.util.AttributeSet
import android.util.TypedValue
import android.view.View
import android.widget.FrameLayout
import android.widget.RatingBar
import android.widget.SeekBar
import android.widget.TextView
import androidx.core.content.ContextCompat
import com.github.damontecres.stashapp.R
import com.github.damontecres.stashapp.util.ServerPreferences

class StashRatingBar(context: Context, attrs: AttributeSet?) : FrameLayout(context, attrs) {
    constructor(context: Context) : this(context, null)

    private val starRatingBar: RatingBar

    private val decimalLayout: View
    private val decimalRatingText: TextView
    private val decimalRatingBar: SeekBar

    val ratingAsStars: Boolean

    private var ratingCallback: RatingCallback? = null

    var rating100: Int = 0
        @SuppressLint("RestrictedApi", "SetTextI18n")
        set(rating100) {
            field = rating100
            starRatingBar.rating = (field.div(20.0)).toFloat()
            decimalRatingBar.progress = field
            decimalRatingText.text = context.getString(R.string.stashapp_rating) + " (${field / 10.0}):"
        }

    init {
        inflate(context, R.layout.stash_rating_bar, this)

        val serverPreferences = ServerPreferences(context)

        starRatingBar = findViewById(R.id.rating_star)
        starRatingBar.setOnClickListener(RatingOnClickListener())

        decimalLayout = findViewById(R.id.rating_decimal_holder)
        decimalRatingText = findViewById(R.id.rating_decimal_text)
        decimalRatingBar = findViewById(R.id.rating_decimal)
        decimalRatingBar.setOnClickListener(RatingOnClickListener())

        ratingAsStars = serverPreferences.ratingsAsStars
        if (ratingAsStars) {
            decimalLayout.visibility = View.GONE
        } else {
            starRatingBar.visibility = View.GONE
        }
        context.theme.obtainStyledAttributes(
            attrs,
            R.styleable.StashRatingBar,
            0,
            0,
        ).apply {
            try {
                rating100 = getInteger(R.styleable.StashRatingBar_defaultRating100, 0)

                decimalRatingText.setTextSize(TypedValue.COMPLEX_UNIT_PX, getDimension(R.styleable.StashRatingBar_android_textSize, 16f))
                decimalRatingText.setTextColor(
                    getColor(R.styleable.StashRatingBar_android_textColor, resources.getColor(android.R.color.white, null)),
                )
            } finally {
                recycle()
            }
        }

        val precision =
            serverPreferences.preferences.getFloat(ServerPreferences.PREF_RATING_PRECISION, 1.0f)
        starRatingBar.stepSize = precision

        decimalRatingBar.setOnSeekBarChangeListener(
            object : SeekBar.OnSeekBarChangeListener {
                @SuppressLint("SetTextI18n")
                override fun onProgressChanged(
                    seekBar: SeekBar?,
                    progress: Int,
                    fromUser: Boolean,
                ) {
                    decimalRatingText.text =
                        context.getString(R.string.stashapp_rating) + " (${progress / 10.0}):"
                }

                override fun onStartTrackingTouch(seekBar: SeekBar?) {
                    // no-op
                }

                override fun onStopTrackingTouch(seekBar: SeekBar?) {
                    // no-op
                }
            },
        )

        val focusChangeListener =
            object : OnFocusChangeListener {
                @SuppressLint("SetTextI18n")
                override fun onFocusChange(
                    v: View,
                    hasFocus: Boolean,
                ) {
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
                        starRatingBar.rating = rating100 / 20.0f
                        decimalRatingBar.progress = rating100
                        decimalRatingText.text = context.getString(R.string.stashapp_rating) + " (${rating100 / 10.0}):"
                    }
                }
            }
        starRatingBar.onFocusChangeListener = focusChangeListener
        decimalRatingBar.onFocusChangeListener = focusChangeListener
    }

    fun setRatingCallback(ratingCallback: RatingCallback) {
        this.ratingCallback = ratingCallback
    }

    private inner class RatingOnClickListener : OnClickListener {
        @SuppressLint("RestrictedApi")
        override fun onClick(v: View) {
            rating100 =
                if (ratingAsStars) {
                    (starRatingBar.rating * 20).toInt()
                } else {
                    decimalRatingBar.progress
                }
            ratingCallback?.setRating(rating100)
        }
    }

    fun interface RatingCallback {
        fun setRating(rating100: Int)
    }
}
