package com.github.damontecres.stashapp.ui.pages

import android.widget.Toast
import androidx.compose.foundation.layout.fillMaxSize
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
import com.github.damontecres.stashapp.data.StashFindFilter
import com.github.damontecres.stashapp.suppliers.FilterArgs
import com.github.damontecres.stashapp.ui.ComposeUiConfig
import com.github.damontecres.stashapp.ui.components.FilterUiMode
import com.github.damontecres.stashapp.ui.components.ItemDetails
import com.github.damontecres.stashapp.ui.components.ItemOnClicker
import com.github.damontecres.stashapp.ui.components.LongClicker
import com.github.damontecres.stashapp.ui.components.StashGridControls
import com.github.damontecres.stashapp.ui.components.TabPage
import com.github.damontecres.stashapp.ui.components.TabProvider
import com.github.damontecres.stashapp.ui.components.TableRow
import com.github.damontecres.stashapp.util.MutationEngine
import com.github.damontecres.stashapp.util.PageFilterKey
import com.github.damontecres.stashapp.util.QueryEngine
import com.github.damontecres.stashapp.util.StashCoroutineExceptionHandler
import com.github.damontecres.stashapp.util.StashServer
import com.github.damontecres.stashapp.util.getUiTabs
import com.github.damontecres.stashapp.util.showDebugInfo
import com.github.damontecres.stashapp.views.parseTimeToString
import kotlinx.coroutines.launch

@Composable
fun TagPage(
    server: StashServer,
    id: String,
    includeSubTags: Boolean,
    itemOnClick: ItemOnClicker<Any>,
    longClicker: LongClicker<Any>,
    modifier: Modifier = Modifier,
) {
    var tag by remember { mutableStateOf<TagData?>(null) }
    // Remember separately so we don't have refresh the whole page
    var favorite by remember { mutableStateOf(false) }
    LaunchedEffect(id) {
        tag = QueryEngine(server).getTag(id)
        tag?.let {
            favorite = it.favorite
        }
    }
    val context = LocalContext.current
    val scope = rememberCoroutineScope()

    fun createTab(initialFilter: FilterArgs): TabProvider =
        TabProvider(context.getString(initialFilter.dataType.pluralStringId)) { positionCallback ->
            StashGridControls(
                server = server,
                initialFilter = initialFilter,
                itemOnClick = itemOnClick,
                longClicker = longClicker,
                filterUiMode = FilterUiMode.CREATE_FILTER,
                modifier = Modifier,
                positionCallback = positionCallback,
                uiConfig = ComposeUiConfig.fromStashServer(server),
            )
        }

    fun findFilter(pageFilterKey: PageFilterKey): StashFindFilter? = server.serverPreferences.getDefaultPageFilter(pageFilterKey).findFilter

    tag?.let { tag ->
        val tags =
            Optional.present(
                HierarchicalMultiCriterionInput(
                    value = Optional.present(listOf(tag.id)),
                    modifier = CriterionModifier.INCLUDES_ALL,
                    depth = Optional.present(if (includeSubTags) -1 else 0),
                ),
            )
        val uiTabs = getUiTabs(context, DataType.TAG)
        val tabs =
            listOf(
                TabProvider(stringResource(R.string.stashapp_details)) {
                    TagDetails(
                        modifier = Modifier.fillMaxSize(),
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
                createTab(
                    FilterArgs(
                        dataType = DataType.SCENE,
                        findFilter = findFilter(PageFilterKey.TAG_SCENES),
                        objectFilter = SceneFilterType(tags = tags),
                    ),
                ),
                createTab(
                    FilterArgs(
                        dataType = DataType.GALLERY,
                        findFilter = findFilter(PageFilterKey.TAG_GALLERIES),
                        objectFilter = GalleryFilterType(tags = tags),
                    ),
                ),
                createTab(
                    FilterArgs(
                        dataType = DataType.IMAGE,
                        findFilter = findFilter(PageFilterKey.TAG_IMAGES),
                        objectFilter = ImageFilterType(tags = tags),
                    ),
                ),
                createTab(
                    FilterArgs(
                        dataType = DataType.MARKER,
                        findFilter = findFilter(PageFilterKey.TAG_MARKERS),
                        objectFilter = SceneMarkerFilterType(tags = tags),
                    ),
                ),
                createTab(
                    FilterArgs(
                        dataType = DataType.PERFORMER,
                        findFilter = findFilter(PageFilterKey.TAG_PERFORMERS),
                        objectFilter = PerformerFilterType(tags = tags),
                    ),
                ),
                createTab(
                    FilterArgs(
                        dataType = DataType.STUDIO,
                        findFilter = null,
                        objectFilter = StudioFilterType(tags = tags),
                    ),
                ),
                TabProvider(stringResource(R.string.stashapp_sub_tags)) { positionCallback ->
                    StashGridControls(
                        server = server,
                        initialFilter =
                            FilterArgs(
                                dataType = DataType.TAG,
                                objectFilter = TagFilterType(parents = tags),
                            ),
                        itemOnClick = itemOnClick,
                        longClicker = longClicker,
                        filterUiMode = FilterUiMode.CREATE_FILTER,
                        modifier = Modifier,
                        positionCallback = positionCallback,
                        uiConfig = ComposeUiConfig.fromStashServer(server),
                    )
                },
                TabProvider(stringResource(R.string.stashapp_parent_tags)) { positionCallback ->
                    StashGridControls(
                        server = server,
                        initialFilter =
                            FilterArgs(
                                dataType = DataType.TAG,
                                objectFilter =
                                    TagFilterType(
                                        children =
                                            Optional.present(
                                                HierarchicalMultiCriterionInput(
                                                    value = Optional.present(listOf(tag.id)),
                                                    modifier = CriterionModifier.INCLUDES_ALL,
                                                    depth = Optional.present(0),
                                                ),
                                            ),
                                    ),
                            ),
                        itemOnClick = itemOnClick,
                        longClicker = longClicker,
                        filterUiMode = FilterUiMode.CREATE_FILTER,
                        modifier = Modifier,
                        positionCallback = positionCallback,
                        uiConfig = ComposeUiConfig.fromStashServer(server),
                    )
                },
            ).filter { it.name in uiTabs }
        val title = AnnotatedString(tag.name)
        TabPage(title, tabs, modifier)
    }
}

@Composable
fun TagDetails(
    tag: TagData,
    favorite: Boolean,
    favoriteClick: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val rows =
        buildList {
            if (showDebugInfo()) {
                add(TableRow(R.string.id, tag.id))
            }
            add(TableRow.from(R.string.stashapp_description, tag.description))
            if (tag.aliases.isNotEmpty()) {
                add(
                    TableRow(
                        R.string.stashapp_aliases,
                        tag.aliases.joinToString(", "),
                    ),
                )
            }

            add(TableRow.from(R.string.stashapp_created_at, parseTimeToString(tag.created_at)))
            add(TableRow.from(R.string.stashapp_updated_at, parseTimeToString(tag.updated_at)))
        }.filterNotNull()
    ItemDetails(
        modifier = modifier,
        imageUrl = tag.image_path,
        tableRows = rows,
        favorite = favorite,
        favoriteClick = favoriteClick,
    )
}
