package com.github.damontecres.stashapp.ui.components.prefs

import androidx.compose.runtime.Composable
import androidx.compose.runtime.ReadOnlyComposable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringArrayResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.TextStyle
import androidx.tv.material3.MaterialTheme
import androidx.tv.material3.Text
import com.github.damontecres.stashapp.navigation.Destination
import com.github.damontecres.stashapp.navigation.NavigationManager
import com.github.damontecres.stashapp.ui.components.DialogItem
import com.github.damontecres.stashapp.ui.components.DialogPopup
import com.github.damontecres.stashapp.ui.pages.DialogParams
import com.github.damontecres.stashapp.util.StashCoroutineExceptionHandler
import com.github.damontecres.stashapp.util.StashServer
import com.github.damontecres.stashapp.util.testStashConnection
import kotlinx.coroutines.launch

@Composable
fun <T> ComposablePreference(
    server: StashServer,
    navigationManager: NavigationManager,
    preference: StashPreference<T>,
    value: T?,
    onValueChange: (T) -> Unit,
    modifier: Modifier = Modifier,
) {
    val context = LocalContext.current
    val scope = rememberCoroutineScope()

    var dialogParams by remember { mutableStateOf<DialogParams?>(null) }
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
                modifier = modifier,
            )
        }

        StashPreference.ManageServers ->
            ClickPreference(
                title = title,
                onClick = {
                    navigationManager.navigate(Destination.ManageServers(true))
                },
                summary = preference.summary(context, value),
                modifier = modifier,
            )

        is StashClickablePreference ->
            ClickPreference(
                title = title,
                onClick = {},
                summary = preference.summary(context, value),
                modifier = modifier,
            )

        is StashSwitchPreference ->
            SwitchPreference(
                title = title,
                value = value as Boolean,
                onClick = { onValueChange.invoke(!value as T) },
                summary = preference.summary(context, value),
                modifier = modifier,
            )

        is StashPinPreference -> {}
        is StashStringPreference -> {}

        is StashChoicePreference -> {
            val values = stringArrayResource(preference.displayValues).toList()
            val summary =
                preference.summary(context, value) ?: preference
                    .valueToIndex(value as T)
                    ?.let { values[it] }
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
                                            onValueChange(preference.indexToValue(index) as T)
                                        },
                                    )
                                },
                        )
                },
                modifier = modifier,
            )
        }

        is StashIntChoicePreference -> {
            val values = stringArrayResource(preference.displayValues).toList()
            val summary =
                preference.summary(context, value as Int) ?: preference
                    .valueToIndex(value)
                    ?.let { values[it] }
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
                                            onValueChange(preference.indexToValue(index) as T)
                                        },
                                    )
                                },
                        )
                },
                modifier = modifier,
            )
        }

        is StashStringChoicePreference -> {
            val values = stringArrayResource(preference.displayValues).toList()
            val summary =
                preference.summary(context, value as String) ?: preference
                    .valueToIndex(value)
                    ?.let { values[it] }
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
                                            onValueChange(preference.indexToValue(index) as T)
                                        },
                                    )
                                },
                        )
                },
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
                modifier = modifier,
            )
        }
    }

    dialogParams?.let {
        DialogPopup(
            showDialog = true,
            title = it.title,
            dialogItems = it.items,
            onDismissRequest = { dialogParams = null },
            waitToLoad = false,
        )
    }
}

val PreferenceTitleStyle: TextStyle
    @Composable @ReadOnlyComposable
    get() = MaterialTheme.typography.titleLarge

val PreferenceSummaryStyle: TextStyle
    @Composable @ReadOnlyComposable
    get() = MaterialTheme.typography.bodyMedium

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
