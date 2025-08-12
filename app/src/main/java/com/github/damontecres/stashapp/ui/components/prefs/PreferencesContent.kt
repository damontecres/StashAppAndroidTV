package com.github.damontecres.stashapp.ui.components.prefs

import android.widget.Toast
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
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
import com.github.damontecres.stashapp.navigation.NavigationManager
import com.github.damontecres.stashapp.proto.StashPreferences
import com.github.damontecres.stashapp.ui.tryRequestFocus
import com.github.damontecres.stashapp.ui.util.ifElse
import com.github.damontecres.stashapp.util.StashCoroutineExceptionHandler
import com.github.damontecres.stashapp.util.StashServer
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
            R.string.advanced_settings,
            listOf(
                StashPreference.AdvancedSettings,
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
            listOf(), // TODO
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

    val prefList =
        when (preferenceScreenOption) {
            PreferenceScreenOption.BASIC -> basicPreferences
            PreferenceScreenOption.ADVANCED -> advancedPreferences
            PreferenceScreenOption.USER_INTERFACE -> uiPreferences
        }
    val title =
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
        verticalArrangement = Arrangement.spacedBy(8.dp),
        contentPadding = PaddingValues(16.dp),
        modifier = modifier,
    ) {
        item {
            Text(
                text = title,
                style = MaterialTheme.typography.headlineSmall,
                color = MaterialTheme.colorScheme.onSurface,
            )
        }
        prefList.forEachIndexed { groupIndex, group ->
            item {
                Text(
                    text = stringResource(group.title),
                    style = MaterialTheme.typography.titleLarge,
                    color = MaterialTheme.colorScheme.border,
                    textAlign = TextAlign.Center,
                    modifier = Modifier.fillMaxWidth(),
                )
            }
            group.preferences.forEachIndexed { prefIndex, pref ->
                pref as StashPreference<Any>
                item {
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
                                        if (pref is StashPinPreference) {
                                            // TODO also store in shared preferences
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

fun getPinPreferenceKey(pref: StashPreference<*>) =
    when (pref) {
        StashPreference.PinCode -> R.string.pref_key_pin_code
        StashPreference.ReadOnlyMode -> R.string.pref_key_read_only_mode_pin
        else -> throw IllegalStateException("Unknown preference type: $pref")
    }
