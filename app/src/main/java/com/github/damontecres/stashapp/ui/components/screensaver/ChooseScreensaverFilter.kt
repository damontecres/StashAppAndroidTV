package com.github.damontecres.stashapp.ui.components.screensaver

import android.content.Context
import android.util.Log
import android.widget.Toast
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.drawBehind
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.unit.dp
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.tv.material3.MaterialTheme
import androidx.tv.material3.Text
import androidx.tv.material3.surfaceColorAtElevation
import com.github.damontecres.stashapp.StashApplication
import com.github.damontecres.stashapp.api.fragment.SavedFilter
import com.github.damontecres.stashapp.api.type.SortDirectionEnum
import com.github.damontecres.stashapp.data.DataType
import com.github.damontecres.stashapp.data.SortAndDirection
import com.github.damontecres.stashapp.data.SortOption
import com.github.damontecres.stashapp.suppliers.FilterArgs
import com.github.damontecres.stashapp.suppliers.toFilterArgs
import com.github.damontecres.stashapp.ui.ComposeUiConfig
import com.github.damontecres.stashapp.ui.compat.ListItem
import com.github.damontecres.stashapp.ui.components.DialogItem
import com.github.damontecres.stashapp.ui.components.DialogPopup
import com.github.damontecres.stashapp.ui.components.filter.CreateFilterContent
import com.github.damontecres.stashapp.util.FilterParser
import com.github.damontecres.stashapp.util.QueryEngine
import com.github.damontecres.stashapp.util.StashParcelable
import com.github.damontecres.stashapp.util.StashServer
import com.github.damontecres.stashapp.util.launchDefault
import com.github.damontecres.stashapp.util.launchIO
import com.github.damontecres.stashapp.util.showToastOnMain
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.serialization.ExperimentalSerializationApi
import kotlinx.serialization.Serializable
import kotlinx.serialization.encodeToByteArray
import java.io.File

private const val TAG = "ChooseScreensaver"

@OptIn(ExperimentalSerializationApi::class)
class ChooseScreensaverFilterViewModel : ViewModel() {
    private val context = StashApplication.getApplication()
    private val server = StashServer.requireCurrentServer()

    val state = MutableStateFlow(ChooseScreensaverFilterState())

    init {
        viewModelScope.launchIO {
            val file = getScreensaverFile(context, server)
            if (file.exists()) {
                try {
                    val filter = ScreensaverFilter.read(file)
                    if (filter.savedFilterId == null) {
                        state.update {
                            it.copy(
                                filter = filter.filter,
                                savedFilterId = filter.savedFilterId,
                            )
                        }
                    }
                    state.update {
                        it.copy(filterConfigured = true)
                    }
                } catch (ex: Exception) {
                    Log.e(TAG, "Error reading filter", ex)
                    showToastOnMain(context, "Error reading filter: ${ex.localizedMessage}", Toast.LENGTH_LONG)
                }
            }
            val savedFilters = QueryEngine(server).getSavedFilters(DataType.IMAGE)
            state.update {
                val savedFilter = it.savedFilterId?.let { id -> savedFilters.firstOrNull { it.id == id } }
                it.copy(
                    savedFilters = savedFilters,
                    savedFilter = savedFilter,
                )
            }
        }
    }

    @OptIn(ExperimentalSerializationApi::class)
    fun saveFilter(
        filter: FilterArgs,
        filterId: String? = null,
    ) {
        viewModelScope.launchIO {
            try {
                val toSave = ScreensaverFilter(filterId, filter)
                val file = getScreensaverFile(context, server)
                file.parentFile!!.mkdirs()
                toSave.write(file)
                val savedFilter = filterId?.let { id -> state.value.savedFilters.firstOrNull { it.id == id } }
                state.update {
                    it.copy(
                        filter = filter,
                        filterConfigured = true,
                        savedFilterId = filterId,
                        savedFilter = savedFilter,
                    )
                }
            } catch (ex: Exception) {
                Log.e(TAG, "Error saving filter", ex)
                showToastOnMain(context, "Error saving filter: ${ex.localizedMessage}", Toast.LENGTH_LONG)
            }
        }
    }

    fun saveFilter(savedFilter: SavedFilter) {
        viewModelScope.launchDefault {
            val filterArgs = savedFilter.toFilterArgs(FilterParser(server.version))
            saveFilter(filterArgs, savedFilter.id)
        }
    }

    fun deleteFilter() {
        viewModelScope.launchIO {
            val file = getScreensaverFile(context, server)
            file.delete()
            state.update {
                it.copy(
                    filter = ScreensaverFilter.makeDefault().filter,
                    filterConfigured = false,
                )
            }
        }
    }
}

fun getScreensaverFile(
    context: Context,
    server: StashServer,
): File {
    val filename = server.serverPreferences.serverKey
    val parentDir = File(context.filesDir, "screensaver")
    return File(parentDir, filename)
}

data class ChooseScreensaverFilterState(
    val savedFilters: List<SavedFilter> = emptyList(),
    val filter: FilterArgs = ScreensaverFilter.makeDefault().filter,
    val filterConfigured: Boolean = false,
    val savedFilterId: String? = null,
    val savedFilter: SavedFilter? = null,
)

@Serializable
data class ScreensaverFilter(
    val savedFilterId: String?,
    val filter: FilterArgs,
) {
    companion object {
        fun makeDefault() =
            ScreensaverFilter(
                null,
                FilterArgs(DataType.IMAGE).with(
                    SortAndDirection(
                        SortOption.Random,
                        SortDirectionEnum.ASC,
                    ),
                ),
            )

        @OptIn(ExperimentalSerializationApi::class)
        suspend fun read(file: File): ScreensaverFilter {
            val bytes =
                file.inputStream().use {
                    it.readBytes()
                }
            return StashParcelable.decodeFromByteArray(serializer(), bytes)
        }
    }

    @OptIn(ExperimentalSerializationApi::class)
    suspend fun write(file: File) {
        val bytes = StashParcelable.encodeToByteArray(this)
        file.outputStream().use { it.write(bytes) }
    }
}

@Composable
fun ChooseScreensaverFilterDialog(
    uiConfig: ComposeUiConfig,
    onDismissRequest: () -> Unit,
) {
    Dialog(
        onDismissRequest = onDismissRequest,
        properties = DialogProperties(usePlatformDefaultWidth = true),
    ) {
        val elevatedContainerColor = MaterialTheme.colorScheme.surfaceColorAtElevation(1.dp)
        Box(
            modifier =
                Modifier
                    .graphicsLayer {
                        this.clip = true
                        this.shape = RoundedCornerShape(28.0.dp)
                    }.drawBehind { drawRect(color = elevatedContainerColor) }
                    .padding(PaddingValues(24.dp)),
        ) {
            ChooseScreensaverFilter(
                uiConfig = uiConfig,
                modifier = Modifier,
            )
        }
    }
}

@Composable
fun ChooseScreensaverFilter(
    uiConfig: ComposeUiConfig,
    modifier: Modifier = Modifier,
    viewModel: ChooseScreensaverFilterViewModel = viewModel(),
) {
    val state by viewModel.state.collectAsState()

    var showSavedFilterDialog by remember { mutableStateOf(false) }
    var showCreateFilterDialog by remember { mutableStateOf(false) }

    Column(modifier = modifier) {
        Text(
            text = "Choose filter",
            color = MaterialTheme.colorScheme.onSurface,
            style = MaterialTheme.typography.titleLarge,
        )
        LazyColumn {
            item {
                ListItem(
                    selected = false,
                    enabled = state.savedFilters.isNotEmpty(),
                    headlineContent = {
                        Text("Use saved filter")
                    },
                    supportingContent =
                        if (state.savedFilters.isEmpty()) {
                            {
                                Text("No saved filters")
                            }
                        } else {
                            null
                        },
                    onClick = {
                        showSavedFilterDialog = true
                    },
                )
            }
            item {
                ListItem(
                    selected = false,
                    headlineContent = {
                        Text("Create filter")
                    },
                    onClick = {
                        showCreateFilterDialog = true
                    },
                )
            }
            item {
                ListItem(
                    selected = false,
                    enabled = state.filterConfigured,
                    headlineContent = {
                        Text("Clear filter")
                    },
                    supportingContent =
                        if (state.filterConfigured) {
                            {
                                val name =
                                    if (state.savedFilter != null) {
                                        state.savedFilter?.name ?: ""
                                    } else {
                                        "Custom filter"
                                    }
                                Text(name)
                            }
                        } else {
                            null
                        },
                    onClick = {
                        viewModel.deleteFilter()
                    },
                )
            }
        }
    }
    DialogPopup(
        showDialog = showSavedFilterDialog,
        title = "Choose saved filter",
        dialogItems =
            remember(state) {
                state.savedFilters.map {
                    DialogItem(it.name) { viewModel.saveFilter(it) }
                }
            },
        onDismissRequest = { showSavedFilterDialog = false },
        waitToLoad = false,
    )
    if (showCreateFilterDialog) {
        Dialog(
            onDismissRequest = { showCreateFilterDialog = false },
            properties = DialogProperties(usePlatformDefaultWidth = false),
        ) {
            val elevatedContainerColor = MaterialTheme.colorScheme.background
            Box(
                modifier =
                    Modifier
                        .fillMaxSize()
                        .drawBehind { drawRect(color = elevatedContainerColor) }
                        .padding(PaddingValues(24.dp)),
            ) {
                CreateScreensaverFilter(
                    uiConfig = uiConfig,
                    onSubmit = {
                        showCreateFilterDialog = false
                        viewModel.saveFilter(it)
                    },
                    initialFilter = state.filter,
                    modifier = Modifier.fillMaxSize(),
                )
            }
        }
    }
}

@Composable
fun CreateScreensaverFilter(
    uiConfig: ComposeUiConfig,
    onSubmit: (FilterArgs) -> Unit,
    initialFilter: FilterArgs,
    modifier: Modifier = Modifier,
) {
    CreateFilterContent(
        uiConfig = uiConfig,
        dataType = DataType.IMAGE,
        initialFilter = initialFilter,
        saveEnabled = false,
        onSubmit = { _, filter -> onSubmit.invoke(filter) },
        modifier = modifier,
    )
}
