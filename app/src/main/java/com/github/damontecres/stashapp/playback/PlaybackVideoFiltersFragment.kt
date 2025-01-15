package com.github.damontecres.stashapp.playback

import android.os.Bundle
import android.view.View
import android.widget.Button
import android.widget.SeekBar
import android.widget.SeekBar.OnSeekBarChangeListener
import android.widget.TextView
import androidx.fragment.app.Fragment
import androidx.fragment.app.viewModels
import androidx.preference.PreferenceManager
import com.github.damontecres.stashapp.R
import com.github.damontecres.stashapp.data.DataType
import com.github.damontecres.stashapp.data.VideoFilter
import com.github.damontecres.stashapp.views.StashOnFocusChangeListener

/**
 * Display the [VideoFilter] options to manipulate
 */
class PlaybackVideoFiltersFragment : Fragment(R.layout.apply_video_filters) {
    private val viewModel: VideoFilterViewModel by viewModels(ownerProducer = { requireParentFragment() })

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
        val blurAdjust = view.findViewById<SeekBar>(R.id.blur_adjust)

        val redText = view.findViewById<TextView>(R.id.red_adjust_text)
        val greenText = view.findViewById<TextView>(R.id.green_adjust_text)
        val blueText = view.findViewById<TextView>(R.id.blue_adjust_text)
        val brightnessText = view.findViewById<TextView>(R.id.brightness_adjust_text)
        val contrastText = view.findViewById<TextView>(R.id.contrast_adjust_text)
        val saturationText = view.findViewById<TextView>(R.id.saturation_adjust_text)
        val hueText = view.findViewById<TextView>(R.id.hue_adjust_text)
        val blurText = view.findViewById<TextView>(R.id.blur_adjust_text)

        fun setUi(vf: VideoFilter) {
            redAdjust.progress = vf.red
            greenAdjust.progress = vf.green
            blueAdjust.progress = vf.blue
            brightnessAdjust.progress = vf.brightness
            contrastAdjust.progress = vf.contrast
            saturationAdjust.progress = vf.saturation
            hueAdjust.progress = vf.hue
            blurAdjust.progress = vf.blur

            redText.text = getString(R.string.format_percent, vf.red - 100)
            greenText.text = getString(R.string.format_percent, vf.green - 100)
            blueText.text = getString(R.string.format_percent, vf.blue - 100)
            brightnessText.text = getString(R.string.format_percent, vf.brightness)
            contrastText.text = getString(R.string.format_percent, vf.contrast)
            saturationText.text = getString(R.string.format_percent, vf.saturation)
            hueText.text = getString(R.string.format_degrees, vf.hue)
            blurText.text = getString(R.string.format_pixels, vf.blur / 10f)
        }
        setUi(viewModel.videoFilter.value ?: VideoFilter())

        redAdjust.setOnSeekBarChangeListener {
            viewModel.videoFilter.value = getOrCreateVideoFilter().copy(red = it)
            redText.text = getString(R.string.format_percent, it - 100)
        }
        greenAdjust.setOnSeekBarChangeListener {
            viewModel.videoFilter.value = getOrCreateVideoFilter().copy(green = it)
            greenText.text = getString(R.string.format_percent, it - 100)
        }
        blueAdjust.setOnSeekBarChangeListener {
            viewModel.videoFilter.value = getOrCreateVideoFilter().copy(blue = it)
            blueText.text = getString(R.string.format_percent, it - 100)
        }
        brightnessAdjust.setOnSeekBarChangeListener {
            viewModel.videoFilter.value = getOrCreateVideoFilter().copy(brightness = it)
            brightnessText.text = getString(R.string.format_percent, it)
        }
        contrastAdjust.setOnSeekBarChangeListener {
            viewModel.videoFilter.value = getOrCreateVideoFilter().copy(contrast = it)
            contrastText.text = getString(R.string.format_percent, it)
        }
        saturationAdjust.setOnSeekBarChangeListener {
            viewModel.videoFilter.value = getOrCreateVideoFilter().copy(saturation = it)
            saturationText.text = getString(R.string.format_percent, it)
        }
        hueAdjust.setOnSeekBarChangeListener {
            viewModel.videoFilter.value = getOrCreateVideoFilter().copy(hue = it)
            hueText.text = getString(R.string.format_degrees, it)
        }
        blurAdjust.setOnSeekBarChangeListener {
            viewModel.videoFilter.value = getOrCreateVideoFilter().copy(blur = it)
            blurText.text = getString(R.string.format_pixels, it / 10f)
        }

        val onFocusChangeListener = StashOnFocusChangeListener(requireContext())
        val rotateLeftButton = view.findViewById<Button>(R.id.rotate_left_button)
        val rotateRightButton = view.findViewById<Button>(R.id.rotate_right_button)
        rotateLeftButton.onFocusChangeListener = onFocusChangeListener
        rotateLeftButton.setOnClickListener {
            val vf = getOrCreateVideoFilter()
            viewModel.videoFilter.value = vf.copy(rotation = vf.rotation + 90)
        }
        rotateRightButton.onFocusChangeListener = onFocusChangeListener
        rotateRightButton.setOnClickListener {
            val vf = getOrCreateVideoFilter()
            viewModel.videoFilter.value = vf.copy(rotation = vf.rotation - 90)
        }
        val saveButton = view.findViewById<Button>(R.id.save_button)
        saveButton.onFocusChangeListener = onFocusChangeListener
        saveButton.setOnClickListener {
            viewModel.maybeSaveFilter()
        }
        val saveFilters =
            PreferenceManager.getDefaultSharedPreferences(requireContext()).getBoolean(
                getString(R.string.pref_key_playback_save_effects),
                true,
            )
        if (!saveFilters) {
            saveButton.visibility = View.GONE
        }

        val resetButton = view.findViewById<Button>(R.id.reset_button)
        resetButton.onFocusChangeListener = onFocusChangeListener
        resetButton.setOnClickListener {
            val vf = VideoFilter()
            viewModel.videoFilter.value = vf
            setUi(vf)
        }

        if (viewModel.dataType == DataType.IMAGE) {
            view.findViewById<View>(R.id.hue_row).visibility = View.GONE
            view.findViewById<View>(R.id.blur_row).visibility = View.GONE
            rotateLeftButton.visibility = View.GONE
            rotateRightButton.visibility = View.GONE
        }
    }

    private fun getOrCreateVideoFilter(): VideoFilter = viewModel.videoFilter.value ?: VideoFilter()

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
