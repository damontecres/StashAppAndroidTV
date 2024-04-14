package com.github.damontecres.stashapp

import android.content.Intent
import android.os.Bundle
import android.text.InputType
import android.util.Log
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.EditText
import android.widget.Toast
import androidx.core.content.edit
import androidx.fragment.app.Fragment
import androidx.leanback.app.GuidedStepSupportFragment
import androidx.leanback.preference.LeanbackEditTextPreferenceDialogFragmentCompat
import androidx.leanback.preference.LeanbackPreferenceFragmentCompat
import androidx.leanback.preference.LeanbackSettingsFragmentCompat
import androidx.lifecycle.lifecycleScope
import androidx.preference.EditTextPreference
import androidx.preference.ListPreference
import androidx.preference.Preference
import androidx.preference.PreferenceDialogFragmentCompat
import androidx.preference.PreferenceFragmentCompat
import androidx.preference.PreferenceManager
import androidx.preference.PreferenceScreen
import androidx.preference.SeekBarPreference
import com.bumptech.glide.Glide
import com.bumptech.glide.load.engine.cache.DiskCache
import com.github.damontecres.stashapp.util.Constants
import com.github.damontecres.stashapp.util.LongClickPreference
import com.github.damontecres.stashapp.util.MutationEngine
import com.github.damontecres.stashapp.util.ServerPreferences
import com.github.damontecres.stashapp.util.StashCoroutineExceptionHandler
import com.github.damontecres.stashapp.util.UpdateChecker
import com.github.damontecres.stashapp.util.cacheDurationPrefToDuration
import com.github.damontecres.stashapp.util.configureHttpsTrust
import com.github.damontecres.stashapp.util.testStashConnection
import kotlinx.coroutines.CoroutineExceptionHandler
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import okhttp3.Cache
import java.io.File

class SettingsFragment : LeanbackSettingsFragmentCompat() {
    override fun onPreferenceStartInitialScreen() {
        startPreferenceFragment(PreferencesFragment(::startPreferenceFragment))
    }

    override fun onPreferenceStartFragment(
        caller: PreferenceFragmentCompat,
        pref: Preference,
    ): Boolean {
        val args: Bundle = pref.extras
        val f: Fragment =
            childFragmentManager.fragmentFactory.instantiate(
                requireActivity().classLoader,
                pref.fragment!!,
            )
        f.arguments = args
        f.setTargetFragment(caller, 0)
        if (f is PreferenceFragmentCompat ||
            f is PreferenceDialogFragmentCompat
        ) {
            startPreferenceFragment(f)
        } else {
            startImmersiveFragment(f)
        }
        return true
    }

    override fun onPreferenceStartScreen(
        caller: PreferenceFragmentCompat,
        pref: PreferenceScreen,
    ): Boolean {
        val fragment: Fragment = PreferencesFragment(::startPreferenceFragment)
        val args = Bundle(1)
        args.putString(PreferenceFragmentCompat.ARG_PREFERENCE_ROOT, pref.key)
        fragment.arguments = args
        startPreferenceFragment(fragment)
        return true
    }

    override fun onPreferenceDisplayDialog(
        caller: PreferenceFragmentCompat,
        pref: Preference,
    ): Boolean {
        return if (pref.key == getString(R.string.pref_key_pin_code)) {
            val f = NumberEditTextPreferencesDialog(pref.key)
            f.setTargetFragment(caller, 0)
            startPreferenceFragment(f)
            true
        } else {
            super.onPreferenceDisplayDialog(caller, pref)
        }
    }

    class PreferencesFragment(val startPreferenceFragmentFunc: (fragment: LeanbackPreferenceFragmentCompat) -> Unit) :
        LeanbackPreferenceFragmentCompat() {
        private var serverKeys = listOf<String>()
        private var serverValues = listOf<String>()

        override fun onCreatePreferences(
            savedInstanceState: Bundle?,
            rootKey: String?,
        ) {
            setPreferencesFromResource(R.xml.root_preferences, rootKey)

            val manager = PreferenceManager.getDefaultSharedPreferences(requireContext())

            val pinCodePref = findPreference<EditTextPreference>("pinCode")!!
            pinCodePref.summaryProvider =
                Preference.SummaryProvider<EditTextPreference> { preference ->
                    if (preference.text.isNullOrBlank()) {
                        "No PIN is set"
                    } else {
                        "PIN is set"
                    }
                }

            val installedVersion = UpdateChecker.getInstalledVersion(requireActivity())
            val versionPref = findPreference<Preference>("versionName")!!
            versionPref.summary = installedVersion.toString()
            var clickCount = 0
            versionPref.setOnPreferenceClickListener {
                if (clickCount > 2) {
                    clickCount = 0
                    startActivity(Intent(requireContext(), DebugActivity::class.java))
                } else {
                    clickCount++
                }
                true
            }

            val checkForUpdatePref = findPreference<LongClickPreference>("checkForUpdate")!!
            checkForUpdatePref.setOnPreferenceClickListener {
                viewLifecycleOwner.lifecycleScope.launch(StashCoroutineExceptionHandler()) {
                    val release = UpdateChecker.getLatestRelease(requireContext())
                    if (release != null) {
                        if (release.version.isGreaterThan(installedVersion)) {
                            GuidedStepSupportFragment.add(
                                requireActivity().supportFragmentManager,
                                UpdateAppFragment(release),
                            )
                        } else {
                            Toast.makeText(
                                requireContext(),
                                "No update available!",
                                Toast.LENGTH_SHORT,
                            ).show()
                        }
                    } else {
                        Toast.makeText(
                            requireContext(),
                            "Failed to check for updates",
                            Toast.LENGTH_LONG,
                        ).show()
                    }
                }
                true
            }
            checkForUpdatePref.setOnLongClickListener {
                viewLifecycleOwner.lifecycleScope.launch(StashCoroutineExceptionHandler()) {
                    val release = UpdateChecker.getLatestRelease(requireContext())
                    if (release != null) {
                        GuidedStepSupportFragment.add(
                            requireActivity().supportFragmentManager,
                            UpdateAppFragment(release),
                        )
                    } else {
                        Toast.makeText(
                            requireContext(),
                            "Failed to check for updates",
                            Toast.LENGTH_LONG,
                        ).show()
                    }
                }
                true
            }

            findPreference<Preference>("testStashServer")!!
                .setOnPreferenceClickListener {
                    viewLifecycleOwner.lifecycleScope.launch(StashCoroutineExceptionHandler()) {
                        testStashConnection(requireContext(), true)
                    }
                    true
                }

            val urlPref = findPreference<EditTextPreference>("stashUrl")!!

            val apiKayPref = findPreference<EditTextPreference>("stashApiKey")!!
            apiKayPref.summaryProvider =
                Preference.SummaryProvider<EditTextPreference> { preference ->
                    if (preference.text.isNullOrBlank()) {
                        "No API key configured"
                    } else {
                        "API Key is configured"
                    }
                }

            val triggerExceptionHandler =
                CoroutineExceptionHandler { _, ex ->
                    Log.e(TAG, "Error during trigger", ex)
                    Toast.makeText(
                        requireContext(),
                        "Error trying to trigger a task: ${ex.message}",
                        Toast.LENGTH_LONG,
                    ).show()
                }

            findPreference<Preference>("triggerScan")!!
                .setOnPreferenceClickListener {
                    viewLifecycleOwner.lifecycleScope.launch(triggerExceptionHandler) {
                        MutationEngine(requireContext()).triggerScan()
                        Toast.makeText(
                            requireContext(),
                            "Triggered a default library scan",
                            Toast.LENGTH_LONG,
                        ).show()
                    }
                    true
                }

            findPreference<Preference>("triggerGenerate")!!
                .setOnPreferenceClickListener {
                    viewLifecycleOwner.lifecycleScope.launch(triggerExceptionHandler) {
                        MutationEngine(requireContext()).triggerGenerate()
                        Toast.makeText(
                            requireContext(),
                            "Triggered a default generate",
                            Toast.LENGTH_LONG,
                        ).show()
                    }
                    true
                }

            setServers()
            val chooseServer = findPreference<ListPreference>("chooseStashServer")!!
            chooseServer.entries = serverKeys.toTypedArray()
            chooseServer.entryValues = serverValues.toTypedArray()
            chooseServer.setOnPreferenceClickListener {
                setServers()
                chooseServer.entries = serverKeys.toTypedArray()
                chooseServer.entryValues = serverValues.toTypedArray()
                if (serverKeys.isEmpty()) {
                    Toast.makeText(requireContext(), "No other servers defined", Toast.LENGTH_SHORT)
                        .show()
                    true
                } else {
                    false
                }
            }
            chooseServer.setOnPreferenceChangeListener { preference: Preference, newValue: Any ->
                val currentUrl = urlPref?.text
                val currentApiKey = apiKayPref?.text
                if (!currentUrl.isNullOrBlank()) {
                    manager.edit(true) {
                        putString(SERVER_PREF_PREFIX + currentUrl, currentUrl)
                        putString(SERVER_APIKEY_PREF_PREFIX + currentUrl, currentApiKey)
                    }
                }

                val serverKey = newValue.toString()
                val apiKeyKey = serverKey.replace(SERVER_PREF_PREFIX, SERVER_APIKEY_PREF_PREFIX)

                val server = manager.getString(serverKey, null)
                val apiKey = manager.getString(apiKeyKey, null)
                urlPref.text = server
                apiKayPref.text = apiKey

                false
            }

            val newServer = findPreference<Preference>("newStashServer")!!
            newServer.setOnPreferenceClickListener {
                val url = urlPref.text
                val apiKey = apiKayPref.text
                if (url.isNullOrBlank()) {
                    Toast.makeText(
                        requireContext(),
                        "Enter URL before adding a new one",
                        Toast.LENGTH_LONG,
                    ).show()
                } else {
                    manager.edit(true) {
                        putString(SERVER_PREF_PREFIX + url, url)
                        putString(SERVER_APIKEY_PREF_PREFIX + url, apiKey)
                    }

                    urlPref.text = null
                    apiKayPref.text = null
                    setServers()
                    Toast.makeText(
                        requireContext(),
                        "Enter details above",
                        Toast.LENGTH_LONG,
                    ).show()
                }
                true
            }

            val removeServer = findPreference<ListPreference>("deleteStashServer")!!
            removeServer.entries = serverKeys.toTypedArray()
            removeServer.entryValues = serverValues.toTypedArray()
            removeServer.setOnPreferenceClickListener {
                setServers()
                removeServer.entries = serverKeys.toTypedArray()
                removeServer.entryValues = serverValues.toTypedArray()
                if (serverKeys.isEmpty()) {
                    Toast.makeText(requireContext(), "No servers defined", Toast.LENGTH_SHORT)
                        .show()
                    true
                } else {
                    false
                }
            }
            removeServer.setOnPreferenceChangeListener { preference, newValue ->
                val key = newValue.toString()
                manager.edit(true) {
                    val apiKeyKey = key.replace(SERVER_PREF_PREFIX, SERVER_APIKEY_PREF_PREFIX)
                    remove(key)
                    remove(apiKeyKey)
                }
                val url = key.replace(SERVER_PREF_PREFIX, "")
                if (url == urlPref.text) {
                    urlPref.text = null
                    apiKayPref.text = null
                }
                setServers()
                false
            }

            findPreference<SeekBarPreference>("skip_back_time")!!.min = 5
            findPreference<SeekBarPreference>("skip_forward_time")!!.min = 5

            val advancedPreferences = findPreference<Preference>("advancedPreferences")!!
            advancedPreferences.setOnPreferenceClickListener {
                startPreferenceFragmentFunc(AdvancedPreferencesFragment())
                true
            }
        }

        override fun onViewCreated(
            view: View,
            savedInstanceState: Bundle?,
        ) {
            super.onViewCreated(view, savedInstanceState)

            if (PreferenceManager.getDefaultSharedPreferences(requireContext())
                    .getBoolean("autoCheckForUpdates", true)
            ) {
                val checkForUpdatePref = findPreference<Preference>("checkForUpdate")
                viewLifecycleOwner.lifecycleScope.launch(StashCoroutineExceptionHandler()) {
                    val release = UpdateChecker.getLatestRelease(requireContext())
                    val installedVersion = UpdateChecker.getInstalledVersion(requireActivity())
                    if (release != null) {
                        if (release.version.isGreaterThan(installedVersion)) {
                            checkForUpdatePref?.title = "Install update"
                            checkForUpdatePref?.summary =
                                getString(R.string.stashapp_package_manager_latest_version) + ": ${release.version}"
                        } else {
                            checkForUpdatePref?.title = "No update available"
                            checkForUpdatePref?.summary = null
                        }
                    }
                }
            }
        }

        override fun onResume() {
            super.onResume()
            viewLifecycleOwner.lifecycleScope.launch(StashCoroutineExceptionHandler()) {
                ServerPreferences(requireContext()).updatePreferences()
            }
        }

        override fun onStop() {
            super.onStop()
            val url = findPreference<EditTextPreference>("stashUrl")!!.text
            val apiKey = findPreference<EditTextPreference>("stashApiKey")!!.text
            if (!url.isNullOrBlank()) {
                PreferenceManager.getDefaultSharedPreferences(requireContext()).edit(true) {
                    putString(SERVER_PREF_PREFIX + url, url)
                    putString(SERVER_APIKEY_PREF_PREFIX + url, apiKey)
                }
            }
        }

        private fun setServers() {
            val manager = PreferenceManager.getDefaultSharedPreferences(requireContext())
            val keys =
                manager.all.keys.filter { it.startsWith(SERVER_PREF_PREFIX) }.sorted().toList()
            val values = keys.map { manager.all[it].toString() }.toList()

            serverKeys = values
            serverValues = keys
        }

        companion object {
            const val TAG = "SettingsFragment"

            private const val SERVER_PREF_PREFIX = "server_"
            private const val SERVER_APIKEY_PREF_PREFIX = "apikey_"
        }
    }

    class AdvancedPreferencesFragment : LeanbackPreferenceFragmentCompat() {
        override fun onCreatePreferences(
            savedInstanceState: Bundle?,
            rootKey: String?,
        ) {
            setPreferencesFromResource(R.xml.advanced_preferences, rootKey)

            findPreference<SeekBarPreference>("maxSearchResults")!!.min = 5
            findPreference<SeekBarPreference>("searchDelay")!!.min = 50

            val cacheSizePref = findPreference<SeekBarPreference>("networkCacheSize")!!
            cacheSizePref.min = 25
            val cache = Constants.getNetworkCache(requireContext())
            setUsedCachedSummary(cacheSizePref, cache)

            findPreference<Preference>("clearCache")!!.setOnPreferenceClickListener {
                cache.evictAll()
                viewLifecycleOwner.lifecycleScope.launch(StashCoroutineExceptionHandler()) {
                    withContext(Dispatchers.IO) {
                        Glide.get(requireContext()).clearDiskCache()
                    }
                    setUsedCachedSummary(cacheSizePref, cache)
                }
                true
            }

            val cacheDurationPref = findPreference<SeekBarPreference>("networkCacheDuration")!!
            setCacheDurationSummary(cacheDurationPref, cacheDurationPref.value)
            cacheDurationPref.setOnPreferenceChangeListener { _, newValue ->
                setCacheDurationSummary(cacheDurationPref, newValue)
                true
            }

            findPreference<Preference>("license")!!.setOnPreferenceClickListener {
                startActivity(Intent(requireContext(), LicenseActivity::class.java))
                true
            }

            findPreference<Preference>("trustAllCerts")!!.setOnPreferenceChangeListener { _, newValue ->
                val app = requireActivity().application as StashApplication
                configureHttpsTrust(app, newValue as Boolean)
                true
            }
        }

        private fun setCacheDurationSummary(
            pref: SeekBarPreference,
            value: Any,
        ) {
            val duration = cacheDurationPrefToDuration(value.toString().toInt())
            pref.summary =
                if (duration != null) {
                    duration.toString()
                } else {
                    "Always request from server"
                }
        }

        private fun setUsedCachedSummary(
            cacheSizePref: Preference,
            cache: Cache,
        ) {
            val cacheSize = cache.size() / 1024.0 / 1024
            val glideCacheSize =
                File(
                    requireContext().cacheDir,
                    DiskCache.Factory.DEFAULT_DISK_CACHE_DIR,
                ).walkTopDown()
                    .filter { it.isFile }.map { it.length() }.sum() / 1024.0 / 1024.0
            val cacheSizeFormatted = String.format("%.2f", cacheSize)
            val glideCacheSizeFormatted = String.format("%.2f", glideCacheSize)

            cacheSizePref.summary =
                "Using $cacheSizeFormatted MB (Images $glideCacheSizeFormatted MB)"
        }
    }

    class NumberEditTextPreferencesDialog(key: String) :
        LeanbackEditTextPreferenceDialogFragmentCompat() {
        init {
            val args = Bundle(1)
            args.putString(ARG_KEY, key)
            arguments = args
        }

        override fun onCreateView(
            inflater: LayoutInflater,
            container: ViewGroup?,
            savedInstanceState: Bundle?,
        ): View? {
            val root = super.onCreateView(inflater, container, savedInstanceState)
            val editTextView = root?.findViewById<EditText>(android.R.id.edit)
            editTextView?.inputType = InputType.TYPE_CLASS_NUMBER
            return root
        }
    }
}
