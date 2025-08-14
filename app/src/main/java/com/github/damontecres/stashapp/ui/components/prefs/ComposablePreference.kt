package com.github.damontecres.stashapp.ui.components.prefs

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.foundation.background
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.runtime.Composable
import androidx.compose.runtime.ReadOnlyComposable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.mutableStateSetOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringArrayResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.unit.dp
import androidx.compose.ui.window.Dialog
import androidx.core.content.edit
import androidx.preference.PreferenceManager
import androidx.tv.material3.MaterialTheme
import androidx.tv.material3.Switch
import androidx.tv.material3.Text
import com.github.damontecres.stashapp.R
import com.github.damontecres.stashapp.SettingsFragment
import com.github.damontecres.stashapp.navigation.NavigationManager
import com.github.damontecres.stashapp.ui.components.DialogItem
import com.github.damontecres.stashapp.ui.components.DialogPopup
import com.github.damontecres.stashapp.ui.components.server.ConfigurePin
import com.github.damontecres.stashapp.ui.pages.DialogParams
import com.github.damontecres.stashapp.util.StashCoroutineExceptionHandler
import com.github.damontecres.stashapp.util.StashServer
import com.github.damontecres.stashapp.util.isNotNullOrBlank
import com.github.damontecres.stashapp.util.plugin.CompanionPlugin
import com.github.damontecres.stashapp.util.testStashConnection
import kotlinx.coroutines.launch

@Suppress("UNCHECKED_CAST")
@Composable
fun <T> ComposablePreference(
    server: StashServer,
    navigationManager: NavigationManager,
    preference: StashPreference<T>,
    value: T?,
    onValueChange: (T) -> Unit,
    modifier: Modifier = Modifier,
    interactionSource: MutableInteractionSource = remember { MutableInteractionSource() },
) {
    val context = LocalContext.current
    val scope = rememberCoroutineScope()

    var dialogParams by remember { mutableStateOf<DialogParams?>(null) }
    var showPinDialog by remember { mutableStateOf<StashPinPreference?>(null) }

    val title = stringResource(preference.title)

    when (preference) {
        StashPreference.CurrentServer -> {
            ClickPreference(
                title = title,
                onClick = {
                    scope.launch(StashCoroutineExceptionHandler()) {
                        testStashConnection(context, true, server.apolloClient)
                    }
                },
                summary = server.url,
                interactionSource = interactionSource,
                modifier = modifier,
            )
        }

        StashPreference.SendLogs -> {
            ClickPreference(
                title = title,
                onClick = {
                    scope.launch(StashCoroutineExceptionHandler()) {
                        CompanionPlugin.sendLogCat(
                            context,
                            server,
                            false,
                        )
                    }
                },
                onLongClick = {
                    scope.launch(StashCoroutineExceptionHandler()) {
                        CompanionPlugin.sendLogCat(
                            context,
                            server,
                            true,
                        )
                    }
                },
                summary = preference.summary(context, value),
                interactionSource = interactionSource,
                modifier = modifier,
            )
        }

        StashPreference.CacheClear -> {
            ClickPreference(
                title = title,
                onClick = {
                    scope.launch(StashCoroutineExceptionHandler()) {
                        SettingsFragment.clearCaches(context)
                    }
                },
                summary = preference.summary(context, value),
                interactionSource = interactionSource,
                modifier = modifier,
            )
        }

        is StashDestinationPreference ->
            ClickPreference(
                title = title,
                onClick = {
                    navigationManager.navigate(preference.destination)
                },
                summary = preference.summary(context, value),
                interactionSource = interactionSource,
                modifier = modifier,
            )

        is StashClickablePreference ->
            ClickPreference(
                title = title,
                onClick = {},
                summary = preference.summary(context, value),
                interactionSource = interactionSource,
                modifier = modifier,
            )

        is StashSwitchPreference ->
            SwitchPreference(
                title = title,
                value = value as Boolean,
                onClick = { onValueChange.invoke(!value as T) },
                summary = preference.summary(context, value),
                interactionSource = interactionSource,
                modifier = modifier,
            )

        is StashPinPreference -> {
            val enabled = (value as String).isNotNullOrBlank()
            SwitchPreference(
                title = stringResource(preference.title),
                value = enabled,
                summary = preference.summary(context, value),
                onClick = {
                    if (enabled) {
                        // Enabled, so disable
                        onValueChange.invoke("" as T)
                        PreferenceManager
                            .getDefaultSharedPreferences(context)
                            .edit(true) {
                                remove(context.getString(getPinPreferenceKey(preference)))
                            }
                    } else {
                        showPinDialog = preference
                    }
                },
                interactionSource = interactionSource,
                modifier = Modifier,
            )
        }
        is StashStringPreference -> {
            // TODO
        }

        is StashChoicePreference -> {
            val values = stringArrayResource(preference.displayValues).toList()
            val summary =
                preference.summary?.let { stringResource(it) }
                    ?: preference.summary(context, value)
                    ?: preference
                        .valueToIndex(value as T)
                        .let { values[it] }
            ClickPreference(
                title = title,
                summary = summary,
                onClick = {
                    dialogParams =
                        DialogParams(
                            title = title,
                            fromLongClick = false,
                            items =
                                values.mapIndexed { index, it ->
                                    DialogItem(
                                        text = it,
                                        onClick = {
                                            onValueChange(preference.indexToValue(index))
                                            dialogParams = null
                                        },
                                    )
                                },
                        )
                },
                interactionSource = interactionSource,
                modifier = modifier,
            )
        }

        is StashMultiChoicePreference<*> -> {
            val values = stringArrayResource(preference.displayValues).toList()
            val summary =
                preference.summary?.let { stringResource(it) }
                    ?: preference.summary(context, value)
            val selectedValues =
                remember {
                    val list = mutableStateSetOf<Any>()
                    list.addAll(value as List<Any>)
                    list
                }

            ClickPreference(
                title = title,
                summary = summary,
                onClick = {
                    dialogParams =
                        DialogParams(
                            title = title,
                            fromLongClick = false,
                            items =
                                values.mapIndexed { index, it ->
                                    val item = preference.allValues[index]!!
                                    DialogItem(
                                        headlineContent = { Text(it) },
                                        trailingContent = {
                                            Switch(
                                                checked = selectedValues.contains(item),
                                                onCheckedChange = {
                                                    // no-op
                                                },
                                            )
                                        },
                                        onClick = {
                                            if (selectedValues.contains(item)) {
                                                selectedValues.remove(item)
                                            } else {
                                                selectedValues.add(item)
                                            }
                                            onValueChange.invoke(selectedValues.toList() as T)
                                        },
                                    )
                                },
                        )
                },
                interactionSource = interactionSource,
                modifier = modifier,
            )
        }

        is StashSliderPreference -> {
            val summary =
                preference.summary(context, value)
                    ?: preference.summary?.let { stringResource(it) }
            SliderPreference(
                preference = preference,
                title = title,
                summary = summary,
                value = value as Int,
                onChange = { onValueChange(it as T) },
                summaryBelow = false,
                interactionSource = interactionSource,
                modifier = modifier,
            )
        }
    }

    AnimatedVisibility(dialogParams != null) {
        dialogParams?.let {
            DialogPopup(
                showDialog = true,
                title = it.title,
                dialogItems = it.items,
                onDismissRequest = { dialogParams = null },
                waitToLoad = false,
                dismissOnClick = false,
            )
        }
    }
    AnimatedVisibility(showPinDialog != null) {
        Dialog(
            onDismissRequest = { showPinDialog = null },
        ) {
            showPinDialog?.let { pref ->
                ConfigurePin(
                    onCancel = { showPinDialog = null },
                    onSubmit = { pin ->
                        onValueChange.invoke(pin as T)
                        showPinDialog = null
                    },
                    descriptionString = pref.description,
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
}

val PreferenceTitleStyle: TextStyle
    @Composable @ReadOnlyComposable
    get() = MaterialTheme.typography.titleSmall

val PreferenceSummaryStyle: TextStyle
    @Composable @ReadOnlyComposable
    get() = MaterialTheme.typography.bodySmall

@Composable
fun PreferenceTitle(
    title: String,
    modifier: Modifier = Modifier,
    color: Color = Color.Unspecified,
) {
    Text(
        text = title,
        style = PreferenceTitleStyle,
        color = color,
        modifier = modifier,
    )
}

@Composable
fun PreferenceSummary(
    summary: String?,
    modifier: Modifier = Modifier,
    color: Color = Color.Unspecified,
) {
    summary?.let {
        Text(
            text = it,
            style = PreferenceSummaryStyle,
            color = color,
            modifier = modifier,
        )
    }
}
