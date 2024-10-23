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
    }

    private fun setVideoDelaySummary(
        pref: SeekBarPreference,
        value: Any,
    ) {
        val newValue = value.toString().toInt()
        pref.summary =
            if (newValue > 0) {
                String.format(
                    Locale.getDefault(),
                    "%.1f %s",
                    newValue / 1000.0,
                    getString(R.string.stashapp_seconds),
                )
            } else {
                "No delay"
            }
    }
}
