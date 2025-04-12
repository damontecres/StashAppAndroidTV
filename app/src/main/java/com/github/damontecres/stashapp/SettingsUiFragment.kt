package com.github.damontecres.stashapp

import android.os.Bundle
import android.widget.Toast
import androidx.leanback.preference.LeanbackPreferenceFragmentCompat
import androidx.preference.Preference
import androidx.preference.PreferenceManager
import androidx.preference.SeekBarPreference
import com.github.damontecres.stashapp.navigation.Destination
import com.github.damontecres.stashapp.util.composeEnabled
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

        val chooseThemePref =
            findPreference<Preference>(getString(R.string.pref_key_ui_theme_file))
        chooseThemePref?.summary =
            PreferenceManager.getDefaultSharedPreferences(requireContext()).getString(
                getString(R.string.pref_key_ui_theme_file),
                "default",
            )
        if (composeEnabled(requireContext())) {
            chooseThemePref?.setOnPreferenceClickListener {
                StashApplication.navigationManager.navigate(Destination.ChooseTheme)
                true
            }
        } else {
            Toast
                .makeText(
                    requireContext(),
                    "Must enable compose to change this",
                    Toast.LENGTH_SHORT,
                ).show()
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
