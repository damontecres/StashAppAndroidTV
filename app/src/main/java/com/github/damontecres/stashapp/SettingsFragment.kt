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
import androidx.fragment.app.Fragment
import androidx.fragment.app.activityViewModels
import androidx.leanback.app.GuidedStepSupportFragment
import androidx.leanback.preference.LeanbackEditTextPreferenceDialogFragmentCompat
import androidx.leanback.preference.LeanbackPreferenceFragmentCompat
import androidx.leanback.preference.LeanbackSettingsFragmentCompat
import androidx.lifecycle.lifecycleScope
import androidx.preference.EditTextPreference
import androidx.preference.Preference
import androidx.preference.PreferenceDialogFragmentCompat
import androidx.preference.PreferenceFragmentCompat
import androidx.preference.PreferenceManager
import androidx.preference.PreferenceScreen
import androidx.preference.SeekBarPreference
import androidx.preference.SwitchPreference
import androidx.room.Room
import com.bumptech.glide.Glide
import com.bumptech.glide.load.engine.cache.DiskCache
import com.github.damontecres.stashapp.data.JobResult
import com.github.damontecres.stashapp.data.room.AppDatabase
import com.github.damontecres.stashapp.playback.PlaybackSceneFragment.Companion.DB_NAME
import com.github.damontecres.stashapp.setup.ManageServersFragment
import com.github.damontecres.stashapp.util.Constants
import com.github.damontecres.stashapp.util.LongClickPreference
import com.github.damontecres.stashapp.util.MutationEngine
import com.github.damontecres.stashapp.util.QueryEngine
import com.github.damontecres.stashapp.util.ServerPreferences
import com.github.damontecres.stashapp.util.StashClient
import com.github.damontecres.stashapp.util.StashCoroutineExceptionHandler
import com.github.damontecres.stashapp.util.StashServer
import com.github.damontecres.stashapp.util.UpdateChecker
import com.github.damontecres.stashapp.util.cacheDurationPrefToDuration
import com.github.damontecres.stashapp.util.launchIO
import com.github.damontecres.stashapp.util.plugin.CompanionPlugin
import com.github.damontecres.stashapp.util.testStashConnection
import com.github.damontecres.stashapp.views.models.ServerViewModel
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
        private val viewModel: ServerViewModel by activityViewModels()

        override fun onCreatePreferences(
            savedInstanceState: Bundle?,
            rootKey: String?,
        ) {
            setPreferencesFromResource(R.xml.root_preferences, rootKey)

            findPreference<Preference>(PREF_STASH_URL)!!.setOnPreferenceClickListener {
                viewLifecycleOwner.lifecycleScope.launch(StashCoroutineExceptionHandler()) {
                    testStashConnection(
                        requireContext(),
                        true,
                        StashClient.getApolloClient(requireContext()),
                    )
                }
                true
            }

            val manageServers = findPreference<Preference>("manageServers")
            manageServers!!.setOnPreferenceClickListener {
                GuidedStepSupportFragment.add(
                    requireActivity().supportFragmentManager,
                    ManageServersFragment(),
                )
                true
            }

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
            val installUpdate = findPreference<LongClickPreference>("installUpdate")!!
            listOf(checkForUpdatePref, installUpdate).forEach { pref ->
                pref.setOnPreferenceClickListener {
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
                pref.setOnLongClickListener {
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

            viewModel.currentServer.observe(viewLifecycleOwner) {
                refresh(it)
            }

            if (PreferenceManager.getDefaultSharedPreferences(requireContext())
                    .getBoolean("autoCheckForUpdates", true)
            ) {
                val checkForUpdatePref = findPreference<Preference>("checkForUpdate")
                val installUpdate = findPreference<Preference>("installUpdate")
                val updateCategory = findPreference<Preference>("updateCategory")

                val updatePrefs = listOf(checkForUpdatePref, installUpdate)

                viewLifecycleOwner.lifecycleScope.launch(StashCoroutineExceptionHandler()) {
                    val release = UpdateChecker.getLatestRelease(requireContext())
                    val installedVersion = UpdateChecker.getInstalledVersion(requireActivity())
                    if (release != null) {
                        if (release.version.isGreaterThan(installedVersion)) {
                            updatePrefs.forEach {
                                it?.title = "Install update"
                                it?.summary =
                                    getString(R.string.stashapp_package_manager_latest_version) + ": ${release.version}"
                            }
                            updateCategory?.isVisible = true
                        } else {
                            updatePrefs.forEach {
                                it?.title = "No update available"
                                it?.summary = null
                            }
                            updateCategory?.isVisible = false
                        }
                    }
                }
            }
        }

        private fun refresh(currentServer: StashServer?) {
            Log.v(TAG, "refresh")
            findPreference<Preference>(PREF_STASH_URL)!!.summary = currentServer?.url

            val sendLogsPref = findPreference<LongClickPreference>("sendLogs")!!
            sendLogsPref.title = "Checking for companion plugin..."
            sendLogsPref.summary = null

            val setupSendLogsPref = {
                sendLogsPref.title = "Send Logs"
                sendLogsPref.summary = "Send a copy of recent app logs to your current server"
                sendLogsPref.setOnPreferenceClickListener {
                    viewLifecycleOwner.lifecycleScope.launch(StashCoroutineExceptionHandler()) {
                        CompanionPlugin.sendLogCat(requireContext(), false)
                    }
                    true
                }
                sendLogsPref.setOnLongClickListener {
                    viewLifecycleOwner.lifecycleScope.launch(StashCoroutineExceptionHandler()) {
                        CompanionPlugin.sendLogCat(requireContext(), true)
                    }
                    true
                }
            }

            viewLifecycleOwner.lifecycleScope.launch(StashCoroutineExceptionHandler()) {
                val serverPrefs = ServerPreferences(requireContext()).updatePreferences()
                if (serverPrefs.companionPluginInstalled) {
                    setupSendLogsPref()
                } else {
                    sendLogsPref.title = "Install companion plugin"
                    sendLogsPref.summary =
                        "Install StashAppAndroid TV Companion plugin on the current server"
                    sendLogsPref.setOnPreferenceClickListener {
                        viewLifecycleOwner.lifecycleScope.launch(Dispatchers.IO + StashCoroutineExceptionHandler()) {
                            val jobId =
                                CompanionPlugin.installPlugin(MutationEngine(requireContext()))
                            val queryEngine = QueryEngine(requireContext())
                            val result = queryEngine.waitForJob(jobId)
                            withContext(Dispatchers.Main) {
                                if (result is JobResult.Failure) {
                                    Toast.makeText(
                                        requireContext(),
                                        "Error installing plugin: ${result.message}",
                                        Toast.LENGTH_LONG,
                                    ).show()
                                } else if (result is JobResult.NotFound) {
                                    Toast.makeText(
                                        requireContext(),
                                        "Error installing plugin",
                                        Toast.LENGTH_LONG,
                                    ).show()
                                } else {
                                    val serverPrefs =
                                        ServerPreferences(requireContext()).updatePreferences()
                                    if (serverPrefs.companionPluginInstalled) {
                                        Toast.makeText(
                                            requireContext(),
                                            "Companion plugin installed!",
                                            Toast.LENGTH_SHORT,
                                        ).show()
                                        setupSendLogsPref()
                                    } else {
                                        Toast.makeText(
                                            requireContext(),
                                            "Error installing plugin",
                                            Toast.LENGTH_LONG,
                                        ).show()
                                    }
                                }
                            }
                        }
                        true
                    }
                }
            }
        }

        companion object {
            private const val TAG = "SettingsFragment"

            const val SERVER_PREF_PREFIX = "server_"
            const val SERVER_APIKEY_PREF_PREFIX = "apikey_"
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

            val networkTimeoutPref = findPreference<Preference>("networkTimeout")!!
            networkTimeoutPref.setOnPreferenceClickListener {
                viewLifecycleOwner.lifecycleScope.launch(StashCoroutineExceptionHandler()) {
                    StashClient.invalidate()
                    testStashConnection(
                        requireContext(),
                        true,
                        StashClient.getApolloClient(requireContext()),
                    )
                }
                true
            }
            networkTimeoutPref.setOnPreferenceChangeListener { _, newValue ->
                if (newValue == 0) {
                    Toast.makeText(
                        requireContext(),
                        "Warning! A zero network timeout will wait forever!",
                        Toast.LENGTH_LONG,
                    ).show()
                }
                true
            }

            val persistPlaybackEffectsPref =
                findPreference<SwitchPreference>(getString(R.string.pref_key_playback_save_effects))!!
            persistPlaybackEffectsPref.setOnPreferenceChangeListener { preference, newValue ->
                if (newValue == false) {
                    viewLifecycleOwner.lifecycleScope.launchIO {
                        val db =
                            Room.databaseBuilder(
                                StashApplication.getApplication(),
                                AppDatabase::class.java,
                                DB_NAME,
                            ).build()
                        db.playbackEffectsDao().deleteAll()
                    }
                }
                true
            }
        }

        private fun setCacheDurationSummary(
            pref: SeekBarPreference,
            value: Any,
        ) {
            val duration = cacheDurationPrefToDuration(value.toString().toInt())
            pref.summary = duration?.toString() ?: "Always request from server"
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

    companion object {
        const val PREF_STASH_URL = "stashUrl"
        const val PREF_STASH_API_KEY = "stashApiKey"
    }
}
