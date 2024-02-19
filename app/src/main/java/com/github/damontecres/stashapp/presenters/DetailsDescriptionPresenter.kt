package com.github.damontecres.stashapp.presenters

import android.annotation.SuppressLint
import android.view.View
import android.widget.RatingBar
import android.widget.SeekBar
import android.widget.TextView
import androidx.leanback.widget.AbstractDetailsDescriptionPresenter
import com.github.damontecres.stashapp.R
import com.github.damontecres.stashapp.StashOnFocusChangeListener
import com.github.damontecres.stashapp.api.fragment.SlimSceneData
import com.github.damontecres.stashapp.util.Constants
import com.github.damontecres.stashapp.util.ServerPreferences
import com.github.damontecres.stashapp.util.concatIfNotBlank
import com.github.damontecres.stashapp.util.titleOrFilename

class DetailsDescriptionPresenter(val ratingCallback: RatingCallback) :
    AbstractDetailsDescriptionPresenter() {
    override fun onBindDescription(
        viewHolder: ViewHolder,
        item: Any,
    ) {
        val scene = item as SlimSceneData

        viewHolder.title.text = scene.titleOrFilename
        viewHolder.body.text = scene.details

        val file = scene.files.firstOrNull()
        if (file != null) {
            val resolution = "${file.videoFileData.height}P"
            val duration = Constants.durationToString(file.videoFileData.duration)
            viewHolder.subtitle.text =
                concatIfNotBlank(
                    " - ",
                    scene.studio?.name,
                    scene.date,
                    duration,
                    resolution,
                )
        }
        val serverPreferences = ServerPreferences(viewHolder.view.context)
        val type =
            serverPreferences.preferences.getString(ServerPreferences.PREF_RATING_TYPE, "star")
        val precision =
            serverPreferences.preferences.getFloat(ServerPreferences.PREF_RATING_PRECISION, 1.0f)

        val ratingBar = viewHolder.view.findViewById<RatingBar>(R.id.rating)
        val ratingBarDecimal = viewHolder.view.findViewById<SeekBar>(R.id.rating_decimal)
        val ratingBarDecimalHolder = viewHolder.view.findViewById<View>(R.id.rating_decimal_holder)
        val ratingBarDecimalText = viewHolder.view.findViewById<TextView>(R.id.rating_decimal_text)
        if (type == "decimal") {
            ratingBar.visibility = View.GONE
        } else {
            // star
            ratingBarDecimalHolder.visibility = View.GONE
        }

        var currentRating = scene.rating100 ?: 0

        ratingBar.rating = (scene.rating100?.div(20.0))?.toFloat() ?: 0.0f
        ratingBar.stepSize = precision

        ratingBarDecimal.setOnSeekBarChangeListener(
            object : SeekBar.OnSeekBarChangeListener {
                @SuppressLint("SetTextI18n")
                override fun onProgressChanged(
                    seekBar: SeekBar?,
                    progress: Int,
                    fromUser: Boolean,
                ) {
                    ratingBarDecimalText.text =
                        viewHolder.view.context.getString(R.string.stashapp_rating) + " (${progress / 10.0}):"
                }

                override fun onStartTrackingTouch(seekBar: SeekBar?) {
                    // no-op
                }

                override fun onStopTrackingTouch(seekBar: SeekBar?) {
                    // no-op
                }
            },
        )
        ratingBarDecimal.progress = scene.rating100 ?: 0
        ratingBarDecimalText.text =
            viewHolder.view.context.getString(R.string.stashapp_rating) + " (${currentRating / 10.0}):"

        ratingBar.setOnClickListener {
            currentRating = (ratingBar.rating * 20).toInt()
            ratingCallback.setRating(scene.id.toInt(), currentRating)
        }
        ratingBarDecimal.setOnClickListener {
            currentRating = ratingBarDecimal.progress
            ratingCallback.setRating(scene.id.toInt(), currentRating)
        }

        val focusChangeListener =
            object : StashOnFocusChangeListener(ratingBar.context) {
                override fun onFocusChange(
                    v: View,
                    hasFocus: Boolean,
                ) {
                    super.onFocusChange(v, hasFocus)
                    if (!hasFocus) {
                        ratingBar.rating = currentRating / 20.0f
                        ratingBarDecimal.progress = currentRating
                    }
                }
            }
        ratingBar.onFocusChangeListener = focusChangeListener
        ratingBarDecimal.onFocusChangeListener = focusChangeListener
    }

    fun interface RatingCallback {
        fun setRating(
            sceneId: Int,
            rating100: Int,
        )
    }
}
