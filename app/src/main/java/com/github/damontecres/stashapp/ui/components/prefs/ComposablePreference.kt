package com.github.damontecres.stashapp.ui.components.prefs

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.foundation.background
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.runtime.Composable
import androidx.compose.runtime.ReadOnlyComposable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.mutableStateSetOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringArrayResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import androidx.compose.ui.window.Dialog
import androidx.core.content.edit
import androidx.preference.PreferenceManager
import androidx.tv.material3.MaterialTheme
import androidx.tv.material3.Switch
import androidx.tv.material3.Text
import coil3.imageLoader
import com.github.damontecres.stashapp.R
import com.github.damontecres.stashapp.SettingsFragment
import com.github.damontecres.stashapp.navigation.NavigationManager
import com.github.damontecres.stashapp.ui.compat.Button
import com.github.damontecres.stashapp.ui.components.DialogItem
import com.github.damontecres.stashapp.ui.components.DialogPopup
import com.github.damontecres.stashapp.ui.components.EditTextBox
import com.github.damontecres.stashapp.ui.components.server.ConfigurePin
import com.github.damontecres.stashapp.ui.pages.DialogParams
import com.github.damontecres.stashapp.util.Constants
import com.github.damontecres.stashapp.util.MutationEngine
import com.github.damontecres.stashapp.util.StashCoroutineExceptionHandler
import com.github.damontecres.stashapp.util.StashServer
import com.github.damontecres.stashapp.util.isNotNullOrBlank
import com.github.damontecres.stashapp.util.plugin.CompanionPlugin
import com.github.damontecres.stashapp.util.testStashConnection
import com.github.damontecres.stashapp.views.formatBytes
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
    var showStringDialog by remember { mutableStateOf<StringInput?>(null) }

    val title = stringResource(preference.title)

    val onClick: () -> Unit = {
        scope.launch(StashCoroutineExceptionHandler()) {
            when (preference) {
                StashPreference.CurrentServer -> {
                    testStashConnection(context, true, server.apolloClient)
                }

                StashPreference.SendLogs -> {
                    CompanionPlugin.sendLogCat(context, server, false)
                }

                StashPreference.CacheClear -> SettingsFragment.clearCaches(context)

                StashPreference.TriggerScan -> MutationEngine(server).triggerScan()

                StashPreference.TriggerGenerate -> MutationEngine(server).triggerGenerate()

                else -> {}
            }
        }
    }
    val onLongClick: () -> Unit = {
        scope.launch(StashCoroutineExceptionHandler()) {
            when (preference) {
                StashPreference.SendLogs -> {
                    CompanionPlugin.sendLogCat(context, server, true)
                }

                else -> null
            }
        }
    }

    when (preference) {
        StashPreference.CurrentServer ->
            ClickPreference(
                title = title,
                onClick = onClick,
                summary = server.url,
                interactionSource = interactionSource,
                modifier = modifier,
            )

        StashPreference.NetworkCache -> {
            preference as StashSliderPreference
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
                heightAdjustment = 16.dp,
            ) {
                val size = remember { formatBytes(Constants.getNetworkCache(context).size()) }
                Text(
                    text = "Using $size",
                    modifier = Modifier.fillMaxWidth(),
                )
            }
        }

        StashPreference.ImageDiskCache -> {
            preference as StashSliderPreference
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
                heightAdjustment = 32.dp,
            ) {
                val memoryUsed =
                    remember { formatBytes(context.imageLoader.memoryCache?.size ?: 0L) }
                val memoryMax =
                    remember { formatBytes(context.imageLoader.memoryCache?.maxSize ?: 0L) }
                val diskUsed =
                    remember { formatBytes(context.imageLoader.diskCache?.size ?: 0L) }
                Column(
                    verticalArrangement = Arrangement.spacedBy(4.dp),
                ) {
                    Text(
                        text = "Memory used: $memoryUsed / $memoryMax",
                        modifier = Modifier.fillMaxWidth(),
                    )
                    Text(
                        text = "Disk used: $diskUsed",
                        modifier = Modifier.fillMaxWidth(),
                    )
                }
            }
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
                onClick = onClick,
                onLongClick = onLongClick,
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
        is StashStringPreference ->
            ClickPreference(
                title = title,
                onClick = {
                    showStringDialog =
                        StringInput(
                            title = title,
                            value = value as String?,
                            keyboardOptions =
                                KeyboardOptions(
                                    autoCorrectEnabled = false,
                                    keyboardType =
                                        if (preference == StashPreference.UpdateUrl) {
                                            KeyboardType.Uri
                                        } else {
                                            KeyboardType.Unspecified
                                        },
                                    imeAction = ImeAction.Done,
                                ),
                            onSubmit = { input ->
                                onValueChange.invoke(input as T)
                                showStringDialog = null
                            },
                        )
                },
                summary =
                    preference.summary(context, value)
                        ?: preference.summary?.let { stringResource(it) },
                interactionSource = interactionSource,
                modifier = modifier,
            )

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
    AnimatedVisibility(showStringDialog != null) {
        showStringDialog?.let {
            var mutableValue by remember { mutableStateOf(it.value ?: "") }
            val onDone = {
                it.onSubmit.invoke(mutableValue)
            }
            Dialog(
                onDismissRequest = { showStringDialog = null },
            ) {
                Box(
                    contentAlignment = Alignment.Center,
                    modifier =
                        Modifier
                            .padding(16.dp)
                            .background(
                                color = MaterialTheme.colorScheme.secondaryContainer,
                                shape = RoundedCornerShape(8.dp),
                            ),
                ) {
                    Column(
                        verticalArrangement = Arrangement.spacedBy(8.dp),
                        modifier =
                            Modifier
                                .padding(16.dp),
                    ) {
                        Text(
                            text = it.title,
                            style = MaterialTheme.typography.titleLarge,
                            color = MaterialTheme.colorScheme.onSecondaryContainer,
                            modifier = Modifier,
                        )
                        EditTextBox(
                            value = mutableValue,
                            onValueChange = { mutableValue = it },
                            keyboardOptions = it.keyboardOptions.copy(imeAction = ImeAction.Done),
                            keyboardActions =
                                KeyboardActions(
                                    onDone = { onDone.invoke() },
                                ),
                            leadingIcon = null,
                            isInputValid = { true },
                            modifier = Modifier.fillMaxWidth(),
                        )

                        Row(
                            horizontalArrangement = Arrangement.SpaceAround,
                            modifier = Modifier.fillMaxWidth(),
                        ) {
                            Button(
                                onClick = { showStringDialog = null },
                                modifier = Modifier,
                            ) {
                                Text(
                                    text = stringResource(R.string.stashapp_actions_cancel),
                                )
                            }
                            Button(
                                onClick = onDone,
                                modifier = Modifier,
                            ) {
                                Text(
                                    text = stringResource(R.string.stashapp_actions_save),
                                )
                            }
                        }
                    }
                }
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

private data class StringInput(
    val title: String,
    val value: String?,
    val keyboardOptions: KeyboardOptions,
    val onSubmit: (String) -> Unit,
)
