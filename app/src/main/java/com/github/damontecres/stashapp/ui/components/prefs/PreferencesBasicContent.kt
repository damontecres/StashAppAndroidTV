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
import androidx.tv.material3.MaterialTheme
import androidx.tv.material3.Text
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
                StashPreference.AutoSubmitPin,
                StashPreference.PinCode,
                StashPreference.CardSize,
                StashPreference.PlayVideoPreviews,
                StashPreference.MoreUiSettings,
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
