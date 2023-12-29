package com.github.damontecres.stashapp

import android.os.Bundle
import android.widget.Toast
import androidx.fragment.app.Fragment
import androidx.leanback.preference.LeanbackPreferenceFragmentCompat
import androidx.leanback.preference.LeanbackSettingsFragmentCompat
import androidx.lifecycle.lifecycleScope
import androidx.preference.EditTextPreference
import androidx.preference.Preference
import androidx.preference.PreferenceDialogFragmentCompat
import androidx.preference.PreferenceFragmentCompat
import androidx.preference.PreferenceScreen
import com.apollographql.apollo3.exception.ApolloException
import com.apollographql.apollo3.exception.ApolloHttpException
import com.github.damontecres.stashapp.api.ServerInfoQuery
import kotlinx.coroutines.launch


class SettingsFragment : LeanbackSettingsFragmentCompat() {

    override fun onPreferenceStartInitialScreen() {
        startPreferenceFragment(PreferencesFragment())
    }

    override fun onPreferenceStartFragment(
        caller: PreferenceFragmentCompat,
        pref: Preference
    ): Boolean {
        val args: Bundle = pref.getExtras()
        val f: Fragment = childFragmentManager.fragmentFactory.instantiate(
            requireActivity().classLoader, pref.getFragment()!!
        )
        f.setArguments(args)
        f.setTargetFragment(caller, 0)
        if (f is PreferenceFragmentCompat
            || f is PreferenceDialogFragmentCompat
        ) {
            startPreferenceFragment(f)
        } else {
            startImmersiveFragment(f)
        }
        return true
    }

    override fun onPreferenceStartScreen(
        caller: PreferenceFragmentCompat,
        pref: PreferenceScreen
    ): Boolean {
        val fragment: Fragment = PreferencesFragment()
        val args = Bundle(1)
        args.putString(PreferenceFragmentCompat.ARG_PREFERENCE_ROOT, pref.getKey())
        fragment.setArguments(args)
        startPreferenceFragment(fragment)
        return true
    }
}

class PreferencesFragment : LeanbackPreferenceFragmentCompat() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
    }

    override fun onCreatePreferences(savedInstanceState: Bundle?, rootKey: String?) {
        setPreferencesFromResource(R.xml.root_preferences, rootKey)

        findPreference<Preference>("testStashServer")?.setOnPreferenceClickListener {
            viewLifecycleOwner.lifecycleScope.launch {
                testStashConnection(requireContext(), true)
            }
            true
        }

        val apiKayPref = findPreference<EditTextPreference>("stashApiKey")
        apiKayPref?.summaryProvider = object : Preference.SummaryProvider<EditTextPreference> {
            override fun provideSummary(preference: EditTextPreference): CharSequence? {
                return if (preference.text.isNullOrBlank()) {
                    "No API key configured"
                } else {
                    "API Key is configured"
                }
            }
        }

    }
}