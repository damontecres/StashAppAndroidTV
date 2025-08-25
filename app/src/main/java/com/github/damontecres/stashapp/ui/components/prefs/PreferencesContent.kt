package com.github.damontecres.stashapp.ui.components.prefs

import android.content.Intent
import android.widget.Toast
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.slideInHorizontally
import androidx.compose.animation.slideOutHorizontally
import androidx.compose.foundation.background
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.interaction.collectIsFocusedAsState
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.livedata.observeAsState
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.focus.focusRequester
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.AnnotatedString
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.core.content.edit
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.preference.PreferenceManager
import androidx.tv.material3.MaterialTheme
import androidx.tv.material3.Text
import com.github.damontecres.stashapp.PreferenceScreenOption
import com.github.damontecres.stashapp.R
import com.github.damontecres.stashapp.RootActivity
import com.github.damontecres.stashapp.navigation.Destination
import com.github.damontecres.stashapp.navigation.NavigationManager
import com.github.damontecres.stashapp.proto.StashPreferences
import com.github.damontecres.stashapp.ui.tryRequestFocus
import com.github.damontecres.stashapp.ui.util.ifElse
import com.github.damontecres.stashapp.ui.util.playOnClickSound
import com.github.damontecres.stashapp.ui.util.playSoundOnFocus
import com.github.damontecres.stashapp.util.Release
import com.github.damontecres.stashapp.util.StashCoroutineExceptionHandler
import com.github.damontecres.stashapp.util.StashServer
import com.github.damontecres.stashapp.util.UpdateChecker
import com.github.damontecres.stashapp.util.isNotNullOrBlank
import com.github.damontecres.stashapp.util.preferences
import kotlinx.coroutines.launch

val basicPreferences =
    listOf(
        PreferenceGroup(
            R.string.app_name_long,
            listOf(
                StashPreference.CurrentServer,
                StashPreference.ManageServers,
            ),
        ),
        PreferenceGroup(
            R.string.basic_interface,
            listOf(
                StashPreference.PinCode,
                StashPreference.ReadOnlyMode,
                StashPreference.CardSize,
                StashPreference.PlayVideoPreviews,
                StashPreference.MoreUiSettings,
            ),
        ),
        PreferenceGroup(
            R.string.playback,
            listOf(
                StashPreference.SkipForward,
                StashPreference.SkipBack,
                StashPreference.FinishedBehavior,
            ),
        ),
        PreferenceGroup(
            R.string.stashapp_config_categories_about,
            listOf(
                StashPreference.InstalledVersion,
                StashPreference.Update,
            ),
        ),
        PreferenceGroup(
            R.string.advanced_settings,
            listOf(
                StashPreference.SendLogs,
                StashPreference.AdvancedSettings,
            ),
        ),
    )

val uiPreferences =
    listOf(
        PreferenceGroup(
            R.string.advanced_ui,
            listOf(
                StashPreference.AutoSubmitPin,
                StashPreference.RememberTab,
                StashPreference.VideoPreviewDelay,
                StashPreference.SlideshowDuration,
                StashPreference.SlideshowImageClipDelay,
                StashPreference.ScrollViewAll,
                StashPreference.ScrollTopOnBack,
                StashPreference.ShowGridFooter,
                StashPreference.ShowCardRating,
                StashPreference.PageRemoteButtons,
                StashPreference.PlayCardAudio,
            ),
        ),
        PreferenceGroup(
            R.string.new_ui,
            listOf(
                StashPreference.UseNewUI,
                StashPreference.GridJumpButtons,
                StashPreference.ShowProgressSkipping,
                StashPreference.MovementSound,
                StashPreference.UpDownNextPrevious,
                StashPreference.CaptionsOnByDefault,
                StashPreference.ThemeStylePref,
                StashPreference.ChooseTheme,
            ),
        ),
        PreferenceGroup(
            R.string.show_hide_tabs,
            StashPreference.TabPrefs,
        ),
    )

val advancedPreferences =
    listOf(
        PreferenceGroup(
            R.string.playback,
            listOf(
                StashPreference.SavePlayHistory,
                StashPreference.DPadSkipping,
                StashPreference.DPadSkipIndicator,
                StashPreference.ControllerTimeout,
                StashPreference.StartPlaybackMuted,
                StashPreference.PlaybackStreamChoice,
                StashPreference.TranscodeAboveResolution,
            ),
        ),
        PreferenceGroup(
            R.string.advanced_playback,
            listOf(
                StashPreference.PlaybackDebugInfo,
                StashPreference.PlaybackDebugLogging,
                StashPreference.PlaybackStreamingClient,
                StashPreference.DirectPlayVideo,
                StashPreference.DirectPlayAudio,
                StashPreference.DirectPlayFormat,
            ),
        ),
        PreferenceGroup(
            R.string.effects_filters,
            listOf(
                StashPreference.VideoFilter,
                StashPreference.PersistVideoFilter,
            ),
        ),
        PreferenceGroup(
            R.string.stashapp_actions_search,
            listOf(
                StashPreference.SearchResults,
                StashPreference.SearchDelay,
            ),
        ),
        PreferenceGroup(
            R.string.stashapp_config_tasks_job_queue,
            listOf(
                StashPreference.TriggerScan,
                StashPreference.TriggerGenerate,
            ),
        ),
        PreferenceGroup(
            R.string.cache,
            listOf(
                StashPreference.NetworkCache,
                StashPreference.ImageDiskCache,
                StashPreference.CacheInvalidation,
                StashPreference.CacheLogging,
                StashPreference.CacheClear,
            ),
        ),
        PreferenceGroup(
            R.string.stashapp_config_categories_about,
            listOf(
                StashPreference.AutoCheckForUpdates,
                StashPreference.UpdateUrl,
                StashPreference.OssLicenseInfo,
            ),
        ),
        PreferenceGroup(
            R.string.advanced_settings,
            listOf(
                StashPreference.CrashReporting,
                StashPreference.LogErrorsToServer,
                StashPreference.ExperimentalFeatures,
                StashPreference.TrustCertificates,
                StashPreference.NetworkTimeout,
                StashPreference.ImageThreads,
                StashPreference.MigratePreferences,
            ),
        ),
    )

@Composable
fun PreferencesContent(
    server: StashServer,
    navigationManager: NavigationManager,
    initialPreferences: StashPreferences,
    preferenceScreenOption: PreferenceScreenOption,
    modifier: Modifier = Modifier,
    onUpdateTitle: ((AnnotatedString) -> Unit)? = null,
    viewModel: PreferencesViewModel = viewModel(),
) {
    val context = LocalContext.current
    val scope = rememberCoroutineScope()
    val focusRequester = remember { FocusRequester() }
    var focusedIndex by rememberSaveable { mutableStateOf(Pair(0, 0)) }
    val state = rememberLazyListState()
    var preferences by remember { mutableStateOf(initialPreferences) }
    val sharedPrefs = remember { PreferenceManager.getDefaultSharedPreferences(context) }
    LaunchedEffect(Unit) {
        context.preferences.data.collect {
            preferences = it
        }
    }

    val movementSounds = preferences.interfacePreferences.playMovementSounds
    val installedVersion = remember { UpdateChecker.getInstalledVersion(context) }
    var updateVersion by remember { mutableStateOf<Release?>(null) }
    val updateAvailable =
        remember(updateVersion) { updateVersion?.version?.isGreaterThan(installedVersion) == true }

    if (preferences.updatePreferences.checkForUpdates) {
        LaunchedEffect(Unit) {
            updateVersion =
                UpdateChecker.getLatestRelease(context, preferences.updatePreferences.updateUrl)
        }
    }

    val prefList =
        when (preferenceScreenOption) {
            PreferenceScreenOption.BASIC -> basicPreferences
            PreferenceScreenOption.ADVANCED -> advancedPreferences
            PreferenceScreenOption.USER_INTERFACE -> uiPreferences
        }
    val screenTitle =
        when (preferenceScreenOption) {
            PreferenceScreenOption.BASIC -> "Preferences"
            PreferenceScreenOption.ADVANCED -> "Advanced Preferences"
            PreferenceScreenOption.USER_INTERFACE -> "User Interface Preferences"
        }
    LaunchedEffect(Unit) {
        onUpdateTitle?.invoke(AnnotatedString(screenTitle))
        if (preferenceScreenOption == PreferenceScreenOption.ADVANCED) {
            viewModel.init(context, server)
        }
    }
    val jobQueue by viewModel.runningJobs.observeAsState(listOf())
    val cacheUsage by viewModel.cacheUsage.observeAsState(CacheUsage(0, 0, 0, 0))

    var visible by remember { mutableStateOf(false) }
    LaunchedEffect(Unit) {
        // Forces the animated to trigger
        visible = true
    }

    AnimatedVisibility(
        visible = visible,
        enter = fadeIn() + slideInHorizontally { it / 2 },
        exit = fadeOut() + slideOutHorizontally { it / 2 },
        modifier = modifier,
    ) {
        LaunchedEffect(Unit) {
            focusRequester.tryRequestFocus()
        }
        LazyColumn(
            state = state,
            horizontalAlignment = Alignment.Start,
            verticalArrangement = Arrangement.spacedBy(0.dp),
            contentPadding = PaddingValues(16.dp),
            modifier = Modifier.background(MaterialTheme.colorScheme.secondaryContainer),
        ) {
            if (onUpdateTitle == null) {
                stickyHeader {
                    Text(
                        text = screenTitle,
                        style = MaterialTheme.typography.headlineSmall,
                        color = MaterialTheme.colorScheme.onSurface,
                        textAlign = TextAlign.Center,
                        modifier =
                            Modifier
                                .fillMaxWidth()
                                .padding(vertical = 8.dp),
                    )
                }
            }
            prefList.forEachIndexed { groupIndex, group ->
                item {
                    Text(
                        text = stringResource(group.title),
                        style = MaterialTheme.typography.titleMedium,
                        color = MaterialTheme.colorScheme.border,
                        textAlign = TextAlign.Start,
                        modifier =
                            Modifier
                                .fillMaxWidth()
                                .padding(top = 8.dp, bottom = 4.dp),
                    )
                }
                if (updateAvailable &&
                    groupIndex == 0 &&
                    preferenceScreenOption == PreferenceScreenOption.BASIC
                ) {
                    item {
                        val updateFocusRequester = remember { FocusRequester() }
                        LaunchedEffect(Unit) {
                            if (focusedIndex.first == 0 && focusedIndex.second == 0) {
                                // Only re-focus if the user hasn't moved
                                updateFocusRequester.tryRequestFocus()
                            }
                        }
                        ClickPreference(
                            title = stringResource(R.string.install_update),
                            onClick = {
                                if (movementSounds) playOnClickSound(context)
                                updateVersion?.let {
                                    navigationManager.navigate(Destination.UpdateApp(it))
                                }
                            },
                            summary = updateVersion?.version?.toString(),
                            modifier =
                                Modifier
                                    .focusRequester(updateFocusRequester)
                                    .playSoundOnFocus(movementSounds),
                        )
                    }
                }
                group.preferences.forEachIndexed { prefIndex, pref ->
                    pref as StashPreference<Any>
                    item {
                        val interactionSource = remember { MutableInteractionSource() }
                        val focused = interactionSource.collectIsFocusedAsState().value
                        LaunchedEffect(focused) {
                            if (focused) {
                                focusedIndex = Pair(groupIndex, prefIndex)
                                if (movementSounds) playOnClickSound(context)
                            }
                        }
                        when (pref) {
                            StashPreference.InstalledVersion -> {
                                var clickCount by remember { mutableIntStateOf(0) }
                                ClickPreference(
                                    title = stringResource(R.string.stashapp_package_manager_installed_version),
                                    onClick = {
                                        if (movementSounds) playOnClickSound(context)
                                        if (clickCount++ >= 2) {
                                            clickCount = 0
                                            navigationManager.navigate(Destination.Debug)
                                        }
                                    },
                                    summary = installedVersion.toString(),
                                    interactionSource = interactionSource,
                                    modifier =
                                        Modifier
                                            .ifElse(
                                                groupIndex == focusedIndex.first && prefIndex == focusedIndex.second,
                                                Modifier.focusRequester(focusRequester),
                                            ),
                                )
                            }

                            StashPreference.Update -> {
                                ClickPreference(
                                    title =
                                        if (updateVersion != null && updateAvailable) {
                                            stringResource(R.string.install_update)
                                        } else if (!preferences.updatePreferences.checkForUpdates) {
                                            stringResource(R.string.stashapp_package_manager_check_for_updates)
                                        } else {
                                            stringResource(R.string.no_update_available)
                                        },
                                    onClick = {
                                        if (movementSounds) playOnClickSound(context)
                                        if (updateVersion != null && updateAvailable) {
                                            updateVersion?.let {
                                                navigationManager.navigate(
                                                    Destination.UpdateApp(it),
                                                )
                                            }
                                        } else if (!preferences.updatePreferences.checkForUpdates) {
                                            scope.launch(StashCoroutineExceptionHandler()) {
                                                updateVersion =
                                                    UpdateChecker.getLatestRelease(
                                                        context,
                                                        preferences.updatePreferences.updateUrl,
                                                    )
                                            }
                                        } else {
                                            Toast
                                                .makeText(
                                                    context,
                                                    R.string.no_update_available,
                                                    Toast.LENGTH_SHORT,
                                                ).show()
                                        }
                                    },
                                    onLongClick = {
                                        if (movementSounds) playOnClickSound(context)
                                        updateVersion?.let {
                                            navigationManager.navigate(
                                                Destination.UpdateApp(it),
                                            )
                                        }
                                    },
                                    summary =
                                        if (updateAvailable) {
                                            updateVersion?.version?.toString()
                                        } else {
                                            null
                                        },
                                    interactionSource = interactionSource,
                                    modifier =
                                        Modifier
                                            .ifElse(
                                                groupIndex == focusedIndex.first && prefIndex == focusedIndex.second,
                                                Modifier.focusRequester(focusRequester),
                                            ),
                                )
                            }

                            else -> {
                                val value = pref.getter.invoke(preferences)
                                ComposablePreference(
                                    server = server,
                                    navigationManager = navigationManager,
                                    preference = pref,
                                    value = value,
                                    onValueChange = { newValue ->
                                        val validation = pref.validate(newValue)
                                        when (validation) {
                                            is PreferenceValidation.Invalid -> {
                                                // TODO?
                                                Toast
                                                    .makeText(
                                                        context,
                                                        validation.message,
                                                        Toast.LENGTH_SHORT,
                                                    ).show()
                                            }

                                            PreferenceValidation.Valid -> {
                                                scope.launch(StashCoroutineExceptionHandler()) {
                                                    preferences =
                                                        context.preferences.updateData { prefs ->
                                                            pref.setter(prefs, newValue)
                                                        }
                                                    sharedPrefs.edit {
                                                        pref.prefSetter(context, this, newValue)
                                                        if (pref == StashPreference.ReadOnlyMode) {
                                                            // Legacy read only mode has two preferences
                                                            // New mode just saves the PIN, but need to explicitly enable/disable too for legacy
                                                            putBoolean(
                                                                context.getString(R.string.pref_key_read_only_mode),
                                                                newValue
                                                                    .toString()
                                                                    .isNotNullOrBlank(),
                                                            )
                                                        }
                                                    }
                                                    if (pref == StashPreference.UseNewUI && newValue is Boolean && !newValue) {
                                                        context.startActivity(
                                                            Intent(
                                                                context,
                                                                RootActivity::class.java,
                                                            ).setFlags(Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TASK),
                                                        )
                                                    }
                                                }
                                            }
                                        }
                                    },
                                    onCacheClear = { viewModel.updateCacheUsage(context) },
                                    cacheUsage = cacheUsage,
                                    interactionSource = interactionSource,
                                    modifier =
                                        Modifier
                                            .ifElse(
                                                groupIndex == focusedIndex.first && prefIndex == focusedIndex.second,
                                                Modifier.focusRequester(focusRequester),
                                            ),
                                )
                            }
                        }
                    }
                }
                if (preferenceScreenOption == PreferenceScreenOption.ADVANCED && group.title == R.string.stashapp_config_tasks_job_queue) {
                    if (jobQueue.isEmpty()) {
                        item {
                            Text(
                                text = stringResource(R.string.stashapp_config_tasks_empty_queue),
                                style = MaterialTheme.typography.titleSmall,
                                color = MaterialTheme.colorScheme.onSurface,
                                modifier =
                                    Modifier
                                        .padding(horizontal = 8.dp, vertical = 16.dp),
                            )
                        }
                    } else {
                        item {
                            Spacer(Modifier.height(8.dp))
                            jobQueue.forEach {
                                JobDisplay(it, Modifier.padding(horizontal = 8.dp))
                            }
                        }
                    }
                }
            }
        }
    }
}

fun getPinPreferenceKey(pref: StashPreference<*>) =
    when (pref) {
        StashPreference.PinCode -> R.string.pref_key_pin_code
        StashPreference.ReadOnlyMode -> R.string.pref_key_read_only_mode_pin
        else -> throw IllegalStateException("Unknown preference type: $pref")
    }
