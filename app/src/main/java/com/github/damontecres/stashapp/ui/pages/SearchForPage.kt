package com.github.damontecres.stashapp.ui.pages

import android.content.Context
import android.util.Log
import android.widget.Toast
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.drawBehind
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import androidx.preference.PreferenceManager
import androidx.tv.material3.MaterialTheme
import androidx.tv.material3.Text
import androidx.tv.material3.surfaceColorAtElevation
import com.apollographql.apollo.api.Optional
import com.github.damontecres.stashapp.R
import com.github.damontecres.stashapp.SearchForFragment
import com.github.damontecres.stashapp.SearchForFragment.Companion.DATA_TYPE_SUGGESTIONS
import com.github.damontecres.stashapp.StashApplication
import com.github.damontecres.stashapp.api.fragment.StashData
import com.github.damontecres.stashapp.api.type.CriterionModifier
import com.github.damontecres.stashapp.api.type.FindFilterType
import com.github.damontecres.stashapp.api.type.GalleryFilterType
import com.github.damontecres.stashapp.api.type.GroupCreateInput
import com.github.damontecres.stashapp.api.type.PerformerCreateInput
import com.github.damontecres.stashapp.api.type.SortDirectionEnum
import com.github.damontecres.stashapp.api.type.StringCriterionInput
import com.github.damontecres.stashapp.api.type.TagCreateInput
import com.github.damontecres.stashapp.data.DataType
import com.github.damontecres.stashapp.data.SortOption
import com.github.damontecres.stashapp.data.room.RecentSearchItem
import com.github.damontecres.stashapp.navigation.FilterAndPosition
import com.github.damontecres.stashapp.ui.ComposeUiConfig
import com.github.damontecres.stashapp.ui.LocalGlobalContext
import com.github.damontecres.stashapp.ui.Material3MainTheme
import com.github.damontecres.stashapp.ui.components.ItemsRow
import com.github.damontecres.stashapp.ui.components.SearchEditTextBox
import com.github.damontecres.stashapp.util.CreateNew
import com.github.damontecres.stashapp.util.MutationEngine
import com.github.damontecres.stashapp.util.QueryEngine
import com.github.damontecres.stashapp.util.StashCoroutineExceptionHandler
import com.github.damontecres.stashapp.util.StashServer
import com.github.damontecres.stashapp.util.isNotNullOrBlank
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

private val TAG = "SearchForPage"

@Composable
fun SearchForDialog(
    show: Boolean,
    dataType: DataType,
    onItemClick: (StashData) -> Unit,
    onDismissRequest: () -> Unit,
    uiConfig: ComposeUiConfig,
    dialogTitle: String? = null,
    dismissOnClick: Boolean = true,
) {
    if (show) {
        Material3MainTheme {
            val server = LocalGlobalContext.current.server
            Dialog(
                onDismissRequest = onDismissRequest,
                properties =
                    DialogProperties(
                        usePlatformDefaultWidth = false,
                    ),
            ) {
                val elevatedContainerColor = MaterialTheme.colorScheme.surfaceColorAtElevation(3.dp)
                val color = MaterialTheme.colorScheme.secondaryContainer
                Box(
                    Modifier
                        .fillMaxSize(.9f)
                        .graphicsLayer {
                            this.clip = true
                            this.shape = RoundedCornerShape(28.0.dp)
                        }.drawBehind { drawRect(color = color) }
                        .background(MaterialTheme.colorScheme.secondaryContainer)
                        .padding(PaddingValues(12.dp)),
                    propagateMinConstraints = true,
                ) {
                    SearchForPage(
                        server = server,
                        title = dialogTitle ?: ("Add " + stringResource(dataType.stringId)),
                        searchId = 0,
                        dataType = dataType,
                        itemOnClick = { id, item ->
                            if (dismissOnClick) {
                                onDismissRequest.invoke()
                            }
                            onItemClick.invoke(item)
                        },
                        uiConfig = uiConfig,
                        modifier = Modifier.fillMaxSize(),
                    )
                }
            }
        }
    }
}

@Composable
fun SearchForPage(
    server: StashServer,
    title: String?,
    searchId: Long,
    dataType: DataType,
    itemOnClick: (Long, StashData) -> Unit,
    uiConfig: ComposeUiConfig,
    modifier: Modifier = Modifier,
) {
    val context = LocalContext.current
    val scope = rememberCoroutineScope()
    val queryEngine = QueryEngine(server)

    val searchDelay =
        PreferenceManager
            .getDefaultSharedPreferences(context)
            .getInt(context.getString(R.string.pref_key_search_delay), 500)
            .toLong()
    val perPage =
        PreferenceManager
            .getDefaultSharedPreferences(context)
            .getInt("maxSearchResults", 25)

    var searchQuery by remember { mutableStateOf("") }

    var results by remember { mutableStateOf<List<Any>>(listOf()) }
    var suggestions by remember { mutableStateOf<List<StashData>>(listOf()) }
    var recent by remember { mutableStateOf<List<StashData>>(listOf()) }

    val itemOnClickWrapper =
        { item: Any, _: FilterAndPosition? ->
            if (item is StashData) {
                if (dataType in DATA_TYPE_SUGGESTIONS) {
                    scope.launch(Dispatchers.IO + StashCoroutineExceptionHandler()) {
                        StashApplication
                            .getDatabase()
                            .recentSearchItemsDao()
                            .insert(RecentSearchItem(server.url, item.id, dataType))
                    }
                }
                itemOnClick.invoke(searchId, item)
            } else if (item is CreateNew) {
                scope.launch(Dispatchers.Main + StashCoroutineExceptionHandler()) {
                    val newItem = handleCreate(context, server, item.dataType, item.name)
                    if (newItem != null) {
                        withContext(Dispatchers.IO) {
                            StashApplication
                                .getDatabase()
                                .recentSearchItemsDao()
                                .insert(RecentSearchItem(server.url, newItem.id, dataType))
                        }
                        itemOnClick.invoke(searchId, newItem)
                    }
                }
            }
        }

    var job: Job? = null

    fun search(query: String) {
        if (query.isNotNullOrBlank()) {
            job?.cancel()
            job =
                scope.launch {
                    delay(searchDelay)
                    Log.v(TAG, "Starting search")
                    val items =
                        queryEngine.find(
                            dataType,
                            FindFilterType(
                                q = Optional.present(query),
                                per_page = Optional.present(perPage),
                            ),
                        )
                    results =
                        if (SearchForFragment.allowCreate(dataType, query, items)) {
                            val mutableItems = (items as List<Any>).toMutableList()
                            mutableItems.add(CreateNew(dataType, query))
                            mutableItems
                        } else {
                            items
                        }
                }
        } else {
            Log.v(TAG, "Query is empty")
            results = listOf(CreateNew(dataType, query))
        }
    }

    LaunchedEffect(Unit) {
        scope.launch(StashCoroutineExceptionHandler() + Dispatchers.IO) {
            val mostRecentIds =
                StashApplication
                    .getDatabase()
                    .recentSearchItemsDao()
                    .getMostRecent(perPage, server.url, dataType)
                    .map { it.id }
            if (mostRecentIds.isNotEmpty()) {
                recent =
                    when (dataType) {
                        DataType.PERFORMER -> queryEngine.findPerformers(performerIds = mostRecentIds)
                        DataType.TAG -> queryEngine.getTags(mostRecentIds)
                        DataType.STUDIO -> queryEngine.findStudios(studioIds = mostRecentIds)
                        DataType.GALLERY -> queryEngine.findGalleries(galleryIds = mostRecentIds)
                        DataType.GROUP -> queryEngine.findGroups(groupIds = mostRecentIds)
                        else -> {
                            listOf()
                        }
                    }
            }
        }
    }

    if (dataType in SearchForFragment.DATA_TYPE_SUGGESTIONS) {
        LaunchedEffect(Unit) {
            val sortBy =
                when (dataType) {
                    DataType.GALLERY -> SortOption.ImagesCount
                    else -> SortOption.ScenesCount
                }
            val filter =
                FindFilterType(
                    direction = Optional.present(SortDirectionEnum.DESC),
                    per_page = Optional.present(perPage),
                    sort = Optional.present(sortBy.key),
                )
            scope.launch(StashCoroutineExceptionHandler() + Dispatchers.IO) {
                suggestions =
                    when (dataType) {
                        DataType.GALLERY ->
                            // Cannot add an image to a zip/folder gallery, so exclude them
                            queryEngine.findGalleries(
                                filter,
                                GalleryFilterType(
                                    path =
                                        Optional.present(
                                            StringCriterionInput(
                                                value = "",
                                                modifier = CriterionModifier.IS_NULL,
                                            ),
                                        ),
                                ),
                            )

                        else -> queryEngine.find(dataType, filter)
                    }
            }
        }
    }

    val scrollState = rememberScrollState()

    Column(
        modifier =
            modifier
                .verticalScroll(scrollState),
        verticalArrangement = Arrangement.spacedBy(16.dp),
    ) {
        if (title != null) {
            Text(
                text = title,
                style = MaterialTheme.typography.titleLarge,
                color = MaterialTheme.colorScheme.onBackground,
            )
        }
        SearchEditTextBox(
            value = searchQuery,
            onValueChange = { newQuery ->
                searchQuery = newQuery
                search(newQuery)
            },
            onSearchClick = { search(searchQuery) },
        )

        val startPadding = 8.dp
        val bottomPadding = 8.dp

        if (results.isEmpty()) {
            if (searchQuery.isBlank()) {
                Text(
                    text = stringResource(R.string.waiting_for_query),
                    style = MaterialTheme.typography.titleLarge,
                    color = MaterialTheme.colorScheme.onBackground,
                )
            } else {
                // TODO handle create
                Text(
                    text = stringResource(R.string.stashapp_studio_tagger_no_results_found),
                    style = MaterialTheme.typography.titleLarge,
                    color = MaterialTheme.colorScheme.onBackground,
                )
            }
        } else {
            SearchItemsRow(
                title = stringResource(R.string.results),
                items = results,
                uiConfig = uiConfig,
                itemOnClick = itemOnClickWrapper,
                longClicker = { _, _ -> },
                modifier = Modifier.padding(start = startPadding, bottom = bottomPadding),
            )
        }
        if (suggestions.isNotEmpty()) {
            ItemsRow(
                title = stringResource(R.string.suggestions),
                items = suggestions,
                uiConfig = uiConfig,
                itemOnClick = itemOnClickWrapper,
                modifier = Modifier.padding(start = startPadding, bottom = bottomPadding),
            )
        }
        if (recent.isNotEmpty()) {
            ItemsRow(
                title =
                    stringResource(
                        R.string.format_recently_used,
                        context.getString(dataType.pluralStringId).lowercase(),
                    ),
                items = recent,
                uiConfig = uiConfig,
                itemOnClick = itemOnClickWrapper,
                modifier = Modifier.padding(start = startPadding, bottom = bottomPadding),
            )
        }
    }
}

suspend fun handleCreate(
    context: Context,
    server: StashServer,
    dataType: DataType,
    query: String?,
): StashData? {
    if (query.isNotNullOrBlank()) {
        val name = query.replaceFirstChar(Char::titlecase)
        val mutationEngine = MutationEngine(server)
        val item =
            when (dataType) {
                DataType.TAG -> {
                    mutationEngine.createTag(TagCreateInput(name = name))
                }

                DataType.PERFORMER -> {
                    mutationEngine.createPerformer(PerformerCreateInput(name = name))
                }

                DataType.GROUP -> {
                    mutationEngine.createGroup(GroupCreateInput(name = name))
                }

                else -> throw IllegalArgumentException("Unsupported datatype $dataType")
            }
        if (item != null) {
            Toast
                .makeText(
                    context,
                    "Created new ${context.getString(dataType.stringId)}: $name",
                    Toast.LENGTH_LONG,
                ).show()
        }
        return item
    } else {
        return null
    }
}
