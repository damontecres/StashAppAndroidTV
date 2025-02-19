package com.github.damontecres.stashapp.ui.pages

import android.widget.Toast
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
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
import com.github.damontecres.stashapp.api.fragment.StudioData
import com.github.damontecres.stashapp.api.type.CriterionModifier
import com.github.damontecres.stashapp.api.type.GalleryFilterType
import com.github.damontecres.stashapp.api.type.GroupFilterType
import com.github.damontecres.stashapp.api.type.HierarchicalMultiCriterionInput
import com.github.damontecres.stashapp.api.type.ImageFilterType
import com.github.damontecres.stashapp.api.type.MultiCriterionInput
import com.github.damontecres.stashapp.api.type.PerformerFilterType
import com.github.damontecres.stashapp.api.type.SceneFilterType
import com.github.damontecres.stashapp.api.type.SceneMarkerFilterType
import com.github.damontecres.stashapp.api.type.StudioFilterType
import com.github.damontecres.stashapp.data.DataType
import com.github.damontecres.stashapp.suppliers.DataSupplierOverride
import com.github.damontecres.stashapp.suppliers.FilterArgs
import com.github.damontecres.stashapp.ui.components.ItemDetails
import com.github.damontecres.stashapp.ui.components.ItemOnClicker
import com.github.damontecres.stashapp.ui.components.LongClicker
import com.github.damontecres.stashapp.ui.components.StashGridTab
import com.github.damontecres.stashapp.ui.components.TabPage
import com.github.damontecres.stashapp.ui.components.TabProvider
import com.github.damontecres.stashapp.ui.components.TableRow
import com.github.damontecres.stashapp.ui.components.createTabFunc
import com.github.damontecres.stashapp.ui.components.tabFindFilter
import com.github.damontecres.stashapp.util.MutationEngine
import com.github.damontecres.stashapp.util.PageFilterKey
import com.github.damontecres.stashapp.util.QueryEngine
import com.github.damontecres.stashapp.util.StashCoroutineExceptionHandler
import com.github.damontecres.stashapp.util.StashServer
import com.github.damontecres.stashapp.util.getUiTabs
import com.github.damontecres.stashapp.util.showDebugInfo
import com.github.damontecres.stashapp.util.showSetRatingToast
import com.github.damontecres.stashapp.views.parseTimeToString
import kotlinx.coroutines.launch

@Composable
fun StudioPage(
    server: StashServer,
    id: String,
    includeSubStudios: Boolean,
    itemOnClick: ItemOnClicker<Any>,
    longClicker: LongClicker<Any>,
    modifier: Modifier = Modifier,
) {
    var studio by remember { mutableStateOf<StudioData?>(null) }
    // Remember separately so we don't have refresh the whole page
    var favorite by remember { mutableStateOf(false) }
    var rating100 by remember { mutableIntStateOf(0) }
    LaunchedEffect(id) {
        studio = QueryEngine(server).getStudio(id)
        studio?.let {
            favorite = it.favorite
            rating100 = it.rating100 ?: 0
        }
    }
    val context = LocalContext.current
    val scope = rememberCoroutineScope()

    val createTab = createTabFunc(server, itemOnClick, longClicker)

    studio?.let { studio ->
        val studios =
            Optional.present(
                HierarchicalMultiCriterionInput(
                    value = Optional.present(listOf(studio.id)),
                    modifier = CriterionModifier.INCLUDES_ALL,
                    depth = Optional.present(if (includeSubStudios) -1 else 0),
                ),
            )
        val uiTabs = getUiTabs(context, DataType.STUDIO)
        val tabs =
            listOf(
                TabProvider(stringResource(R.string.stashapp_details)) {
                    StudioDetails(
                        modifier = Modifier.fillMaxSize(),
                        studio = studio,
                        favorite = favorite,
                        favoriteClick = {
                            val mutationEngine = MutationEngine(server)
                            scope.launch(StashCoroutineExceptionHandler(autoToast = true)) {
                                val newStudio =
                                    mutationEngine.updateStudio(
                                        studioId = studio.id,
                                        favorite = !favorite,
                                    )
                                if (newStudio != null) {
                                    favorite = newStudio.favorite
                                    if (newStudio.favorite) {
                                        Toast
                                            .makeText(
                                                context,
                                                context.getString(R.string.studio_favorited),
                                                Toast.LENGTH_SHORT,
                                            ).show()
                                    }
                                }
                            }
                        },
                        rating100 = rating100,
                        rating100Click = { newRating100 ->
                            val mutationEngine = MutationEngine(server)
                            scope.launch(StashCoroutineExceptionHandler(autoToast = true)) {
                                val newStudio =
                                    mutationEngine.updateStudio(
                                        studioId = studio.id,
                                        rating100 = newRating100,
                                    )
                                if (newStudio != null) {
                                    rating100 = newStudio.rating100 ?: 0
                                    showSetRatingToast(
                                        context,
                                        newStudio.rating100 ?: 0,
                                        server.serverPreferences.ratingsAsStars,
                                    )
                                }
                            }
                        },
                    )
                },
                createTab(
                    FilterArgs(
                        dataType = DataType.SCENE,
                        findFilter = tabFindFilter(server, PageFilterKey.STUDIO_SCENES),
                        objectFilter = SceneFilterType(studios = studios),
                    ),
                ),
                createTab(
                    FilterArgs(
                        dataType = DataType.GALLERY,
                        findFilter = tabFindFilter(server, PageFilterKey.STUDIO_GALLERIES),
                        objectFilter = GalleryFilterType(studios = studios),
                    ),
                ),
                createTab(
                    FilterArgs(
                        dataType = DataType.IMAGE,
                        findFilter = tabFindFilter(server, PageFilterKey.STUDIO_IMAGES),
                        objectFilter = ImageFilterType(studios = studios),
                    ),
                ),
                createTab(
                    FilterArgs(
                        dataType = DataType.PERFORMER,
                        findFilter = tabFindFilter(server, PageFilterKey.STUDIO_PERFORMERS),
                        objectFilter = PerformerFilterType(studios = studios),
                    ),
                ),
                createTab(
                    FilterArgs(
                        dataType = DataType.GROUP,
                        findFilter = tabFindFilter(server, PageFilterKey.STUDIO_GROUPS),
                        objectFilter = GroupFilterType(studios = studios),
                    ),
                ),
                createTab(
                    FilterArgs(
                        dataType = DataType.TAG,
                        override = DataSupplierOverride.StudioTags(studio.id),
                    ),
                ),
                TabProvider(stringResource(R.string.stashapp_subsidiary_studios)) { positionCallback ->
                    StashGridTab(
                        name = stringResource(R.string.stashapp_subsidiary_studios),
                        server = server,
                        initialFilter =
                            FilterArgs(
                                dataType = DataType.STUDIO,
                                findFilter = tabFindFilter(server, PageFilterKey.STUDIO_CHILDREN),
                                objectFilter =
                                    StudioFilterType(
                                        parents =
                                            Optional.present(
                                                MultiCriterionInput(
                                                    value = Optional.present(listOf(studio.id)),
                                                    modifier = CriterionModifier.INCLUDES,
                                                ),
                                            ),
                                    ),
                            ),
                        itemOnClick = itemOnClick,
                        longClicker = longClicker,
                        modifier = Modifier,
                        positionCallback = positionCallback,
                    )
                },
                createTab(
                    FilterArgs(
                        dataType = DataType.MARKER,
                        objectFilter =
                            SceneMarkerFilterType(
                                scene_filter =
                                    Optional.present(
                                        SceneFilterType(studios = studios),
                                    ),
                            ),
                    ),
                ),
            ).filter { it.name in uiTabs }
        val title = AnnotatedString(studio.name)
        TabPage(title, tabs, modifier)
    }
}

@Composable
fun StudioDetails(
    studio: StudioData,
    favorite: Boolean,
    favoriteClick: () -> Unit,
    rating100: Int,
    rating100Click: (rating100: Int) -> Unit,
    modifier: Modifier = Modifier,
) {
    val rows =
        buildList {
            if (showDebugInfo()) {
                add(TableRow(R.string.id, studio.id))
            }
            add(TableRow.from(R.string.stashapp_description, studio.details))
            add(TableRow.from(R.string.stashapp_parent_studio, studio.parent_studio?.name))
            if (studio.aliases.isNotEmpty()) {
                add(
                    TableRow.from(
                        R.string.stashapp_aliases,
                        studio.aliases.joinToString(", "),
                    ),
                )
            }

            add(TableRow.from(R.string.stashapp_created_at, parseTimeToString(studio.created_at)))
            add(TableRow.from(R.string.stashapp_updated_at, parseTimeToString(studio.updated_at)))
        }.filterNotNull()
    ItemDetails(
        modifier = modifier,
        imageUrl = studio.image_path,
        tableRows = rows,
        favorite = favorite,
        favoriteClick = favoriteClick,
        rating100 = rating100,
        rating100Click = rating100Click,
    )
}
