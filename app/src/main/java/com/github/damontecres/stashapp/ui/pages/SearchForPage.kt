package com.github.damontecres.stashapp.ui.pages

import android.util.Log
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Search
import androidx.compose.material3.TextField
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.unit.dp
import androidx.preference.PreferenceManager
import androidx.tv.material3.Icon
import androidx.tv.material3.MaterialTheme
import androidx.tv.material3.Text
import com.apollographql.apollo.api.Optional
import com.github.damontecres.stashapp.R
import com.github.damontecres.stashapp.SearchForFragment
import com.github.damontecres.stashapp.StashApplication
import com.github.damontecres.stashapp.api.fragment.StashData
import com.github.damontecres.stashapp.api.type.CriterionModifier
import com.github.damontecres.stashapp.api.type.FindFilterType
import com.github.damontecres.stashapp.api.type.GalleryFilterType
import com.github.damontecres.stashapp.api.type.SortDirectionEnum
import com.github.damontecres.stashapp.api.type.StringCriterionInput
import com.github.damontecres.stashapp.data.DataType
import com.github.damontecres.stashapp.data.SortOption
import com.github.damontecres.stashapp.navigation.FilterAndPosition
import com.github.damontecres.stashapp.ui.ComposeUiConfig
import com.github.damontecres.stashapp.ui.Material3MainTheme
import com.github.damontecres.stashapp.util.QueryEngine
import com.github.damontecres.stashapp.util.StashCoroutineExceptionHandler
import com.github.damontecres.stashapp.util.StashServer
import com.github.damontecres.stashapp.util.isNotNullOrBlank
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch

private val TAG = "SearchForPage"

@Composable
fun SearchForPage(
    server: StashServer,
    title: String?,
    dataType: DataType,
    itemOnClick: (StashData) -> Unit,
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

    var results by remember { mutableStateOf<List<StashData>>(listOf()) }
    var suggestions by remember { mutableStateOf<List<StashData>>(listOf()) }
    var recent by remember { mutableStateOf<List<StashData>>(listOf()) }

    val itemOnClickWrapper =
        { item: Any, _: FilterAndPosition? -> itemOnClick.invoke(item as StashData) }

    var job: Job? = null

    fun search(query: String) {
        if (query.isNotNullOrBlank()) {
            job?.cancel()
            job =
                scope.launch {
                    delay(searchDelay)
                    Log.v(TAG, "Starting search")
                    results = queryEngine.find(dataType, FindFilterType(q = Optional.present(query), per_page = Optional.present(perPage)))
                }
        } else {
            Log.v(TAG, "Query is empty")
            results = listOf()
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
        Material3MainTheme {
            TextField(
                value = searchQuery,
                onValueChange = { newQuery ->
                    searchQuery = newQuery
                    search(newQuery)
                },
                leadingIcon = {
                    Icon(
                        Icons.Default.Search,
                        contentDescription = stringResource(R.string.stashapp_actions_search),
                    )
                },
                maxLines = 1,
                shape = CircleShape,
                keyboardOptions =
                    KeyboardOptions(
                        autoCorrectEnabled = false,
                        imeAction = ImeAction.Search,
                    ),
                keyboardActions =
                    KeyboardActions(
                        onSearch = {
                            search(searchQuery)
                            this.defaultKeyboardAction(ImeAction.Done)
                        },
                    ),
            )
        }
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
                Text(
                    text = stringResource(R.string.stashapp_studio_tagger_no_results_found),
                    style = MaterialTheme.typography.titleLarge,
                    color = MaterialTheme.colorScheme.onBackground,
                )
            }
        } else {
            ItemsRow(
                title = R.string.results,
                items = results,
                uiConfig = ComposeUiConfig.fromStashServer(server),
                itemOnClick = itemOnClickWrapper,
                longClicker = { _, _ -> },
                modifier = Modifier.padding(start = startPadding, bottom = bottomPadding),
            )
        }
        if (suggestions.isNotEmpty()) {
            ItemsRow(
                title = R.string.suggestions,
                items = suggestions,
                uiConfig = ComposeUiConfig.fromStashServer(server),
                itemOnClick = itemOnClickWrapper,
                longClicker = { _, _ -> },
                modifier = Modifier.padding(start = startPadding, bottom = bottomPadding),
            )
        }
        if (recent.isNotEmpty()) {
            val title =
                context.getString(
                    R.string.format_recently_used,
                    context.getString(dataType.pluralStringId).lowercase(),
                )
            ItemsRow(
                title = title,
                items = recent,
                uiConfig = ComposeUiConfig.fromStashServer(server),
                itemOnClick = itemOnClickWrapper,
                longClicker = { _, _ -> },
                modifier = Modifier.padding(start = startPadding, bottom = bottomPadding),
            )
        }
    }
}
