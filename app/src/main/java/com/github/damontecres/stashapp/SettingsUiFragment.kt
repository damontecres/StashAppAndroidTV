package com.github.damontecres.stashapp

import android.os.Bundle
import androidx.leanback.preference.LeanbackPreferenceFragmentCompat
import androidx.preference.SeekBarPreference
import java.util.Locale

class SettingsUiFragment : LeanbackPreferenceFragmentCompat() {
    override fun onCreatePreferences(
        savedInstanceState: Bundle?,
        rootKey: String?,
    ) {
        setPreferencesFromResource(R.xml.ui_preferences, rootKey)

        val videoDelayPref =
            findPreference<SeekBarPreference>(getString(R.string.pref_key_ui_card_overlay_delay))!!
        setVideoDelaySummary(videoDelayPref, videoDelayPref.value)
        videoDelayPref.setOnPreferenceChangeListener { _, newValue ->
            setVideoDelaySummary(videoDelayPref, newValue)
            true
        }

        val slideshowDurationPref =
            findPreference<SeekBarPreference>(getString(R.string.pref_key_slideshow_duration))!!
        slideshowDurationPref.min = 1

        val slideShowDurationPref =
            findPreference<SeekBarPreference>(getString(R.string.pref_key_slideshow_duration))!!
        setVideoDelaySummary(slideShowDurationPref, slideShowDurationPref.value * 1000, 0)
        slideShowDurationPref.setOnPreferenceChangeListener { _, newValue ->
            setVideoDelaySummary(slideShowDurationPref, newValue.toString().toInt() * 1000, 0)
            true
        }

        val imageClipDelayPref =
            findPreference<SeekBarPreference>(getString(R.string.pref_key_slideshow_duration_image_clip))!!
        setVideoDelaySummary(imageClipDelayPref, imageClipDelayPref.value)
        imageClipDelayPref.setOnPreferenceChangeListener { _, newValue ->
            setVideoDelaySummary(imageClipDelayPref, newValue, 2)
            true
        }
    }

    private fun setVideoDelaySummary(
        pref: SeekBarPreference,
        value: Any,
        decimals: Int = 1,
    ) {
        val newValue = value.toString().toInt()
        pref.summary =
            if (newValue > 0) {
                String.format(
                    Locale.getDefault(),
                    "%.${decimals}f %s",
                    newValue / 1000.0,
                    getString(R.string.stashapp_seconds),
                )
            } else {
                "No delay"
            }
    }
}
