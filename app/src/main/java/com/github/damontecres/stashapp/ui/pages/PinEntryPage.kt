package com.github.damontecres.stashapp.ui.pages

import android.widget.Toast
import androidx.activity.compose.BackHandler
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.focus.focusRequester
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import androidx.tv.material3.MaterialTheme
import androidx.tv.material3.Text
import com.github.damontecres.stashapp.R
import com.github.damontecres.stashapp.di.services.NavigationManager
import com.github.damontecres.stashapp.navigation.Destination
import com.github.damontecres.stashapp.proto.StashPreferences
import com.github.damontecres.stashapp.ui.compat.Button
import com.github.damontecres.stashapp.ui.components.EditTextBox
import com.github.damontecres.stashapp.ui.tryRequestFocus
import com.github.damontecres.stashapp.util.PreferenceScreenOption
import com.github.damontecres.stashapp.util.findActivity

@Composable
fun PinEntryPage(
    requiredPin: String,
    title: String,
    onCorrectPin: () -> Unit,
    preventBack: Boolean,
    autoSubmit: Boolean,
    modifier: Modifier = Modifier,
) {
    val context = LocalContext.current
    BackHandler(enabled = preventBack) {
        context.findActivity()?.finish()
    }

    val validate = { input: String ->
        input == requiredPin
    }

    val validateAndGo = { input: String ->
        if (validate(input)) {
            onCorrectPin.invoke()
        } else {
            Toast.makeText(context, "Incorrect PIN", Toast.LENGTH_SHORT).show()
        }
    }

    var enteredPin by remember { mutableStateOf("") }
    val focusRequester = remember { FocusRequester() }
    LaunchedEffect(Unit) {
        focusRequester.tryRequestFocus()
    }
    Box(modifier = modifier) {
        LazyColumn(
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.spacedBy(8.dp),
            contentPadding = PaddingValues(16.dp),
            modifier =
                Modifier
                    .padding(top = 80.dp)
                    .align(Alignment.TopCenter)
                    .background(
                        color = MaterialTheme.colorScheme.surface,
                        shape = RoundedCornerShape(16.dp),
                    ),
        ) {
            item {
                Text(
                    text = title,
                    color = MaterialTheme.colorScheme.onBackground,
                    style = MaterialTheme.typography.titleMedium,
                )
            }
            item {
                EditTextBox(
                    modifier = Modifier.focusRequester(focusRequester),
                    value = enteredPin,
                    onValueChange = {
                        enteredPin = it
                        if (autoSubmit && validate(it)) {
                            onCorrectPin.invoke()
                        }
                    },
                    keyboardOptions =
                        KeyboardOptions(
                            keyboardType = KeyboardType.NumberPassword,
                            autoCorrectEnabled = false,
                            imeAction = ImeAction.Go,
                        ),
                    keyboardActions =
                        KeyboardActions(
                            onGo = { validateAndGo(enteredPin) },
                        ),
                )
            }
            item {
                Button(
                    modifier = Modifier,
                    onClick = { validateAndGo(enteredPin) },
                ) {
                    Text(text = stringResource(R.string.stashapp_actions_submit))
                }
            }
        }
    }
}

@Composable
fun SettingsPinPage(
    navigationManager: NavigationManager,
    preferences: StashPreferences,
    modifier: Modifier = Modifier,
) {
    PinEntryPage(
        title = stringResource(R.string.enter_settings_pin),
        requiredPin = preferences.pinPreferences.readOnlyPin,
        autoSubmit = preferences.pinPreferences.autoSubmit,
        onCorrectPin = {
            // Pop Destination.SettingsPin off the stack and go to settings
            // This prevents showing the PIN entry again when going back from settings
            navigationManager.goBack()
            navigationManager.navigate(Destination.Settings(PreferenceScreenOption.BASIC))
        },
        preventBack = false,
        modifier = modifier,
    )
}
