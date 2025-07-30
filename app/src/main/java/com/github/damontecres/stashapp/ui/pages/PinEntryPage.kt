package com.github.damontecres.stashapp.ui.pages

import android.widget.Toast
import androidx.activity.compose.BackHandler
import androidx.annotation.StringRes
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.lazy.LazyColumn
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
import androidx.preference.PreferenceManager
import androidx.tv.material3.MaterialTheme
import androidx.tv.material3.Text
import com.github.damontecres.stashapp.R
import com.github.damontecres.stashapp.ui.compat.Button
import com.github.damontecres.stashapp.ui.components.EditTextBox
import com.github.damontecres.stashapp.ui.tryRequestFocus
import com.github.damontecres.stashapp.util.findActivity

@Composable
fun PinEntryPage(
    @StringRes pinPreference: Int,
    title: String,
    onCorrectPin: () -> Unit,
    preventBack: Boolean,
    modifier: Modifier = Modifier,
) {
    val context = LocalContext.current
    BackHandler(enabled = preventBack) {
        context.findActivity()?.finish()
    }

    val prefs = remember { PreferenceManager.getDefaultSharedPreferences(context) }
    val requiredPin = remember { prefs.getString(context.getString(pinPreference), null)!! }
    val autoSubmit = remember { prefs.getBoolean(context.getString(R.string.pref_key_pin_code_auto), true) }

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
            modifier = Modifier.align(Alignment.Center),
            verticalArrangement = Arrangement.spacedBy(8.dp),
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
