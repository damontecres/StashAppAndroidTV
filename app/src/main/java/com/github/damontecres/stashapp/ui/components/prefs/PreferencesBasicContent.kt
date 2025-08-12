package com.github.damontecres.stashapp.ui.components.prefs

import android.widget.Toast
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.shape.RoundedCornerShape
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
import androidx.compose.ui.window.Dialog
import androidx.tv.material3.MaterialTheme
import androidx.tv.material3.Text
import com.github.damontecres.stashapp.R
import com.github.damontecres.stashapp.navigation.NavigationManager
import com.github.damontecres.stashapp.proto.StashPreferences
import com.github.damontecres.stashapp.ui.components.server.ConfigurePin
import com.github.damontecres.stashapp.ui.tryRequestFocus
import com.github.damontecres.stashapp.ui.util.ifElse
import com.github.damontecres.stashapp.util.StashCoroutineExceptionHandler
import com.github.damontecres.stashapp.util.StashServer
import com.github.damontecres.stashapp.util.isNotNullOrBlank
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
                StashPreference.AutoSubmitPin,
                StashPreference.PinCode,
                StashPreference.CardSize,
                StashPreference.PlayVideoPreviews,
                StashPreference.ReadOnlyMode,
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
    )

@Composable
fun PreferencesBasicContent(
    server: StashServer,
    navigationManager: NavigationManager,
    preferences: StashPreferences,
    modifier: Modifier = Modifier,
) {
    val context = LocalContext.current
    val scope = rememberCoroutineScope()
    val focusRequester = remember { FocusRequester() }

    var readOnlyEnabled by remember { mutableStateOf(preferences.pinPreferences.readOnlyPin.isNotNullOrBlank()) }
    var showReadOnlyDialog by remember { mutableStateOf(false) }

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
                text = "Basic Preferences",
                style = MaterialTheme.typography.headlineSmall,
                color = MaterialTheme.colorScheme.onSurface,
            )
        }
        basicPreferences.forEachIndexed { groupIndex, group ->
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
                    var value by remember { mutableStateOf(pref.getter.invoke(preferences)) }
                    if (pref == StashPreference.ReadOnlyMode) {
                        SwitchPreference(
                            title = stringResource(pref.title),
                            value = readOnlyEnabled,
                            onClick = {
                                if (readOnlyEnabled) {
                                    // Enabled, so disable
                                    scope.launch(StashCoroutineExceptionHandler()) {
                                        context.preferences.updateData { prefs ->
                                            pref.setter(prefs, "")
                                        }
                                        readOnlyEnabled = false
                                    }
                                } else {
                                    showReadOnlyDialog = true
                                }
                            },
                            modifier = Modifier,
                        )
                    } else {
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
                                            context.preferences.updateData { prefs ->
                                                pref.setter(prefs, newValue)
                                            }
                                            value = newValue
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
    AnimatedVisibility(showReadOnlyDialog) {
        Dialog(
            onDismissRequest = { showReadOnlyDialog = false },
        ) {
            ConfigurePin(
                onCancel = { showReadOnlyDialog = false },
                onSubmit = { pin ->
                    scope.launch(StashCoroutineExceptionHandler()) {
                        context.preferences
                            .updateData { prefs ->
                                StashPreference.ReadOnlyMode.setter(prefs, pin)
                            }
                        readOnlyEnabled = true
                        showReadOnlyDialog = false
                    }
                },
                descriptionString = R.string.read_only_pin_description,
                cancelString = R.string.stashapp_actions_cancel,
                modifier =
                    Modifier
                        .background(
                            color = MaterialTheme.colorScheme.secondaryContainer,
                            shape = RoundedCornerShape(8.dp),
                        ),
            )
        }
    }
}
