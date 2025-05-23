package com.github.damontecres.stashapp.ui.pages

import android.util.Log
import android.widget.Toast
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.AnnotatedString
import androidx.compose.ui.unit.dp
import coil3.compose.AsyncImage
import coil3.request.ImageRequest
import coil3.request.crossfade
import com.apollographql.apollo.api.Optional
import com.github.damontecres.stashapp.R
import com.github.damontecres.stashapp.api.fragment.GroupData
import com.github.damontecres.stashapp.api.fragment.GroupRelationshipType
import com.github.damontecres.stashapp.api.fragment.TagData
import com.github.damontecres.stashapp.api.type.CriterionModifier
import com.github.damontecres.stashapp.api.type.GroupFilterType
import com.github.damontecres.stashapp.api.type.HierarchicalMultiCriterionInput
import com.github.damontecres.stashapp.api.type.SceneFilterType
import com.github.damontecres.stashapp.api.type.SceneMarkerFilterType
import com.github.damontecres.stashapp.data.DataType
import com.github.damontecres.stashapp.navigation.Destination
import com.github.damontecres.stashapp.suppliers.DataSupplierOverride
import com.github.damontecres.stashapp.suppliers.FilterArgs
import com.github.damontecres.stashapp.ui.ComposeUiConfig
import com.github.damontecres.stashapp.ui.LocalGlobalContext
import com.github.damontecres.stashapp.ui.components.ItemDetailsFooter
import com.github.damontecres.stashapp.ui.components.ItemOnClicker
import com.github.damontecres.stashapp.ui.components.ItemsRow
import com.github.damontecres.stashapp.ui.components.LongClicker
import com.github.damontecres.stashapp.ui.components.Rating100
import com.github.damontecres.stashapp.ui.components.StashGridTab
import com.github.damontecres.stashapp.ui.components.TabPage
import com.github.damontecres.stashapp.ui.components.TabProvider
import com.github.damontecres.stashapp.ui.components.TableRow
import com.github.damontecres.stashapp.ui.components.TableRowComposable
import com.github.damontecres.stashapp.ui.components.tabFindFilter
import com.github.damontecres.stashapp.ui.filterArgsSaver
import com.github.damontecres.stashapp.ui.titleCount
import com.github.damontecres.stashapp.util.MutationEngine
import com.github.damontecres.stashapp.util.PageFilterKey
import com.github.damontecres.stashapp.util.QueryEngine
import com.github.damontecres.stashapp.util.StashCoroutineExceptionHandler
import com.github.damontecres.stashapp.util.StashServer
import com.github.damontecres.stashapp.util.getUiTabs
import com.github.damontecres.stashapp.util.isNotNullOrBlank
import com.github.damontecres.stashapp.util.showSetRatingToast
import kotlinx.coroutines.launch
import kotlin.time.Duration.Companion.seconds

private const val TAG = "GroupPage"

@Composable
fun GroupPage(
    server: StashServer,
    id: String,
    includeSubGroups: Boolean,
    itemOnClick: ItemOnClicker<Any>,
    longClicker: LongClicker<Any>,
    uiConfig: ComposeUiConfig,
    modifier: Modifier = Modifier,
) {
    var group by remember { mutableStateOf<GroupData?>(null) }
    // Remember separately so we don't have refresh the whole page
    var rating100 by remember { mutableIntStateOf(0) }
    var tags by remember { mutableStateOf<List<TagData>>(listOf()) }
    val context = LocalContext.current
    LaunchedEffect(id) {
        try {
            val queryEngine = QueryEngine(server)
            group = queryEngine.getGroup(id)
            group?.let {
                rating100 = it.rating100 ?: 0
                tags = queryEngine.getTags(it.tags.map { it.slimTagData.id })
            }
        } catch (ex: QueryEngine.QueryException) {
            Log.e(TAG, "No group found with ID $id", ex)
            Toast.makeText(context, "No group found with ID $id", Toast.LENGTH_LONG).show()
        }
    }
    val scope = rememberCoroutineScope()

    group?.let { group ->
        val subToggleLabel =
            if (group.sub_group_count > 0) stringResource(R.string.stashapp_include_sub_group_content) else null
        val groupsFunc = { includeSubGroups: Boolean ->
            Optional.present(
                HierarchicalMultiCriterionInput(
                    value = Optional.present(listOf(group.id)),
                    modifier = CriterionModifier.INCLUDES_ALL,
                    depth = Optional.present(if (includeSubGroups) -1 else 0),
                ),
            )
        }

        val detailsTab =
            TabProvider(stringResource(R.string.stashapp_details)) {
                GroupDetails(
                    modifier = Modifier.fillMaxSize(),
                    uiConfig = uiConfig,
                    group = group,
                    tags = tags,
                    rating100 = rating100,
                    rating100Click = { newRating100 ->
                        val mutationEngine = MutationEngine(server)
                        scope.launch(StashCoroutineExceptionHandler(autoToast = true)) {
                            val newGroup =
                                mutationEngine.updateGroup(
                                    groupId = group.id,
                                    rating100 = newRating100,
                                )
                            if (newGroup != null) {
                                rating100 = newGroup.rating100 ?: 0
                                showSetRatingToast(
                                    context,
                                    newGroup.rating100 ?: 0,
                                    server.serverPreferences.ratingsAsStars,
                                )
                            }
                        }
                    },
                    itemOnClick = itemOnClick,
                    longClicker = longClicker,
                )
            }

        // Scenes
        var scenesSubTags by rememberSaveable { mutableStateOf(false) }
        var scenesFilter by rememberSaveable(scenesSubTags, saver = filterArgsSaver) {
            mutableStateOf(
                FilterArgs(
                    DataType.SCENE,
                    findFilter = tabFindFilter(server, PageFilterKey.TAG_SCENES),
                    objectFilter = SceneFilterType(groups = groupsFunc(scenesSubTags)),
                ),
            )
        }
        val scenesTab =
            remember(scenesFilter, scenesSubTags) {
                TabProvider(context.getString(R.string.stashapp_scenes)) { positionCallback ->
                    StashGridTab(
                        name = context.getString(R.string.stashapp_scenes),
                        server = server,
                        initialFilter = scenesFilter,
                        itemOnClick = itemOnClick,
                        longClicker = longClicker,
                        modifier = Modifier,
                        positionCallback = positionCallback,
                        subToggleLabel = subToggleLabel,
                        onSubToggleCheck = { scenesSubTags = it },
                        subToggleChecked = scenesSubTags,
                        composeUiConfig = uiConfig,
                        onFilterChange = { scenesFilter = it },
                    )
                }
            }
        // markers
        var markersSubTags by rememberSaveable { mutableStateOf(false) }
        var markersFilter by rememberSaveable(markersSubTags, saver = filterArgsSaver) {
            mutableStateOf(
                FilterArgs(
                    DataType.MARKER,
                    findFilter = null,
                    objectFilter =
                        SceneMarkerFilterType(
                            scene_filter =
                                Optional.present(
                                    SceneFilterType(groups = groupsFunc(markersSubTags)),
                                ),
                        ),
                ),
            )
        }
        val markersTab =
            remember(markersSubTags, markersFilter) {
                TabProvider(context.getString(R.string.stashapp_markers)) { positionCallback ->
                    StashGridTab(
                        name = context.getString(R.string.stashapp_markers),
                        server = server,
                        initialFilter = markersFilter,
                        itemOnClick = itemOnClick,
                        longClicker = longClicker,
                        modifier = Modifier,
                        positionCallback = positionCallback,
                        subToggleLabel = subToggleLabel,
                        onSubToggleCheck = { markersSubTags = it },
                        subToggleChecked = markersSubTags,
                        composeUiConfig = uiConfig,
                        onFilterChange = { markersFilter = it },
                    )
                }
            }

        // containing groups
        val containingGroupsTab =
            TabProvider(stringResource(R.string.stashapp_containing_groups)) { positionCallback ->
                var filter by rememberSaveable(saver = filterArgsSaver) {
                    mutableStateOf(
                        FilterArgs(
                            dataType = DataType.GROUP,
                            override =
                                DataSupplierOverride.GroupRelationship(
                                    group.id,
                                    GroupRelationshipType.CONTAINING,
                                ),
                        ),
                    )
                }
                StashGridTab(
                    name = stringResource(R.string.stashapp_containing_groups),
                    server = server,
                    initialFilter = filter,
                    itemOnClick = itemOnClick,
                    longClicker = longClicker,
                    modifier = Modifier,
                    positionCallback = positionCallback,
                    composeUiConfig = uiConfig,
                    subToggleLabel = null,
                    onFilterChange = { filter = it },
                )
            }

        // sub groups
        val subGroupsTab =
            TabProvider(stringResource(R.string.stashapp_sub_groups)) { positionCallback ->
                var filter by rememberSaveable(saver = filterArgsSaver) {
                    mutableStateOf(
                        FilterArgs(
                            dataType = DataType.GROUP,
                            override =
                                DataSupplierOverride.GroupRelationship(
                                    group.id,
                                    GroupRelationshipType.SUB,
                                ),
                        ),
                    )
                }
                StashGridTab(
                    name = stringResource(R.string.stashapp_sub_groups),
                    server = server,
                    initialFilter = filter,
                    itemOnClick = itemOnClick,
                    longClicker = longClicker,
                    composeUiConfig = uiConfig,
                    modifier = Modifier,
                    positionCallback = positionCallback,
                    subToggleLabel =
                        if (group.sub_group_count > 0) {
                            stringResource(R.string.stashapp_include_sub_group_content)
                        } else {
                            null
                        },
                    onFilterChange = { filter = it },
                )
            }

        val uiTabs = getUiTabs(context, DataType.GROUP)
        val tabs =
            listOf(detailsTab, scenesTab, markersTab, containingGroupsTab, subGroupsTab)
                .filter { it.name in uiTabs }
        val title = AnnotatedString(group.name)
        TabPage(title, tabs, modifier)
    }
}

@Composable
fun GroupDetails(
    group: GroupData,
    tags: List<TagData>,
    uiConfig: ComposeUiConfig,
    rating100: Int,
    rating100Click: (rating100: Int) -> Unit,
    itemOnClick: ItemOnClicker<Any>,
    longClicker: LongClicker<Any>,
    modifier: Modifier = Modifier,
) {
    val navigationManager = LocalGlobalContext.current.navigationManager
    val context = LocalContext.current
    val rows =
        buildList {
            add(TableRow.from(R.string.stashapp_date, group.date))
            add(TableRow.from(R.string.stashapp_duration, group.duration?.seconds?.toString()))
            add(
                TableRow.from(R.string.stashapp_studio, group.studio?.name) {
                    navigationManager.navigate(Destination.Item(DataType.STUDIO, group.studio!!.id))
                },
            )
            add(
                TableRow.from(R.string.stashapp_director, group.director) {
                    navigationManager.navigate(
                        Destination.Filter(
                            filterArgs =
                                FilterArgs(
                                    dataType = DataType.GROUP,
                                    name = context.getString(R.string.stashapp_director) + ": " + group.director,
                                    objectFilter = GroupFilterType(director = stringCriterion(group.director!!)),
                                ),
                        ),
                    )
                },
            )
            add(TableRow.from(R.string.stashapp_synopsis, group.synopsis))
        }.filterNotNull()
    LazyColumn(modifier = modifier) {
        item {
            Rating100(
                rating100 = rating100,
                uiConfig = uiConfig,
                onRatingChange = rating100Click,
                enabled = true,
                modifier =
                    Modifier
                        .height(32.dp)
                        .padding(start = 12.dp),
            )
        }
        item {
            Row(
                horizontalArrangement = Arrangement.SpaceEvenly,
                modifier = Modifier.heightIn(max = 368.dp),
            ) {
                // Images
                if (group.front_image_path.isNotNullOrBlank()) {
                    AsyncImage(
                        modifier =
                            Modifier
                                .padding(12.dp)
                                .weight(1f)
                                .fillMaxSize(),
                        model =
                            ImageRequest
                                .Builder(LocalContext.current)
                                .data(group.front_image_path)
                                .crossfade(true)
                                .build(),
                        contentDescription = null,
                        contentScale = ContentScale.Fit,
                    )
                }
                if (group.back_image_path.isNotNullOrBlank()) {
                    AsyncImage(
                        modifier =
                            Modifier
                                .padding(12.dp)
                                .weight(1f)
                                .fillMaxSize(),
                        model =
                            ImageRequest
                                .Builder(LocalContext.current)
                                .data(group.back_image_path)
                                .crossfade(true)
                                .build(),
                        contentDescription = null,
                        contentScale = ContentScale.Fit,
                    )
                }
            }
        }

        items(rows) { row ->
            TableRowComposable(row)
        }
        if (tags.isNotEmpty()) {
            item {
                ItemsRow(
                    title = titleCount(R.string.stashapp_tags, tags),
                    items = tags,
                    uiConfig = uiConfig,
                    itemOnClick = itemOnClick,
                    longClicker = longClicker,
                    modifier = Modifier.padding(top = 12.dp),
                )
            }
        }
        item {
            ItemDetailsFooter(
                id = group.id,
                createdAt = group.created_at.toString(),
                updatedAt = group.updated_at.toString(),
                modifier =
                    Modifier
                        .fillMaxWidth()
                        .padding(top = 32.dp),
            )
        }
    }
}
