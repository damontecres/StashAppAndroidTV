package com.github.damontecres.stashapp.ui.pages

import android.Manifest
import android.app.Application
import android.content.Context
import android.util.Log
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.background
import androidx.compose.foundation.focusable
import androidx.compose.foundation.gestures.scrollBy
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.interaction.collectIsFocusedAsState
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.derivedStateOf
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.draw.blur
import androidx.compose.ui.input.key.Key
import androidx.compose.ui.input.key.KeyEventType
import androidx.compose.ui.input.key.key
import androidx.compose.ui.input.key.onKeyEvent
import androidx.compose.ui.input.key.type
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import androidx.tv.material3.MaterialTheme
import androidx.tv.material3.Text
import androidx.tv.material3.surfaceColorAtElevation
import com.github.damontecres.stashapp.di.services.NavigationManager
import com.github.damontecres.stashapp.ui.ComposeUiConfig
import com.github.damontecres.stashapp.ui.Material3AppTheme
import com.github.damontecres.stashapp.ui.compat.Button
import com.github.damontecres.stashapp.ui.components.BasicDialog
import com.github.damontecres.stashapp.ui.components.CircularProgress
import com.github.damontecres.stashapp.ui.util.DataLoadingState
import com.github.damontecres.stashapp.ui.util.ifElse
import com.github.damontecres.stashapp.util.Release
import com.github.damontecres.stashapp.util.StashCoroutineExceptionHandler
import com.github.damontecres.stashapp.util.UpdateChecker
import com.github.damontecres.stashapp.util.Version
import com.github.damontecres.stashapp.util.findActivity
import com.github.damontecres.stashapp.views.formatBytes
import com.mikepenz.markdown.m3.Markdown
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import org.koin.androidx.compose.koinViewModel
import org.koin.core.annotation.KoinViewModel
import java.io.InputStream
import java.io.OutputStream

@KoinViewModel
class UpdateViewModel(
    private val context: Application,
    private val updateChecker: UpdateChecker,
) : ViewModel(),
    DownloadCallback {
    private val _state =
        MutableStateFlow(UpdateAppState(currentVersion = UpdateChecker.getInstalledVersion(context)))
    val state: StateFlow<UpdateAppState> = _state

    fun init(
        context: Context,
        updateUrl: String,
    ) {
        _state.update { it.copy(release = DataLoadingState.Loading) }
        viewModelScope.launch(Dispatchers.IO) {
            try {
                val release = updateChecker.getLatestRelease(updateUrl)
                if (release != null) {
                    _state.update {
                        it.copy(
                            release = DataLoadingState.Success(release),
                            contentLength = -1,
                            bytesDownloaded = -1,
                        )
                    }
                }
            } catch (ex: Exception) {
                Log.e(TAG, "Exception during release check", ex)
                _state.update { it.copy(release = DataLoadingState.Error(ex)) }
            }
        }
    }

    private var downloadJob: Job? = null

    fun installRelease(
        context: Context,
        release: Release,
    ) {
        downloadJob =
            viewModelScope.launch(
                Dispatchers.IO,
            ) {
                _state.update { it.copy(downloading = true) }
                try {
                    updateChecker.installRelease(
                        context.findActivity()!!,
                        release,
                        this@UpdateViewModel,
                    )
                } catch (ex: Exception) {
                    Log.e(TAG, "Exception during install", ex)
                    _state.update { it.copy(release = DataLoadingState.Error(ex)) }
                }
                _state.update { it.copy(downloading = false) }
            }
    }

    fun cancelDownload() {
        viewModelScope.launch(Dispatchers.IO) {
            downloadJob?.cancel()
            _state.update {
                it.copy(
                    downloading = false,
                    contentLength = -1,
                    bytesDownloaded = -1,
                )
            }
        }
    }

    override fun contentLength(contentLength: Long) {
        _state.update { it.copy(contentLength = contentLength) }
    }

    override fun bytesDownloaded(bytes: Long) {
        _state.update { it.copy(bytesDownloaded = bytes) }
    }
}

interface DownloadCallback {
    fun contentLength(contentLength: Long)

    fun bytesDownloaded(bytes: Long)
}

suspend fun copyTo(
    input: InputStream,
    out: OutputStream,
    bufferSize: Int = 64 * 1024,
    callback: DownloadCallback,
): Long {
    var bytesCopied: Long = 0
    val buffer = ByteArray(bufferSize)
    var bytes = input.read(buffer)
    while (bytes >= 0) {
        out.write(buffer, 0, bytes)
        bytesCopied += bytes
        withContext(Dispatchers.Main) {
            callback.bytesDownloaded(bytesCopied)
        }
        bytes = input.read(buffer)
    }
    return bytesCopied
}

data class UpdateAppState(
    val release: DataLoadingState<Release> = DataLoadingState.Pending,
    val downloading: Boolean = false,
    val contentLength: Long = -1,
    val bytesDownloaded: Long = -1,
    val currentVersion: Version,
)

private const val TAG = "UpdateAppPage"

@Composable
fun UpdateAppPage(
    composeUiConfig: ComposeUiConfig,
    navigationManager: NavigationManager,
    modifier: Modifier = Modifier,
    viewModel: UpdateViewModel = koinViewModel(),
) {
    val context = LocalContext.current
    val state by viewModel.state.collectAsState()
    val release = state.release
    val currentVersion = state.currentVersion

    val isDownloading = state.downloading
    val contentLength by remember { derivedStateOf { state.contentLength } }
    val bytesDownloaded by remember { derivedStateOf { state.bytesDownloaded } }

    LaunchedEffect(Unit) {
        viewModel.init(context, composeUiConfig.preferences.updatePreferences.updateUrl)
    }
    var permissions by remember { mutableStateOf(UpdateChecker.hasPermissions(context)) }
    val launcher =
        rememberLauncherForActivityResult(
            ActivityResultContracts.RequestPermission(),
        ) { isGranted: Boolean ->
            if (isGranted) {
                permissions = true
            } else {
                // TODO
            }
        }
    when (val state = release) {
        is DataLoadingState.Error -> {
            Text(
                "Error: ${state.localizedMessage}",
                style = MaterialTheme.typography.displayLarge,
                color = MaterialTheme.colorScheme.onBackground,
                modifier = modifier,
            )
        }

        DataLoadingState.Loading,
        DataLoadingState.Pending,
        -> {
            CircularProgress(modifier)
        }

        is DataLoadingState.Success -> {
            InstallUpdatePageContent(
                currentVersion = currentVersion,
                release = state.data,
                onInstallRelease = {
                    if (!permissions) {
                        launcher.launch(Manifest.permission.WRITE_EXTERNAL_STORAGE)
                    } else {
                        viewModel.installRelease(context, state.data)
                    }
                },
                onCancel = {
                    navigationManager.goBack()
                },
                modifier =
                    modifier.ifElse(
                        isDownloading,
                        Modifier
                            .alpha(.5f)
                            .blur(16.dp),
                    ),
            )

            if (isDownloading) {
                DownloadDialog(
                    contentLength = contentLength,
                    bytesDownloaded = bytesDownloaded,
                    onDismissRequest = {
                        viewModel.cancelDownload()
                    },
                )
            }
        }
    }
}

@Composable
fun InstallUpdatePageContent(
    currentVersion: Version?,
    release: Release,
    onInstallRelease: () -> Unit,
    onCancel: () -> Unit,
    modifier: Modifier = Modifier,
) {
    Row(
        horizontalArrangement = Arrangement.spacedBy(16.dp),
        modifier = modifier,
    ) {
        val scrollAmount = 100f
        val columnState = rememberLazyListState()
        val scope = rememberCoroutineScope()

        fun scroll(reverse: Boolean = false) {
            scope.launch(StashCoroutineExceptionHandler()) {
                columnState.scrollBy(if (reverse) -scrollAmount else scrollAmount)
            }
        }
        val columnInteractionSource = remember { MutableInteractionSource() }
        val columnFocused by columnInteractionSource.collectIsFocusedAsState()
        val columnColor =
            if (columnFocused) {
                MaterialTheme.colorScheme.surfaceColorAtElevation(3.dp)
            } else {
                MaterialTheme.colorScheme.surfaceColorAtElevation(1.dp)
            }
        LazyColumn(
            state = columnState,
            contentPadding = PaddingValues(16.dp),
            verticalArrangement = Arrangement.spacedBy(8.dp),
            modifier =
                Modifier
                    .focusable(interactionSource = columnInteractionSource)
                    .fillMaxHeight()
                    .fillMaxWidth(.6f)
                    .background(
                        columnColor,
                        shape = RoundedCornerShape(16.dp),
                    ).onKeyEvent {
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
                        return@onKeyEvent false
                    },
        ) {
            item {
                Material3AppTheme {
                    Markdown(
                        (release.notes.joinToString("\n\n") + (release.body ?: ""))
                            .replace(
                                Regex("https://github.com/damontecres/\\w+/pull/(\\d+)"),
                                "#$1",
                            )
                            // Remove the last line for full changelog since its just a link
                            .replace(Regex("\\*\\*Full Changelog\\*\\*.*"), ""),
                    )
                }
            }
        }
        Column(
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.spacedBy(16.dp),
            modifier =
                Modifier
                    .fillMaxWidth()
                    .align(Alignment.CenterVertically)
                    .background(
                        MaterialTheme.colorScheme.surfaceColorAtElevation(1.dp),
                        shape = RoundedCornerShape(16.dp),
                    ).padding(16.dp),
        ) {
            Text(
                text = "Update available",
                style = MaterialTheme.typography.displaySmall,
                color = MaterialTheme.colorScheme.onSurface,
            )
            Text(
                text = "$currentVersion => " + release.version.toString(),
                style = MaterialTheme.typography.titleLarge,
                color = MaterialTheme.colorScheme.onSurface,
            )
            Button(
                onClick = onInstallRelease,
            ) {
                Text(
                    text = "Download and update",
                )
            }
            Button(
                onClick = onCancel,
            ) {
                Text(
                    text = "Cancel",
                )
            }
        }
    }
}

@Composable
fun DownloadDialog(
    contentLength: Long,
    bytesDownloaded: Long,
    onDismissRequest: () -> Unit,
) {
    val progress =
        if (contentLength > 0) {
            bytesDownloaded.toFloat() / contentLength
        } else {
            null
        }
    BasicDialog(
        onDismissRequest = onDismissRequest,
        elevation = 6.dp,
    ) {
        Column(
            verticalArrangement = Arrangement.spacedBy(8.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
            modifier = Modifier.padding(16.dp),
        ) {
            Row(
                horizontalArrangement = Arrangement.spacedBy(8.dp),
                verticalAlignment = Alignment.CenterVertically,
                modifier = Modifier,
            ) {
                Text(
                    text = "Downloading",
                    fontSize = 24.sp,
                    color = MaterialTheme.colorScheme.onSurface,
                )
                if (progress != null) {
                    CircularProgressIndicator(
                        progress = { progress },
                        color = MaterialTheme.colorScheme.border,
                        modifier =
                            Modifier
                                .size(48.dp)
                                .padding(8.dp),
                    )
                } else {
                    CircularProgressIndicator(
                        color = MaterialTheme.colorScheme.border,
                        modifier =
                            Modifier
                                .size(48.dp)
                                .padding(8.dp),
                    )
                }
            }
            if (progress != null) {
                val bytes = formatBytes(bytesDownloaded)
                val size = formatBytes(contentLength)
                Text(
                    text = "$bytes / $size",
                    fontSize = 18.sp,
                    color = MaterialTheme.colorScheme.onSurface,
                )
            }
        }
    }
}
