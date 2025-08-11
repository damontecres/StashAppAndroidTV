package com.github.damontecres.stashapp.ui.components.prefs

import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringArrayResource
import androidx.compose.ui.res.stringResource
import com.github.damontecres.stashapp.ui.components.DialogItem
import com.github.damontecres.stashapp.ui.components.DialogPopup
import com.github.damontecres.stashapp.ui.pages.DialogParams

@Composable
fun <T> ComposablePreference(
    preference: StashPreference<T>,
    value: T?,
    onValueChange: (T) -> Unit,
    modifier: Modifier = Modifier,
) {
    val context = LocalContext.current
    var dialogParams by remember { mutableStateOf<DialogParams?>(null) }

    when (preference) {
        is StashSwitchPreference ->
            SwitchPreference(
                title = stringResource(preference.title),
                value = value as Boolean,
                onClick = { onValueChange.invoke(!value as T) },
                summary = preference.summary(context, value),
                modifier = modifier,
            )

        is StashPinPreference -> {}
        is StashStringPreference -> {}
        is StashIntChoicePreference -> {
            val title = stringResource(preference.title)
            val values = stringArrayResource(preference.displayValues).toList()
            ClickPreference(
                title = title,
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
                                            onValueChange.invoke(preference.storeValues[index] as T)
                                        },
                                    )
                                },
                        )
                },
                modifier = modifier,
                summary = preference.summary(context, value as Int),
            )
        }

        is StashStringChoicePreference -> {
            val title = stringResource(preference.title)
            val values = stringArrayResource(preference.displayValues).toList()
            ClickPreference(
                title = title,
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
                                            onValueChange.invoke(preference.storeValues[index] as T)
                                        },
                                    )
                                },
                        )
                },
                modifier = modifier,
                summary = preference.summary(context, value as String),
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
