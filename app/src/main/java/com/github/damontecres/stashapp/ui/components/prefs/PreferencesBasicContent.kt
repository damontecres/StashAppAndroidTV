package com.github.damontecres.stashapp.ui.components.prefs

import android.widget.Toast
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import androidx.core.content.edit
import androidx.preference.PreferenceManager
import androidx.tv.material3.MaterialTheme
import androidx.tv.material3.Text
import com.github.damontecres.stashapp.R

private val preferences =
    listOf(
        PreferenceGroup(
            R.string.basic_interface,
            listOf(
                StashPreference.AutoSubmitPin,
                StashPreference.PinCode,
                StashPreference.ReadOnlyMode,
                StashPreference.CardSize,
            ),
        ),
    )

@Composable
fun PreferencesBasicContent(modifier: Modifier = Modifier) {
    val context = LocalContext.current
    val pm = remember { PreferenceManager.getDefaultSharedPreferences(context) }
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
        preferences.forEach { group ->
            item {
                Text(
                    text = stringResource(group.title),
                    style = MaterialTheme.typography.titleMedium,
                    color = MaterialTheme.colorScheme.onSurface,
                )
            }
            group.preferences.forEach { pref ->
                pref as StashPreference<Any>
                item {
                    val key = stringResource(pref.key)
                    var value by remember { mutableStateOf(pref.getter.invoke(pm, key)) }
                    ComposablePreference(
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
                                    pm.edit {
                                        pref.setter.invoke(this, key, newValue)
                                    }
                                    value = newValue
                                }
                            }
                        },
                        modifier = Modifier,
                    )
                }
            }
        }
    }
}
