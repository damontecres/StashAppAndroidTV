package com.github.damontecres.stashapp.playback

import android.os.Bundle
import android.view.View
import android.widget.Button
import android.widget.SeekBar
import android.widget.SeekBar.OnSeekBarChangeListener
import android.widget.TextView
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

        val redText = view.findViewById<TextView>(R.id.red_adjust_text)
        val greenText = view.findViewById<TextView>(R.id.green_adjust_text)
        val blueText = view.findViewById<TextView>(R.id.blue_adjust_text)
        val brightnessText = view.findViewById<TextView>(R.id.brightness_adjust_text)
        val contrastText = view.findViewById<TextView>(R.id.contrast_adjust_text)
        val saturationText = view.findViewById<TextView>(R.id.saturation_adjust_text)
        val hueText = view.findViewById<TextView>(R.id.hue_adjust_text)

        redAdjust.setOnSeekBarChangeListener {
            viewModel.videoFilter.value = getOrCreateVideoFilter().copy(red = it)
            redText.text = "$it%"
        }
        greenAdjust.setOnSeekBarChangeListener {
            viewModel.videoFilter.value = getOrCreateVideoFilter().copy(green = it)
            greenText.text = "$it%"
        }
        blueAdjust.setOnSeekBarChangeListener {
            viewModel.videoFilter.value = getOrCreateVideoFilter().copy(blue = it)
            blueText.text = "$it%"
        }
        brightnessAdjust.setOnSeekBarChangeListener {
            viewModel.videoFilter.value = getOrCreateVideoFilter().copy(brightness = it)
            brightnessText.text = "$it%"
        }
        contrastAdjust.setOnSeekBarChangeListener {
            viewModel.videoFilter.value = getOrCreateVideoFilter().copy(contrast = it)
            contrastText.text = "$it%"
        }
        saturationAdjust.setOnSeekBarChangeListener {
            viewModel.videoFilter.value = getOrCreateVideoFilter().copy(saturation = it)
            saturationText.text = "$it%"
        }
        hueAdjust.setOnSeekBarChangeListener {
            viewModel.videoFilter.value = getOrCreateVideoFilter().copy(hue = it)
            hueText.text = "$it"
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
        val submitButton = view.findViewById<Button>(R.id.apply_button)
        submitButton.setOnClickListener {
            viewModel.saveFilter()
            requireActivity().supportFragmentManager.beginTransaction()
                .remove(this@PlaybackFilterFragment)
                .commitNow()
        }

        val resetButton = view.findViewById<Button>(R.id.reset_button)
        resetButton.setOnClickListener {
            viewModel.videoFilter.value = VideoFilter()
            listOf(
                redAdjust,
                greenAdjust,
                blueAdjust,
                brightnessAdjust,
                contrastAdjust,
                saturationAdjust,
            ).forEach {
                it.progress = VideoFilter.COLOR_DEFAULT
            }
            hueAdjust.progress = VideoFilter.HUE_DEFAULT
            listOf(
                redText,
                greenText,
                blueText,
                brightnessText,
                contrastText,
                saturationText,
            ).forEach {
                it.text = VideoFilter.COLOR_DEFAULT.toString() + "%"
            }
            hueText.text = "0"
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
                if (fromUser) {
                    listener(progress)
                }
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
