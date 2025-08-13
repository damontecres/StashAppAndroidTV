package com.github.damontecres.stashapp.ui.components.prefs

import android.widget.Toast
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.focus.focusRequester
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.core.content.edit
import androidx.preference.PreferenceManager
import androidx.tv.material3.MaterialTheme
import androidx.tv.material3.Text
import com.github.damontecres.stashapp.PreferenceScreenOption
import com.github.damontecres.stashapp.R
import com.github.damontecres.stashapp.navigation.Destination
import com.github.damontecres.stashapp.navigation.NavigationManager
import com.github.damontecres.stashapp.proto.StashPreferences
import com.github.damontecres.stashapp.ui.tryRequestFocus
import com.github.damontecres.stashapp.ui.util.ifElse
import com.github.damontecres.stashapp.util.Release
import com.github.damontecres.stashapp.util.StashCoroutineExceptionHandler
import com.github.damontecres.stashapp.util.StashServer
import com.github.damontecres.stashapp.util.UpdateChecker
import com.github.damontecres.stashapp.util.preferences
import kotlinx.coroutines.launch

private val basicPreferences =
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
                StashPreference.AutoSubmitPin,
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

private val uiPreferences =
    listOf(
        PreferenceGroup(
            R.string.advanced_ui,
            listOf(
                StashPreference.RememberTab,
                StashPreference.VideoPreviewDelay,
                StashPreference.SlideshowDuration,
                StashPreference.SlideshowImageClipDelay,
            ),
        ),
        PreferenceGroup(
            R.string.show_hide_tabs,
            StashPreference.TabPrefs,
        ),
        PreferenceGroup(
            R.string.new_ui,
            listOf(
                StashPreference.UseNewUI,
                StashPreference.GridJumpButtons,
                StashPreference.ChooseTheme,
                StashPreference.ThemeStylePref,
                StashPreference.ShowProgressSkipping,
                StashPreference.MovementSound,
                StashPreference.UpDownNextPrevious,
                StashPreference.Captions,
            ),
        ),
    )

private val advancedPreferences =
    listOf(
        PreferenceGroup(
            R.string.advanced_ui,
            StashPreference.advancedUiPrefs,
        ),
        PreferenceGroup(
            R.string.playback,
            StashPreference.advancedPlaybackPrefs,
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
                StashPreference.CheckForUpdates,
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
                StashPreference.NetworkTimeout,
                StashPreference.PlaybackDebugLogging,
                StashPreference.PlaybackStreamingClient,
                StashPreference.ImageThreads,
                StashPreference.TrustCertificates,
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
) {
    val context = LocalContext.current
    val scope = rememberCoroutineScope()
    val focusRequester = remember { FocusRequester() }
    var preferences by remember { mutableStateOf(initialPreferences) }

    val installedVersion = remember { UpdateChecker.getInstalledVersion(context) }
    var updateVersion by remember { mutableStateOf<Release?>(null) }
    val updateAvailable = updateVersion?.version?.isGreaterThan(installedVersion) == true

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
        focusRequester.tryRequestFocus()
    }
    LazyColumn(
        horizontalAlignment = Alignment.Start,
        verticalArrangement = Arrangement.spacedBy(0.dp),
        contentPadding = PaddingValues(16.dp),
        modifier = modifier,
    ) {
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
            if (updateAvailable && preferences.updatePreferences.checkForUpdates) {
                item {
                    val updateFocusRequester = remember { FocusRequester() }
                    LaunchedEffect(Unit) { updateFocusRequester.tryRequestFocus() }
                    ClickPreference(
                        title = stringResource(R.string.install_update),
                        onClick = {
                            updateVersion?.let {
                                navigationManager.navigate(Destination.UpdateApp(it))
                            }
                        },
                        summary = updateVersion?.version?.toString(),
                        modifier = Modifier.focusRequester(updateFocusRequester),
                    )
                }
            }
            group.preferences.forEachIndexed { prefIndex, pref ->
                pref as StashPreference<Any>
                item {
                    when (pref) {
                        StashPreference.InstalledVersion -> {
                            var clickCount by remember { mutableIntStateOf(0) }
                            ClickPreference(
                                title = stringResource(R.string.stashapp_package_manager_installed_version),
                                onClick = {
                                    if (clickCount++ >= 2) {
                                        clickCount = 0
                                        navigationManager.navigate(Destination.Debug)
                                    }
                                },
                                summary = installedVersion.toString(),
                                modifier =
                                    Modifier.ifElse(
                                        groupIndex == 0 && prefIndex == 0,
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
                                modifier =
                                    Modifier.ifElse(
                                        groupIndex == 0 && prefIndex == 0,
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
                                                // TODO also store some in shared preferences
                                                if (pref is StashPinPreference) {
                                                    PreferenceManager
                                                        .getDefaultSharedPreferences(context)
                                                        .edit(true) {
                                                            putString(
                                                                context.getString(
                                                                    getPinPreferenceKey(
                                                                        pref,
                                                                    ),
                                                                ),
                                                                (newValue as String).ifBlank { null },
                                                            )
                                                        }
                                                } else if (pref == StashPreference.UseNewUI) {
                                                    PreferenceManager
                                                        .getDefaultSharedPreferences(context)
                                                        .edit(true) {
                                                            putBoolean(
                                                                context.getString(
                                                                    R.string.pref_key_use_compose_ui,
                                                                ),
                                                                newValue as Boolean,
                                                            )
                                                        }
                                                }
                                            }
                                        }
                                    }
                                },
                                modifier =
                                    Modifier.ifElse(
                                        groupIndex == 0 && prefIndex == 0,
                                        Modifier.focusRequester(focusRequester),
                                    ),
                            )
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
