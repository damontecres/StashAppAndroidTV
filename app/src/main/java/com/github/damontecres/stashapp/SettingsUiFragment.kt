package com.github.damontecres.stashapp

import android.os.Bundle
import androidx.leanback.preference.LeanbackPreferenceFragmentCompat

class SettingsUiFragment : LeanbackPreferenceFragmentCompat() {
    override fun onCreatePreferences(
        savedInstanceState: Bundle?,
        rootKey: String?,
    ) {
        setPreferencesFromResource(R.xml.ui_preferences, rootKey)
    }
}
