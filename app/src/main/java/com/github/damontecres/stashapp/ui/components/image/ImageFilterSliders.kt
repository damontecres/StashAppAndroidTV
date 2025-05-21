package com.github.damontecres.stashapp.ui.components.image

import android.view.Gravity
import androidx.annotation.StringRes
import androidx.compose.foundation.background
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.wrapContentSize
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalView
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import androidx.compose.ui.window.DialogWindowProvider
import androidx.tv.material3.Button
import androidx.tv.material3.MaterialTheme
import androidx.tv.material3.Text
import com.github.damontecres.stashapp.R
import com.github.damontecres.stashapp.data.VideoFilter
import com.github.damontecres.stashapp.ui.AppTheme
import com.github.damontecres.stashapp.ui.ComposeUiConfig
import com.github.damontecres.stashapp.ui.components.SliderBar

@Composable
fun ImageFilterSliders(
    filter: VideoFilter,
    showVideoOptions: Boolean,
    showSaveButton: Boolean,
    onChange: (VideoFilter) -> Unit,
    onClickSave: () -> Unit,
    modifier: Modifier = Modifier,
) {
    LazyColumn(
        verticalArrangement = Arrangement.spacedBy(8.dp),
        modifier = modifier,
    ) {
        item {
            SliderBarRow(
                title = R.string.stashapp_effect_filters_brightness,
                value = filter.brightness,
                min = 0,
                max = 200,
                onChange = { onChange.invoke(filter.copy(brightness = it)) },
                valueFormater = { "$it%" },
            )
        }
        item {
            SliderBarRow(
                title = R.string.stashapp_effect_filters_contrast,
                value = filter.contrast,
                min = 0,
                max = 200,
                onChange = { onChange.invoke(filter.copy(contrast = it)) },
                valueFormater = { "$it%" },
            )
        }
        item {
            SliderBarRow(
                title = R.string.stashapp_effect_filters_saturation,
                value = filter.saturation,
                min = 0,
                max = 200,
                onChange = { onChange.invoke(filter.copy(saturation = it)) },
                valueFormater = { "$it%" },
            )
        }
        if (showVideoOptions) {
            item {
                SliderBarRow(
                    title = R.string.stashapp_effect_filters_hue,
                    value = filter.hue,
                    min = 0,
                    max = 360,
                    onChange = { onChange.invoke(filter.copy(hue = it)) },
                    valueFormater = { "$it\u00b0" },
                )
            }
        }
        item {
            SliderBarRow(
                title = R.string.stashapp_effect_filters_red,
                value = filter.red,
                min = 0,
                max = 200,
                onChange = { onChange.invoke(filter.copy(red = it)) },
                valueFormater = { "$it%" },
                color = Color.Red.copy(alpha = .8f),
            )
        }
        item {
            SliderBarRow(
                title = R.string.stashapp_effect_filters_green,
                value = filter.green,
                min = 0,
                max = 200,
                onChange = { onChange.invoke(filter.copy(green = it)) },
                valueFormater = { "$it%" },
                color = Color.Green.copy(alpha = .8f),
            )
        }
        item {
            SliderBarRow(
                title = R.string.stashapp_effect_filters_blue,
                value = filter.blue,
                min = 0,
                max = 200,
                onChange = { onChange.invoke(filter.copy(blue = it)) },
                valueFormater = { "$it%" },
                color = Color.Blue.copy(alpha = .8f),
            )
        }
        if (showVideoOptions) {
            item {
                SliderBarRow(
                    title = R.string.stashapp_effect_filters_blur,
                    value = filter.blur,
                    min = 0,
                    max = 250,
                    onChange = { onChange.invoke(filter.copy(blur = it)) },
                    valueFormater = { "${it}px" },
                )
            }
        }
        item {
            Box(
                modifier = Modifier.fillMaxWidth(),
                contentAlignment = Alignment.Center,
            ) {
                Row(
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                    modifier = Modifier,
                ) {
                    if (showSaveButton) {
                        Button(
                            onClick = onClickSave,
                        ) {
                            Text(text = stringResource(R.string.stashapp_actions_save))
                        }
                    }
                    Button(
                        onClick = { onChange(VideoFilter()) },
                    ) {
                        Text(text = stringResource(R.string.stashapp_effect_filters_reset_filters))
                    }
                }
            }
        }
    }
}

@Composable
fun SliderBarRow(
    @StringRes title: Int,
    value: Int,
    min: Int,
    max: Int,
    onChange: (Int) -> Unit,
    valueFormater: (Int) -> String,
    modifier: Modifier = Modifier,
    interactionSource: MutableInteractionSource = remember { MutableInteractionSource() },
    interval: Int = 1,
    color: Color = MaterialTheme.colorScheme.border,
) {
    Row(
        horizontalArrangement = Arrangement.spacedBy(8.dp),
        verticalAlignment = Alignment.CenterVertically,
        modifier = modifier,
    ) {
        Text(
            text = stringResource(title),
            color = MaterialTheme.colorScheme.onSurface,
            modifier = Modifier.width(88.dp),
        )
        SliderBar(
            value = value,
            min = min,
            max = max,
            interval = interval,
            onChange = onChange,
            color = color,
            interactionSource = interactionSource,
            modifier = Modifier.weight(1f),
        )
        Text(
            text = valueFormater(value),
            color = MaterialTheme.colorScheme.onSurface,
            modifier = Modifier.width(48.dp),
        )
    }
}

@Composable
fun ImageFilterDialog(
    filter: VideoFilter,
    showVideoOptions: Boolean,
    uiConfig: ComposeUiConfig,
    onChange: (VideoFilter) -> Unit,
    onClickSave: () -> Unit,
    onDismissRequest: () -> Unit,
    modifier: Modifier = Modifier,
) {
    Dialog(
        onDismissRequest = onDismissRequest,
        properties = DialogProperties(usePlatformDefaultWidth = true),
    ) {
        val dialogWindowProvider = LocalView.current.parent as? DialogWindowProvider
        dialogWindowProvider?.window?.let { window ->
            window.setGravity(Gravity.TOP or Gravity.END)
            window.setDimAmount(0f)
        }

        Box(
            modifier =
                modifier
                    .wrapContentSize()
                    .padding(8.dp)
                    .background(MaterialTheme.colorScheme.secondaryContainer.copy(alpha = .4f)),
        ) {
            ImageFilterSliders(
                filter = filter,
                showVideoOptions = showVideoOptions,
                showSaveButton = uiConfig.persistVideoFilters,
                onChange = onChange,
                onClickSave = onClickSave,
                modifier = Modifier.padding(8.dp),
            )
        }
    }
}

@Preview
@Composable
private fun ImageFilterSlidersPreview() {
    AppTheme {
        ImageFilterSliders(
            filter = VideoFilter(),
            showVideoOptions = true,
            onChange = {},
            onClickSave = {},
            showSaveButton = true,
            modifier = Modifier.padding(8.dp),
        )
    }
}
