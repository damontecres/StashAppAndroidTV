package com.github.damontecres.stashapp.ui.pages

import android.app.ActivityManager
import android.content.Context.ACTIVITY_SERVICE
import android.media.MediaCodecList
import android.os.Build
import androidx.compose.foundation.focusable
import androidx.compose.foundation.gestures.scrollBy
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ColumnScope
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.mutableStateListOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.ui.Modifier
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.focus.focusRequester
import androidx.compose.ui.input.key.Key
import androidx.compose.ui.input.key.KeyEventType
import androidx.compose.ui.input.key.key
import androidx.compose.ui.input.key.onKeyEvent
import androidx.compose.ui.input.key.type
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.unit.dp
import androidx.preference.PreferenceManager
import androidx.tv.material3.MaterialTheme
import androidx.tv.material3.ProvideTextStyle
import androidx.tv.material3.Text
import com.github.damontecres.stashapp.BuildConfig
import com.github.damontecres.stashapp.StashApplication
import com.github.damontecres.stashapp.data.DataType
import com.github.damontecres.stashapp.data.room.PlaybackEffect
import com.github.damontecres.stashapp.playback.CodecSupport
import com.github.damontecres.stashapp.ui.ComposeUiConfig
import com.github.damontecres.stashapp.ui.components.TableRow
import com.github.damontecres.stashapp.ui.components.TableRowComposable
import com.github.damontecres.stashapp.ui.tryRequestFocus
import com.github.damontecres.stashapp.util.StashClient
import com.github.damontecres.stashapp.util.StashCoroutineExceptionHandler
import com.github.damontecres.stashapp.util.StashServer
import com.github.damontecres.stashapp.util.plugin.CompanionPlugin
import com.github.damontecres.stashapp.util.toReadableString
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

private fun tableRow(
    key: String,
    value: Any?,
) = if (key.contains("apikey", true) || key.contains("api key", true)) {
    TableRow.from(key, value?.toString()?.ifBlank { null }?.let { it.take(4) + "..." + it.takeLast(8) })
} else {
    TableRow.from(key, value?.toString())
}

@Composable
fun TableRowSmall(
    row: TableRow,
    modifier: Modifier = Modifier,
) {
    TableRowComposable(
        row,
        textStyle = MaterialTheme.typography.bodySmall.copy(color = MaterialTheme.colorScheme.onBackground),
        modifier = modifier,
    )
}

@Composable
fun DebugPage(
    server: StashServer,
    uiConfig: ComposeUiConfig,
    modifier: Modifier = Modifier,
) {
    val context = LocalContext.current
    val columnState = rememberLazyListState()
    val scope = rememberCoroutineScope()

    val focusRequester = remember { FocusRequester() }
    val prefManager = remember { PreferenceManager.getDefaultSharedPreferences(context) }
    val playbackEffects = remember { mutableStateListOf<PlaybackEffect>() }
    val logcat = remember { mutableStateListOf<String>() }

    LaunchedEffect(Unit) {
        val effects =
            withContext(Dispatchers.IO) {
                StashApplication
                    .getDatabase()
                    .playbackEffectsDao()
                    .getPlaybackEffects(server.url)
            }
        playbackEffects.addAll(effects)
    }
    LaunchedEffect(Unit) {
        val lines =
            withContext(Dispatchers.IO) {
                CompanionPlugin.getLogCatLines(true)
            }
        logcat.addAll(lines)
    }
    LaunchedEffect(Unit) { focusRequester.tryRequestFocus() }

    fun scroll(
        reverse: Boolean,
        scrollAmount: Float = 150f,
    ) {
        scope.launch(StashCoroutineExceptionHandler()) {
            columnState.scrollBy(if (reverse) -scrollAmount else scrollAmount)
        }
    }
    LazyColumn(
        state = columnState,
        verticalArrangement = Arrangement.spacedBy(16.dp),
        contentPadding = PaddingValues(8.dp),
        modifier =
            modifier
                .focusRequester(focusRequester)
                .focusable()
                .onKeyEvent {
                    if (it.type == KeyEventType.KeyUp) {
                        return@onKeyEvent false
                    }
                    if (it.key == Key.DirectionDown) {
                        scroll(false)
                        return@onKeyEvent true
                    }
                    if (it.key == Key.DirectionUp) {
                        scroll(true)
                        return@onKeyEvent true
                    }
                    if (it.key == Key.MediaFastForward) {
                        scroll(false, 1500f)
                        return@onKeyEvent true
                    }
                    if (it.key == Key.MediaRewind) {
                        scroll(true, 1500f)
                        return@onKeyEvent true
                    }
                    return@onKeyEvent false
                },
    ) {
        // StashPreferences
        item {
            Column {
                Text(
                    text = "StashPreferences",
                    style = MaterialTheme.typography.displayMedium,
                    color = MaterialTheme.colorScheme.onBackground,
                )
                Text(
                    text = uiConfig.preferences.toString(),
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onBackground,
                )
            }
        }
        // Preferences
        item {
            val rows =
                prefManager.all.let { prefs ->
                    prefs.keys.sorted().mapNotNull {
                        tableRow(it, prefs[it])
                    }
                }
            Column {
                Text(
                    text = "Preferences",
                    style = MaterialTheme.typography.displayMedium,
                    color = MaterialTheme.colorScheme.onBackground,
                )
                rows.forEach { row ->
                    TableRowSmall(row)
                }
            }
        }

        // Current server
        item {
            Column {
                Text(
                    text = "Current Server",
                    style = MaterialTheme.typography.displayMedium,
                    color = MaterialTheme.colorScheme.onBackground,
                )
            }
            listOfNotNull(
                tableRow("URL", server.url),
                tableRow("API Key", server.apiKey),
                tableRow("Resolved URL", StashClient.cleanServerUrl(server.url)),
                tableRow("Root URL", StashClient.getServerRoot(server.url)),
            ).forEach {
                TableRowSmall(it)
            }

            val rows =
                server.serverPreferences.preferences.all.let { prefs ->
                    prefs.keys.sorted().mapNotNull {
                        tableRow(it, prefs[it])
                    }
                }
            Column {
                Text(
                    text = "Server Preferences",
                    style = MaterialTheme.typography.displaySmall,
                    color = MaterialTheme.colorScheme.onBackground,
                )
                rows.forEach { row ->
                    TableRowSmall(row)
                }
                Text(
                    text = "Server default filters",
                    style = MaterialTheme.typography.displaySmall,
                    color = MaterialTheme.colorScheme.onBackground,
                )
                DataType.entries
                    .mapNotNull {
                        val filter = server.serverPreferences.getDefaultFilter(it)
                        val value = "findFilter=${filter.findFilter}\nobjectFilter=${filter.objectFilter?.toReadableString(true)}"
                        TableRow.from(it.name, value)
                    }.forEach {
                        TableRowComposable(
                            it,
                            textStyle = MaterialTheme.typography.bodySmall.copy(color = MaterialTheme.colorScheme.onBackground),
                        )
                    }
            }
            Column {
                Text(
                    text = "Image/Video Effects",
                    style = MaterialTheme.typography.displaySmall,
                    color = MaterialTheme.colorScheme.onBackground,
                )
                ProvideTextStyle(MaterialTheme.typography.bodySmall.copy(color = MaterialTheme.colorScheme.onBackground)) {
                    Row {
                        Text(
                            text = "Type",
                            modifier =
                                Modifier
                                    .weight(1f)
                                    .padding(4.dp),
                        )
                        Text(
                            text = "ID",
                            modifier =
                                Modifier
                                    .weight(1f)
                                    .padding(4.dp),
                        )
                        Text(
                            text = "Filter",
                            modifier =
                                Modifier
                                    .weight(10f)
                                    .padding(4.dp),
                        )
                    }
                    playbackEffects.forEach {
                        Row {
                            Text(
                                text = it.dataType.name,
                                modifier =
                                    Modifier
                                        .weight(1f)
                                        .padding(4.dp),
                            )
                            Text(
                                text = it.id,
                                modifier =
                                    Modifier
                                        .weight(1f)
                                        .padding(4.dp),
                            )
                            Text(
                                text = it.videoFilter.toString(),
                                modifier =
                                    Modifier
                                        .weight(10f)
                                        .padding(4.dp),
                            )
                        }
                    }
                }
            }
        }

        // Other
        item {
            Column {
                Text(
                    text = "Other info",
                    style = MaterialTheme.typography.displayMedium,
                    color = MaterialTheme.colorScheme.onBackground,
                )
                val activityManager = remember { context.getSystemService(ACTIVITY_SERVICE) as ActivityManager }
                listOfNotNull(
                    tableRow("User-Agent", StashClient.createUserAgent(context)),
                    tableRow("Build type", BuildConfig.BUILD_TYPE),
                    tableRow("isLowRamDevice", activityManager.isLowRamDevice.toString()),
                ).forEach {
                    TableRowSmall(it)
                }
            }
        }

        // Codecs
        item {
            val codecs =
                remember { CodecSupport.getSupportedCodecs(uiConfig.preferences.playbackPreferences) }
            val rows =
                listOfNotNull(
                    tableRow("Video codecs", codecs.videoCodecs.sorted().joinToString(", ")),
                    tableRow("Audio codecs", codecs.audioCodecs.sorted().joinToString(", ")),
                    tableRow("Container formats", codecs.containers.sorted().joinToString(", ")),
                )
            Column {
                Text(
                    text = "Codecs",
                    style = MaterialTheme.typography.displayMedium,
                    color = MaterialTheme.colorScheme.onBackground,
                )
                rows.forEach { TableRowSmall(it) }
                Text(
                    text = "Codecs",
                    style = MaterialTheme.typography.displaySmall,
                    color = MaterialTheme.colorScheme.onBackground,
                )
                Decoders()
            }
        }

        // Logcat
        item {
            Column {
                Text(
                    text = "Logcat",
                    style = MaterialTheme.typography.displayMedium,
                    color = MaterialTheme.colorScheme.onBackground,
                )
                logcat.forEach {
                    Text(
                        text = it,
                        style = MaterialTheme.typography.bodySmall.copy(fontFamily = FontFamily.Monospace),
                        color = MaterialTheme.colorScheme.onBackground,
                    )
                }
            }
        }
    }
}

@Composable
private fun ColumnScope.Decoders() {
    val nameModifier =
        Modifier
            .weight(1.5f)
            .padding(4.dp)
    val textModifier =
        Modifier
            .weight(1f)
            .padding(4.dp)
    val smallModifier =
        Modifier
            .weight(.5f)
            .padding(4.dp)
    Row {
        ProvideTextStyle(MaterialTheme.typography.bodySmall.copy(color = MaterialTheme.colorScheme.onBackground)) {
            Text(
                text = "Name",
                modifier = nameModifier,
            )
            Text(
                text = "Type",
                modifier = textModifier,
            )
            Text(
                text = "SW",
                modifier = smallModifier,
            )
            Text(
                text = "HW Acc",
                modifier = smallModifier,
            )
            Text(
                text = "Profiles",
                modifier = textModifier,
            )
            Text(
                text = "Bit rates",
                modifier = textModifier,
            )
            Text(
                text = "Frame rates",
                modifier = textModifier,
            )
            Text(
                text = "Widths",
                modifier = textModifier,
            )
            Text(
                text = "heights",
                modifier = textModifier,
            )
        }
    }

    val androidCodecs = MediaCodecList(MediaCodecList.REGULAR_CODECS)
    androidCodecs.codecInfos
        .filter { !it.isEncoder }
        .flatMap { codecInfo ->
            codecInfo.supportedTypes
                .map { type ->
                    val caps = codecInfo.getCapabilitiesForType(type)
                    val videoCaps =
                        caps.videoCapabilities?.let {
                            arrayOf(
                                it.bitrateRange.toString(),
                                it.supportedFrameRates.toString(),
                                it.supportedWidths.toString(),
                                it.supportedHeights.toString(),
                            )
                        } ?: arrayOf("", "", "", "")
                    val profiles =
                        caps.profileLevels.joinToString("\n") {
                            "profile=0x${it.profile.toString(16)}, level=0x${
                                it.level.toString(
                                    16,
                                )
                            }"
                        }
                    val isSoftware =
                        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
                            codecInfo.isSoftwareOnly
                        } else {
                            null
                        }?.toString()
                    val isHardware =
                        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
                            codecInfo.isHardwareAccelerated
                        } else {
                            null
                        }?.toString()

                    listOf(
                        codecInfo.name,
                        type,
                        isSoftware,
                        isHardware,
                        profiles,
                        *videoCaps,
                    )
                }
        }.sortedWith(compareBy({ it[1] }, { it[0] }))
        .forEach { row ->
            Row {
                ProvideTextStyle(MaterialTheme.typography.bodySmall.copy(color = MaterialTheme.colorScheme.onBackground)) {
                    row.forEachIndexed { index, value ->
                        Text(
                            text = value ?: "",
                            modifier =
                                when (index) {
                                    0 -> nameModifier
                                    2, 3 -> smallModifier
                                    else -> textModifier
                                },
                        )
                    }
                }
            }
        }
}
