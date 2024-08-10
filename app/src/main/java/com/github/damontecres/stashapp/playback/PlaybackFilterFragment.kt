package com.github.damontecres.stashapp.playback

import android.os.Bundle
import android.util.Log
import android.view.View
import android.widget.Button
import android.widget.SeekBar
import android.widget.SeekBar.OnSeekBarChangeListener
import androidx.fragment.app.Fragment
import androidx.fragment.app.activityViewModels
import com.github.damontecres.stashapp.R
import com.github.damontecres.stashapp.data.room.VideoFilter

class PlaybackFilterFragment : Fragment(R.layout.apply_video_filters) {
    private val viewModel: VideoFilterViewModel by activityViewModels()

    override fun onViewCreated(
        view: View,
        savedInstanceState: Bundle?,
    ) {
        super.onViewCreated(view, savedInstanceState)

        val redAdjust = view.findViewById<SeekBar>(R.id.red_adjust)
        val greenAdjust = view.findViewById<SeekBar>(R.id.green_adjust)
        val blueAdjust = view.findViewById<SeekBar>(R.id.blue_adjust)
        val brightnessAdjust = view.findViewById<SeekBar>(R.id.brightness_adjust)
        val contrastAdjust = view.findViewById<SeekBar>(R.id.contrast_adjust)
        val saturationAdjust = view.findViewById<SeekBar>(R.id.saturation_adjust)
        val hueAdjust = view.findViewById<SeekBar>(R.id.hue_adjust)

        redAdjust.setOnSeekBarChangeListener {
            Log.v(TAG, "red=$it")
            viewModel.videoFilter.value = getOrCreateVideoFilter().copy(red = it)
        }
        greenAdjust.setOnSeekBarChangeListener {
            Log.v(TAG, "green=$it")
            viewModel.videoFilter.value = getOrCreateVideoFilter().copy(green = it)
        }
        blueAdjust.setOnSeekBarChangeListener {
            Log.v(TAG, "blue=$it")
            viewModel.videoFilter.value = getOrCreateVideoFilter().copy(blue = it)
        }
        brightnessAdjust.setOnSeekBarChangeListener {
            Log.v(TAG, "brightness=$it")
            viewModel.videoFilter.value = getOrCreateVideoFilter().copy(brightness = it)
        }
        contrastAdjust.setOnSeekBarChangeListener {
            Log.v(TAG, "contrast=$it")
            viewModel.videoFilter.value = getOrCreateVideoFilter().copy(contrast = it)
        }
        saturationAdjust.setOnSeekBarChangeListener {
            Log.v(TAG, "saturation=$it")
            viewModel.videoFilter.value = getOrCreateVideoFilter().copy(saturation = it)
        }
        hueAdjust.setOnSeekBarChangeListener {
            Log.v(TAG, "hue=$it")
            viewModel.videoFilter.value = getOrCreateVideoFilter().copy(hue = it)
        }

        val rotateLeftButton = view.findViewById<Button>(R.id.rotate_left_button)
        val rotateRightButton = view.findViewById<Button>(R.id.rotate_right_button)
        rotateLeftButton.setOnClickListener {
            val vf = getOrCreateVideoFilter()
            viewModel.videoFilter.value = vf.copy(rotation = vf.rotation + 90)
        }
        rotateRightButton.setOnClickListener {
            val vf = getOrCreateVideoFilter()
            viewModel.videoFilter.value = vf.copy(rotation = vf.rotation - 90)
        }
    }

    private fun getOrCreateVideoFilter(): VideoFilter {
        return if (viewModel.videoFilter.isInitialized) {
            viewModel.videoFilter.value!!
        } else {
            VideoFilter()
        }
    }

    companion object {
        const val TAG = "PlaybackFilterFragment"
    }
}

fun SeekBar.setOnSeekBarChangeListener(listener: (Int) -> Unit) {
    setOnSeekBarChangeListener(
        object : OnSeekBarChangeListener {
            override fun onProgressChanged(
                seekBar: SeekBar,
                progress: Int,
                fromUser: Boolean,
            ) {
                listener(progress)
            }

            override fun onStartTrackingTouch(seekBar: SeekBar?) {
                // no-op
            }

            override fun onStopTrackingTouch(seekBar: SeekBar?) {
                // no-op
            }
        },
    )
}
