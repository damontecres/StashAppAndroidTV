package com.github.damontecres.stashapp

import android.content.Intent
import android.os.Bundle
import android.view.View
import android.widget.Toast
import androidx.leanback.preference.LeanbackPreferenceFragmentCompat
import androidx.lifecycle.lifecycleScope
import androidx.preference.Preference
import androidx.preference.PreferenceManager
import androidx.preference.SeekBarPreference
import androidx.preference.SwitchPreference
import coil3.SingletonImageLoader
import coil3.annotation.DelicateCoilApi
import com.github.damontecres.stashapp.navigation.Destination
import com.github.damontecres.stashapp.util.StashCoroutineExceptionHandler
import com.github.damontecres.stashapp.util.composeEnabled
import com.github.damontecres.stashapp.views.dialog.ConfirmationDialogFragment
import kotlinx.coroutines.launch
import java.util.Locale

class SettingsUiFragment : LeanbackPreferenceFragmentCompat() {
    @OptIn(DelicateCoilApi::class)
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

        val composeEnabledPref =
            findPreference<SwitchPreference>(getString(R.string.pref_key_use_compose_ui))!!
        composeEnabledPref.setOnPreferenceChangeListener { preference, newValue ->
            if (newValue == true) {
                ConfirmationDialogFragment.show(
                    childFragmentManager,
                    "The new UI is still in beta! Do you want to try it?\n\nNote: the app will restart.",
                ) {
                    viewLifecycleOwner.lifecycleScope.launch(StashCoroutineExceptionHandler()) {
                        SettingsFragment.clearCaches(requireContext())
                        composeEnabledPref.isChecked = true
                        // Clear coil singleton
                        SingletonImageLoader.reset()
                        requireActivity().startActivity(
                            Intent(
                                requireActivity(),
                                RootActivity::class.java,
                            ).setFlags(Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TASK),
                        )
                    }
                }
            } else if (newValue == false) {
                ConfirmationDialogFragment.show(
                    childFragmentManager,
                    "Please report any issues you encountered with the new UI!\n\nDo you want switch back to the old UI?\n\nNote: the app will restart.",
                ) {
                    viewLifecycleOwner.lifecycleScope.launch(StashCoroutineExceptionHandler()) {
                        SettingsFragment.clearCaches(requireContext())
                        composeEnabledPref.isChecked = false
                        // Clear coil singleton
                        SingletonImageLoader.reset()
                        requireActivity().startActivity(
                            Intent(
                                requireActivity(),
                                RootActivity::class.java,
                            ).setFlags(Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TASK),
                        )
                    }
                }
            }
            false
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
            chooseThemePref?.setOnPreferenceClickListener {
                Toast
                    .makeText(
                        requireContext(),
                        "Must enable compose to change this",
                        Toast.LENGTH_SHORT,
                    ).show()
                true
            }
        }
    }

    override fun onViewCreated(
        view: View,
        savedInstanceState: Bundle?,
    ) {
        super.onViewCreated(view, savedInstanceState)
        setTitle(getString(R.string.ui_settings))
        view.requestFocus()
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
