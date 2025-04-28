package com.github.damontecres.stashapp

import android.annotation.SuppressLint
import android.content.Context
import android.content.DialogInterface
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
import androidx.fragment.app.activityViewModels
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
import coil3.SingletonImageLoader
import coil3.annotation.DelicateCoilApi
import coil3.imageLoader
import com.bumptech.glide.Glide
import com.bumptech.glide.load.engine.cache.DiskCache
import com.github.damontecres.stashapp.api.fragment.StashJob
import com.github.damontecres.stashapp.api.type.JobStatus
import com.github.damontecres.stashapp.api.type.JobStatusUpdateType
import com.github.damontecres.stashapp.data.JobResult
import com.github.damontecres.stashapp.navigation.Destination
import com.github.damontecres.stashapp.setup.readonly.ReadOnlyPinConfigFragment
import com.github.damontecres.stashapp.util.Constants
import com.github.damontecres.stashapp.util.LongClickPreference
import com.github.damontecres.stashapp.util.MutationEngine
import com.github.damontecres.stashapp.util.QueryEngine
import com.github.damontecres.stashapp.util.ServerPreferences
import com.github.damontecres.stashapp.util.StashCoroutineExceptionHandler
import com.github.damontecres.stashapp.util.StashServer
import com.github.damontecres.stashapp.util.SubscriptionEngine
import com.github.damontecres.stashapp.util.UpdateChecker
import com.github.damontecres.stashapp.util.cacheDurationPrefToDuration
import com.github.damontecres.stashapp.util.composeEnabled
import com.github.damontecres.stashapp.util.getDestination
import com.github.damontecres.stashapp.util.isNotNullOrBlank
import com.github.damontecres.stashapp.util.joinNotNullOrBlank
import com.github.damontecres.stashapp.util.launchIO
import com.github.damontecres.stashapp.util.plugin.CompanionPlugin
import com.github.damontecres.stashapp.util.testStashConnection
import com.github.damontecres.stashapp.views.dialog.ConfirmationDialogFragment
import com.github.damontecres.stashapp.views.formatBytes
import com.github.damontecres.stashapp.views.models.ServerViewModel
import io.noties.markwon.Markwon
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import kotlinx.serialization.Serializable
import okhttp3.Cache
import java.io.File

@Serializable
enum class PreferenceScreenOption {
    BASIC,
    ADVANCED,
    USER_INTERFACE,
    ;

    companion object {
        fun fromString(name: String?) = entries.firstOrNull { it.name == name } ?: BASIC
    }
}

class SettingsFragment : LeanbackSettingsFragmentCompat() {
    override fun onPreferenceStartInitialScreen() {
        val destination = requireArguments().getDestination<Destination.Settings>()
        // PREFERENCE_FRAGMENT_TAG is private, so hardcoded here
        val prevFragment =
            childFragmentManager
                .findFragmentByTag("androidx.leanback.preference.LeanbackSettingsFragment.PREFERENCE_FRAGMENT")
        // If the previous fragment was not a preference, then the current one should be, so do not start a new one
        if (prevFragment !is LeanbackPreferenceFragmentCompat) {
            startPreferenceFragment(
                PreferencesFragment(
                    ::startPreferenceFragment,
                    ::startImmersiveFragment,
                ),
            )
        }
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
        val fragment: Fragment =
            PreferencesFragment(::startPreferenceFragment, ::startImmersiveFragment)
        val args = Bundle(1)
        args.putString(PreferenceFragmentCompat.ARG_PREFERENCE_ROOT, pref.key)
        fragment.arguments = args
        startPreferenceFragment(fragment)
        return true
    }

    override fun onPreferenceDisplayDialog(
        caller: PreferenceFragmentCompat,
        pref: Preference,
    ): Boolean =
        if (pref.key == getString(R.string.pref_key_pin_code)) {
            val f = NumberEditTextPreferencesDialog(pref.key)
            f.setTargetFragment(caller, 0)
            startPreferenceFragment(f)
            true
        } else {
            super.onPreferenceDisplayDialog(caller, pref)
        }

    class PreferencesFragment(
        val startPreferenceFragmentFunc: (fragment: LeanbackPreferenceFragmentCompat) -> Unit,
        val startImmersiveFunc: (fragment: Fragment) -> Unit,
    ) : LeanbackPreferenceFragmentCompat() {
        private val serverViewModel: ServerViewModel by activityViewModels()
        private var subscriptionJob: Job? = null

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
                        serverViewModel.requireServer().apolloClient,
                    )
                }
                true
            }

            val manageServers = findPreference<Preference>("manageServers")
            manageServers!!.setOnPreferenceClickListener {
                serverViewModel.navigationManager.navigate(Destination.ManageServers(true))
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

            val readOnlyModePref =
                findPreference<SwitchPreference>(getString(R.string.pref_key_read_only_mode))!!
            readOnlyModePref.setOnPreferenceChangeListener { _, newValue ->
                if (newValue == true) {
                    startImmersiveFunc(ReadOnlyPinConfigFragment())
                }
                newValue == false
            }

            val installedVersion = UpdateChecker.getInstalledVersion(requireActivity())
            val versionPref = findPreference<Preference>("versionName")!!
            versionPref.summary = installedVersion.toString()
            var clickCount = 0
            versionPref.setOnPreferenceClickListener {
                if (clickCount > 2) {
                    clickCount = 0
                    serverViewModel.navigationManager.navigate(Destination.Fragment(DebugFragment::class.qualifiedName!!))
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
                                serverViewModel.navigationManager.navigate(
                                    Destination.UpdateApp(
                                        release,
                                    ),
                                )
                            } else {
                                Toast
                                    .makeText(
                                        requireContext(),
                                        "No update available!",
                                        Toast.LENGTH_SHORT,
                                    ).show()
                            }
                        } else {
                            Toast
                                .makeText(
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
                            serverViewModel.navigationManager.navigate(Destination.UpdateApp(release))
                        } else {
                            Toast
                                .makeText(
                                    requireContext(),
                                    "Failed to check for updates",
                                    Toast.LENGTH_LONG,
                                ).show()
                        }
                    }
                    true
                }
            }

            findPreference<SeekBarPreference>("skip_back_time")!!.min = 5
            findPreference<SeekBarPreference>("skip_forward_time")!!.min = 5

            val advancedPreferences = findPreference<Preference>("advancedPreferences")!!
            advancedPreferences.setOnPreferenceClickListener {
                startPreferenceFragmentFunc(AdvancedPreferencesFragment())
                true
            }
            val advancedUiPreferences = findPreference<Preference>("advancedUiPreferences")!!
            advancedUiPreferences.setOnPreferenceClickListener {
                startPreferenceFragmentFunc(SettingsUiFragment())
                true
            }
        }

        @OptIn(DelicateCoilApi::class)
        override fun onViewCreated(
            view: View,
            savedInstanceState: Bundle?,
        ) {
            Log.v(TAG, "onViewCreated: savedInstanceState==null: ${savedInstanceState == null}")
            super.onViewCreated(view, savedInstanceState)
            setTitle(getString(R.string.stashapp_settings))
            view.requestFocus()
            serverViewModel.currentServer.observe(viewLifecycleOwner) {
                if (it != null) {
                    refresh(it)
                }
            }

            val useCompose = composeEnabled()
            val composeCategoryPef = findPreference<Preference>("composeCategory")!!
            val tryComposePref = findPreference<Preference>("tryCompose")!!
            composeCategoryPef.isVisible = !useCompose
            tryComposePref.setOnPreferenceClickListener {
                val message =
                    """
                    Want to try the new beta UI based on Android Jetpack Compose?

                    The new UI is looks more modern and performs better on lower memory devices!

                    Almost every feature is available in the new UI, but you can always switch back in "More UI Settings".

                    See the `StashAppAndroidTV` GitHub for more information and known issues.

                    Enabling the new UI will restart the app!
                    """.trimIndent()
                ConfirmationDialogFragment.show(
                    childFragmentManager,
                    Markwon.create(requireContext()).toMarkdown(message),
                ) {
                    viewLifecycleOwner.lifecycleScope.launch(StashCoroutineExceptionHandler()) {
                        clearCaches(requireContext())
                        PreferenceManager.getDefaultSharedPreferences(requireContext()).edit(true) {
                            putBoolean(getString(R.string.pref_key_use_compose_ui), true)
                        }
                        // Clear coil singleton
                        SingletonImageLoader.reset()
                        requireActivity().finish()
                    }
                }
                true
            }

            if (PreferenceManager
                    .getDefaultSharedPreferences(requireContext())
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

        /**
         * Handle updating the UI for job progress
         */
        private fun handleJobUpdate(
            updateType: JobStatusUpdateType,
            job: StashJob,
            serverPrefs: ServerPreferences,
        ) {
            val triggerScan = findPreference<Preference>("triggerScan")!!
            val generatePref = findPreference<Preference>("triggerGenerate")!!

            val scanJobId = serverPrefs.scanJobId
            val generateJobId = serverPrefs.generateJobId

            if (job.id == scanJobId || job.id == generateJobId) {
                if (updateType == JobStatusUpdateType.REMOVE || !job.isRunning()) {
                    // Job complete
                    if (job.id == scanJobId) {
                        serverPrefs.scanJobId = null
                        triggerScan.summary =
                            getString(R.string.stashapp_config_tasks_scan_for_content_desc)
                    } else {
                        serverPrefs.generateJobId = null
                        generatePref.summary =
                            getString(R.string.stashapp_config_tasks_generate_desc)
                    }
                } else {
                    val message =
                        if (job.progress != null) {
                            val progress = (job.progress * 1000).toInt() / 10.0
                            if (!job.subTasks.isNullOrEmpty() &&
                                job.subTasks.first().isNotNullOrBlank()
                            ) {
                                "${job.description} $progress%\n${job.subTasks.first()}"
                            } else {
                                "${job.description} $progress%"
                            }
                        } else {
                            job.description
                        }

                    if (job.id == scanJobId) {
                        triggerScan.summary = message
                    } else {
                        generatePref.summary = message
                    }
                }
            }
        }

        private fun StashJob.isRunning() = status == JobStatus.RUNNING || status == JobStatus.READY || status == JobStatus.STOPPING

        private fun refresh(currentServer: StashServer) {
            Log.v(TAG, "refresh")
            findPreference<Preference>(PREF_STASH_URL)!!.summary = currentServer.url

            findPreference<SwitchPreference>(getString(R.string.pref_key_read_only_mode))!!.isChecked =
                PreferenceManager
                    .getDefaultSharedPreferences(requireContext())
                    .getBoolean(getString(R.string.pref_key_read_only_mode), false)

            val queryEngine = QueryEngine(currentServer)

            val triggerScan = findPreference<Preference>("triggerScan")!!
            val pendingScanJobId = currentServer.serverPreferences.scanJobId
            if (pendingScanJobId != null) {
                // Check if a job is already running
                // Such as when a user triggers it, navigates away, and then back
                viewLifecycleOwner.lifecycleScope.launch(StashCoroutineExceptionHandler()) {
                    val job = queryEngine.getJob(pendingScanJobId)
                    if (job == null || !job.isRunning()) {
                        currentServer.serverPreferences.scanJobId = null
                        triggerScan.summary =
                            getString(R.string.stashapp_config_tasks_scan_for_content_desc)
                    } else {
                        handleJobUpdate(
                            JobStatusUpdateType.UPDATE,
                            job,
                            currentServer.serverPreferences,
                        )
                    }
                }
            }

            val generatePref = findPreference<Preference>("triggerGenerate")!!
            val pendingGenJobId = currentServer.serverPreferences.generateJobId
            if (pendingGenJobId != null) {
                viewLifecycleOwner.lifecycleScope.launch(StashCoroutineExceptionHandler()) {
                    val job = queryEngine.getJob(pendingGenJobId)
                    if (job == null || !job.isRunning()) {
                        currentServer.serverPreferences.generateJobId = null
                        generatePref.summary =
                            getString(R.string.stashapp_config_tasks_generate_desc)
                    } else {
                        handleJobUpdate(
                            JobStatusUpdateType.UPDATE,
                            job,
                            currentServer.serverPreferences,
                        )
                    }
                }
            }

            val serverPrefs = currentServer.serverPreferences
            val subscriptionEngine = SubscriptionEngine(currentServer)
            // Cancel previous subscription
            subscriptionJob?.cancel()
            subscriptionJob =
                viewLifecycleOwner.lifecycleScope.launch(StashCoroutineExceptionHandler()) {
                    subscriptionEngine.subscribeToJobs { update ->
                        val type = update.jobsSubscribe.type
                        val jobData = update.jobsSubscribe.job.stashJob
                        Log.v(TAG, "job subscription update: $type ${jobData.id}")
                        handleJobUpdate(type, jobData, serverPrefs)
                    }
                }

            triggerScan.setOnPreferenceClickListener {
                if (currentServer.serverPreferences.scanJobId == null) {
                    viewLifecycleOwner.lifecycleScope.launch(
                        StashCoroutineExceptionHandler(
                            autoToast = true,
                        ),
                    ) {
                        currentServer.updateServerPrefs()
                        val jobId = MutationEngine(currentServer).triggerScan()
                        // TODO: job could finish between these two lines of code
                        currentServer.serverPreferences.scanJobId = jobId
                        Toast
                            .makeText(
                                requireContext(),
                                "Triggered a library scan",
                                Toast.LENGTH_LONG,
                            ).show()
                    }
                }
                true
            }

            generatePref.setOnPreferenceClickListener {
                if (currentServer.serverPreferences.generateJobId == null) {
                    viewLifecycleOwner.lifecycleScope.launch(
                        StashCoroutineExceptionHandler(
                            autoToast = true,
                        ),
                    ) {
                        currentServer.updateServerPrefs()
                        val jobId = MutationEngine(currentServer).triggerGenerate()
                        currentServer.serverPreferences.generateJobId = jobId
                        Toast
                            .makeText(
                                requireContext(),
                                "Triggered a generate task",
                                Toast.LENGTH_LONG,
                            ).show()
                    }
                }
                true
            }

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
                val serverPrefs = StashServer.requireCurrentServer().updateServerPrefs()
                if (serverPrefs.companionPluginInstalled) {
                    setupSendLogsPref()
                } else {
                    sendLogsPref.title = "Install companion plugin"
                    sendLogsPref.summary =
                        "Install StashAppAndroid TV Companion plugin on the current server"
                    sendLogsPref.setOnPreferenceClickListener {
                        viewLifecycleOwner.lifecycleScope.launch(Dispatchers.IO + StashCoroutineExceptionHandler()) {
                            val jobId =
                                CompanionPlugin.installPlugin(
                                    MutationEngine(serverViewModel.currentServer.value!!),
                                )
                            val queryEngine =
                                QueryEngine(serverViewModel.currentServer.value!!)
                            val result = queryEngine.waitForJob(jobId)
                            withContext(Dispatchers.Main) {
                                if (result is JobResult.Failure) {
                                    Toast
                                        .makeText(
                                            requireContext(),
                                            "Error installing plugin: ${result.message}",
                                            Toast.LENGTH_LONG,
                                        ).show()
                                } else if (result is JobResult.NotFound) {
                                    Toast
                                        .makeText(
                                            requireContext(),
                                            "Error installing plugin",
                                            Toast.LENGTH_LONG,
                                        ).show()
                                } else {
                                    val serverPrefs =
                                        StashServer.requireCurrentServer().updateServerPrefs()
                                    if (serverPrefs.companionPluginInstalled) {
                                        Toast
                                            .makeText(
                                                requireContext(),
                                                "Companion plugin installed!",
                                                Toast.LENGTH_SHORT,
                                            ).show()
                                        setupSendLogsPref()
                                    } else {
                                        Toast
                                            .makeText(
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
        }
    }

    class AdvancedPreferencesFragment : LeanbackPreferenceFragmentCompat() {
        private val serverViewModel: ServerViewModel by activityViewModels()

        override fun onCreatePreferences(
            savedInstanceState: Bundle?,
            rootKey: String?,
        ) {
            setPreferencesFromResource(R.xml.advanced_preferences, rootKey)

            findPreference<SeekBarPreference>("maxSearchResults")!!.min = 5
            findPreference<SeekBarPreference>("searchDelay")!!.min = 50

            val cacheSizePref =
                findPreference<SeekBarPreference>(getString(R.string.pref_key_network_cache_size))!!
            cacheSizePref.min = 25
            val cache = Constants.getNetworkCache(requireContext())
            setUsedCachedSummary(cacheSizePref, cache)

            findPreference<Preference>("clearCache")!!.setOnPreferenceClickListener {
                viewLifecycleOwner.lifecycleScope.launch(StashCoroutineExceptionHandler()) {
                    clearCaches(requireContext())
                    withContext(Dispatchers.Main) {
                        setUsedCachedSummary(cacheSizePref, cache)
                    }
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
                serverViewModel.navigationManager.navigate(Destination.Fragment(LicenseFragment::class.qualifiedName!!))
                true
            }

            val networkTimeoutPref = findPreference<Preference>("networkTimeout")!!
            networkTimeoutPref.setOnPreferenceClickListener {
                viewLifecycleOwner.lifecycleScope.launch(StashCoroutineExceptionHandler()) {
                    testStashConnection(
                        requireContext(),
                        true,
                        serverViewModel.requireServer().apolloClient,
                    )
                }
                true
            }
            networkTimeoutPref.setOnPreferenceChangeListener { _, newValue ->
                if (newValue == 0) {
                    Toast
                        .makeText(
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
                        StashApplication.getDatabase().playbackEffectsDao().deleteAll()
                    }
                }
                true
            }

            val videoEffectsPref =
                findPreference<SwitchPreference>(getString(R.string.pref_key_video_filters))!!
            videoEffectsPref.setOnPreferenceChangeListener { _, newValue ->
                if (newValue == true) {
                    ConfirmationDialogFragment(
                        "Some device do not support video filters!\n\nWould you like to enable it?",
                    ) { _, which ->
                        if (which == DialogInterface.BUTTON_POSITIVE) {
                            videoEffectsPref.isChecked = true
                        }
                    }.show(childFragmentManager, null)
                }
                newValue == false
            }

            val cpus = Runtime.getRuntime().availableProcessors()
            val imageThreads =
                findPreference<SeekBarPreference>(getString(R.string.pref_key_image_loading_threads))!!
            imageThreads.min = 1
            imageThreads.max = cpus * 3
            imageThreads.setDefaultValue(cpus)
            imageThreads.value =
                PreferenceManager.getDefaultSharedPreferences(requireContext()).getInt(
                    getString(R.string.pref_key_image_loading_threads),
                    Runtime.getRuntime().availableProcessors(),
                )
            imageThreads.summary = "Requires restart, default is $cpus"
        }

        override fun onViewCreated(
            view: View,
            savedInstanceState: Bundle?,
        ) {
            super.onViewCreated(view, savedInstanceState)
            setTitle(getString(R.string.advanced_settings))
            view.requestFocus()
        }

        private fun setCacheDurationSummary(
            pref: SeekBarPreference,
            value: Any,
        ) {
            val duration = cacheDurationPrefToDuration(value.toString().toInt())
            pref.summary = duration?.toString() ?: "Always request from server"
        }

        @SuppressLint("DefaultLocale")
        private fun setUsedCachedSummary(
            cacheSizePref: Preference,
            cache: Cache,
        ) {
            val glideCacheSize =
                File(
                    requireContext().cacheDir,
                    DiskCache.Factory.DEFAULT_DISK_CACHE_DIR,
                ).walkTopDown()
                    .filter { it.isFile }
                    .map { it.length() }
                    .sum()
            val cacheSizeFormatted = formatBytes(cache.size())
            val glideCacheSizeFormatted = formatBytes(glideCacheSize)

            val useCompose =
                PreferenceManager
                    .getDefaultSharedPreferences(requireContext())
                    .getBoolean(getString(R.string.pref_key_use_compose_ui), false)
            val composeCacheUsed =
                if (useCompose) {
                    val diskUsed =
                        "D:" + formatBytes(requireContext().imageLoader.diskCache?.size ?: 0L)
                    val memoryUsed =
                        "M:" + formatBytes(requireContext().imageLoader.memoryCache?.size ?: 0L)
                    " (Compose: " + listOf(diskUsed, memoryUsed).joinNotNullOrBlank(" / ") + ")"
                } else {
                    ""
                }

            cacheSizePref.summary =
                "Using $cacheSizeFormatted (Images $glideCacheSizeFormatted)$composeCacheUsed"
        }
    }

    class NumberEditTextPreferencesDialog(
        key: String,
    ) : LeanbackEditTextPreferenceDialogFragmentCompat() {
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

        suspend fun clearCaches(context: Context) =
            withContext(Dispatchers.IO) {
                withContext(Dispatchers.Main) {
                    Constants.getNetworkCache(context).evictAll()
                    Glide.get(context).clearMemory()
                }
                Glide.get(context).clearDiskCache()
                context.imageLoader.memoryCache?.clear()
                context.imageLoader.diskCache?.clear()
            }
    }
}
