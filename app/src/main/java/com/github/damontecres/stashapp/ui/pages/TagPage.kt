package com.github.damontecres.stashapp.ui.pages

import android.util.Log
import android.widget.Toast
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.AnnotatedString
import com.apollographql.apollo.api.Optional
import com.github.damontecres.stashapp.R
import com.github.damontecres.stashapp.api.fragment.TagData
import com.github.damontecres.stashapp.api.type.CriterionModifier
import com.github.damontecres.stashapp.api.type.GalleryFilterType
import com.github.damontecres.stashapp.api.type.HierarchicalMultiCriterionInput
import com.github.damontecres.stashapp.api.type.ImageFilterType
import com.github.damontecres.stashapp.api.type.PerformerFilterType
import com.github.damontecres.stashapp.api.type.SceneFilterType
import com.github.damontecres.stashapp.api.type.SceneMarkerFilterType
import com.github.damontecres.stashapp.api.type.StudioFilterType
import com.github.damontecres.stashapp.api.type.TagFilterType
import com.github.damontecres.stashapp.data.DataType
import com.github.damontecres.stashapp.suppliers.FilterArgs
import com.github.damontecres.stashapp.ui.ComposeUiConfig
import com.github.damontecres.stashapp.ui.components.BasicItemInfo
import com.github.damontecres.stashapp.ui.components.ItemDetails
import com.github.damontecres.stashapp.ui.components.ItemOnClicker
import com.github.damontecres.stashapp.ui.components.LongClicker
import com.github.damontecres.stashapp.ui.components.StashGridTab
import com.github.damontecres.stashapp.ui.components.TabPage
import com.github.damontecres.stashapp.ui.components.TabProvider
import com.github.damontecres.stashapp.ui.components.TabWithSubItems
import com.github.damontecres.stashapp.ui.components.TableRow
import com.github.damontecres.stashapp.ui.components.tabFindFilter
import com.github.damontecres.stashapp.ui.filterArgsSaver
import com.github.damontecres.stashapp.util.MutationEngine
import com.github.damontecres.stashapp.util.PageFilterKey
import com.github.damontecres.stashapp.util.QueryEngine
import com.github.damontecres.stashapp.util.StashCoroutineExceptionHandler
import com.github.damontecres.stashapp.util.StashServer
import com.github.damontecres.stashapp.util.getUiTabs
import kotlinx.coroutines.launch

private const val TAG = "TagPage"

@Composable
fun TagPage(
    server: StashServer,
    id: String,
    includeSubTags: Boolean,
    itemOnClick: ItemOnClicker<Any>,
    longClicker: LongClicker<Any>,
    uiConfig: ComposeUiConfig,
    modifier: Modifier = Modifier,
) {
    val context = LocalContext.current
    var tag by remember { mutableStateOf<TagData?>(null) }
    // Remember separately so we don't have refresh the whole page
    var favorite by remember { mutableStateOf(false) }
    LaunchedEffect(id) {
        try {
            tag = QueryEngine(server).getTag(id)
            tag?.let {
                favorite = it.favorite
            }
        } catch (ex: QueryEngine.QueryException) {
            Log.e(TAG, "No tag found with ID $id", ex)
            Toast.makeText(context, "No tag found with ID $id", Toast.LENGTH_LONG).show()
        }
    }

    val scope = rememberCoroutineScope()

    tag?.let { tag ->
        val subToggleLabel =
            if (tag.child_count > 0) stringResource(R.string.stashapp_include_sub_tag_content) else null

        val uiTabs = getUiTabs(context, DataType.TAG)

        val tagsFunc = { includeSubTags: Boolean ->
            Optional.present(
                HierarchicalMultiCriterionInput(
                    value = Optional.present(listOf(tag.id)),
                    modifier = CriterionModifier.INCLUDES_ALL,
                    depth = Optional.present(if (includeSubTags) -1 else 0),
                ),
            )
        }

        val tabs =
            listOf(
                TabProvider(stringResource(R.string.stashapp_details)) {
                    TagDetails(
                        modifier = Modifier.fillMaxSize(),
                        uiConfig = uiConfig,
                        tag = tag,
                        favorite = favorite,
                        favoriteClick = {
                            val mutationEngine = MutationEngine(server)
                            scope.launch(StashCoroutineExceptionHandler(autoToast = true)) {
                                val newTag =
                                    mutationEngine.setTagFavorite(
                                        tagId = tag.id,
                                        favorite = !favorite,
                                    )
                                if (newTag != null) {
                                    favorite = newTag.favorite
                                    if (newTag.favorite) {
                                        Toast
                                            .makeText(
                                                context,
                                                "Tag favorited!",
                                                Toast.LENGTH_SHORT,
                                            ).show()
                                    }
                                }
                            }
                        },
                    )
                },
                TabWithSubItems<SceneFilterType>(
                    DataType.SCENE,
                    tabFindFilter(server, PageFilterKey.TAG_SCENES),
                    { subTags, filter -> filter.copy(tags = tagsFunc(subTags)) },
                ).toTabProvider(
                    server,
                    uiConfig,
                    itemOnClick,
                    longClicker,
                    subToggleLabel,
                    includeSubTags,
                ),
                TabWithSubItems<GalleryFilterType>(
                    DataType.GALLERY,
                    tabFindFilter(server, PageFilterKey.TAG_GALLERIES),
                    { subTags, filter -> filter.copy(tags = tagsFunc(subTags)) },
                ).toTabProvider(
                    server,
                    uiConfig,
                    itemOnClick,
                    longClicker,
                    subToggleLabel,
                    includeSubTags,
                ),
                TabWithSubItems<ImageFilterType>(
                    DataType.IMAGE,
                    tabFindFilter(server, PageFilterKey.TAG_IMAGES),
                    { subTags, filter -> filter.copy(tags = tagsFunc(subTags)) },
                ).toTabProvider(
                    server,
                    uiConfig,
                    itemOnClick,
                    longClicker,
                    subToggleLabel,
                    includeSubTags,
                ),
                TabWithSubItems<SceneMarkerFilterType>(
                    DataType.MARKER,
                    tabFindFilter(server, PageFilterKey.TAG_MARKERS),
                    { subTags, filter -> filter.copy(tags = tagsFunc(subTags)) },
                ).toTabProvider(
                    server,
                    uiConfig,
                    itemOnClick,
                    longClicker,
                    subToggleLabel,
                    includeSubTags,
                ),
                TabWithSubItems<PerformerFilterType>(
                    DataType.PERFORMER,
                    tabFindFilter(server, PageFilterKey.TAG_PERFORMERS),
                    { subTags, filter -> filter.copy(tags = tagsFunc(subTags)) },
                ).toTabProvider(
                    server,
                    uiConfig,
                    itemOnClick,
                    longClicker,
                    subToggleLabel,
                    includeSubTags,
                ),
                TabWithSubItems<StudioFilterType>(
                    DataType.STUDIO,
                    null,
                    { subTags, filter -> filter.copy(tags = tagsFunc(subTags)) },
                ).toTabProvider(
                    server,
                    uiConfig,
                    itemOnClick,
                    longClicker,
                    subToggleLabel,
                    includeSubTags,
                ),
                TabProvider
                    (stringResource(R.string.stashapp_sub_tags)) { positionCallback ->
                        var filter by rememberSaveable(saver = filterArgsSaver) {
                            mutableStateOf(
                                FilterArgs(
                                    dataType = DataType.TAG,
                                    objectFilter = TagFilterType(parents = tagsFunc(false)),
                                ),
                            )
                        }
                        StashGridTab(
                            name = stringResource(R.string.stashapp_sub_tags),
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
                    },
            ).filter { it.name in uiTabs }
        val title = AnnotatedString(tag.name)
        TabPage(title, tabs, modifier)
    }
}

@Composable
fun TagDetails(
    uiConfig: ComposeUiConfig,
    tag: TagData,
    favorite: Boolean,
    favoriteClick: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val rows =
        buildList {
            add(TableRow.from(R.string.stashapp_sort_name, tag.sort_name))
            add(TableRow.from(R.string.stashapp_description, tag.description))
            if (tag.aliases.isNotEmpty()) {
                add(
                    TableRow.from(
                        R.string.stashapp_aliases,
                        tag.aliases.joinToString(", "),
                    ),
                )
            }
        }.filterNotNull()
    ItemDetails(
        modifier = modifier,
        uiConfig = uiConfig,
        imageUrl = tag.image_path,
        tableRows = rows,
        favorite = favorite,
        favoriteClick = favoriteClick,
        basicItemInfo = BasicItemInfo(tag.id, tag.created_at, tag.updated_at),
    )
}
