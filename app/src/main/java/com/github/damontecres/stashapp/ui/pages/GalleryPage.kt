package com.github.damontecres.stashapp.ui.pages

import android.util.Log
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
import com.github.damontecres.stashapp.api.fragment.GalleryData
import com.github.damontecres.stashapp.api.fragment.TagData
import com.github.damontecres.stashapp.api.type.CriterionModifier
import com.github.damontecres.stashapp.api.type.GalleryFilterType
import com.github.damontecres.stashapp.api.type.ImageFilterType
import com.github.damontecres.stashapp.api.type.MultiCriterionInput
import com.github.damontecres.stashapp.api.type.SceneFilterType
import com.github.damontecres.stashapp.data.DataType
import com.github.damontecres.stashapp.navigation.Destination
import com.github.damontecres.stashapp.suppliers.DataSupplierOverride
import com.github.damontecres.stashapp.suppliers.FilterArgs
import com.github.damontecres.stashapp.ui.ComposeUiConfig
import com.github.damontecres.stashapp.ui.LocalGlobalContext
import com.github.damontecres.stashapp.ui.components.BasicItemInfo
import com.github.damontecres.stashapp.ui.components.ItemDetails
import com.github.damontecres.stashapp.ui.components.ItemOnClicker
import com.github.damontecres.stashapp.ui.components.LongClicker
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
import com.github.damontecres.stashapp.util.name
import com.github.damontecres.stashapp.util.showSetRatingToast
import kotlinx.coroutines.launch

private const val TAG = "GalleryPage"

@Composable
fun GalleryPage(
    server: StashServer,
    id: String,
    itemOnClick: ItemOnClicker<Any>,
    longClicker: LongClicker<Any>,
    uiConfig: ComposeUiConfig,
    modifier: Modifier = Modifier,
) {
    val context = LocalContext.current
    var gallery by remember { mutableStateOf<GalleryData?>(null) }
    var tags by remember { mutableStateOf<List<TagData>>(listOf()) }
    // Remember separately so we don't have refresh the whole page
    var rating100 by remember { mutableIntStateOf(0) }
    LaunchedEffect(id) {
        try {
            val queryEngine = QueryEngine(server)
            gallery = queryEngine.getGallery(id)
            gallery?.let {
                rating100 = it.rating100 ?: 0
                tags = queryEngine.getTags(it.tags.map { it.slimTagData.id })
            }
        } catch (ex: QueryEngine.QueryException) {
            Log.e(TAG, "No gallery found with ID $id", ex)
            Toast.makeText(context, "No gallery found with ID $id", Toast.LENGTH_LONG).show()
        }
    }

    val scope = rememberCoroutineScope()

    val createTab =
        createTabFunc(
            server,
            itemOnClick,
            longClicker,
            uiConfig,
        )

    gallery?.let { gallery ->
        val galleries =
            Optional.present(
                MultiCriterionInput(
                    value = Optional.present(listOf(gallery.id)),
                    modifier = CriterionModifier.INCLUDES_ALL,
                ),
            )
        val uiTabs = getUiTabs(context, DataType.GALLERY)
        val tabs =
            listOf(
                TabProvider(stringResource(R.string.stashapp_details)) {
                    GalleryDetails(
                        modifier = Modifier.fillMaxSize(),
                        uiConfig = uiConfig,
                        gallery = gallery,
                        tags = tags,
                        rating100 = rating100,
                        rating100Click = { newRating100 ->
                            val mutationEngine = MutationEngine(server)
                            scope.launch(StashCoroutineExceptionHandler(autoToast = true)) {
                                val newGallery =
                                    mutationEngine.updateGallery(
                                        galleryId = gallery.id,
                                        rating100 = newRating100,
                                    )
                                if (newGallery != null) {
                                    rating100 = newGallery.rating100 ?: 0
                                    showSetRatingToast(
                                        context,
                                        newGallery.rating100 ?: 0,
                                        server.serverPreferences.ratingsAsStars,
                                    )
                                }
                            }
                        },
                        itemOnClick = itemOnClick,
                        longClicker = longClicker,
                    )
                },
                createTab(
                    FilterArgs(
                        dataType = DataType.IMAGE,
                        findFilter = tabFindFilter(server, PageFilterKey.GALLERY_IMAGES),
                        objectFilter = ImageFilterType(galleries = galleries),
                    ),
                ),
                createTab(
                    FilterArgs(
                        dataType = DataType.SCENE,
                        objectFilter = SceneFilterType(galleries = galleries),
                    ),
                ),
                createTab(
                    FilterArgs(
                        dataType = DataType.PERFORMER,
                        override = DataSupplierOverride.GalleryPerformer(gallery.id),
                    ),
                ),
            ).filter { it.name in uiTabs }
        val title = AnnotatedString(gallery.name ?: "")
        TabPage(title, tabs, modifier)
    }
}

@Composable
fun GalleryDetails(
    gallery: GalleryData,
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
            add(TableRow.from(R.string.stashapp_date, gallery.date))
            add(
                TableRow.from(R.string.stashapp_studio, gallery.studio?.name) {
                    navigationManager.navigate(
                        Destination.Item(
                            DataType.STUDIO,
                            gallery.studio!!.id,
                        ),
                    )
                },
            )
            add(TableRow.from(R.string.stashapp_scene_code, gallery.code))
            add(
                TableRow.from(R.string.stashapp_photographer, gallery.photographer) {
                    navigationManager.navigate(
                        Destination.Filter(
                            filterArgs =
                                FilterArgs(
                                    dataType = DataType.GALLERY,
                                    name = context.getString(R.string.stashapp_photographer) + ": " + gallery.photographer,
                                    objectFilter =
                                        GalleryFilterType(
                                            photographer = stringCriterion(gallery.photographer!!),
                                        ),
                                ),
                        ),
                    )
                },
            )
            add(TableRow.from(R.string.stashapp_description, gallery.details))
        }.filterNotNull()
    ItemDetails(
        modifier = modifier,
        uiConfig = uiConfig,
        imageUrl = gallery.paths.cover,
        tableRows = rows,
        favorite = null,
        favoriteClick = null,
        rating100 = rating100,
        rating100Click = rating100Click,
        tags = tags,
        basicItemInfo = BasicItemInfo(gallery.id, gallery.created_at, gallery.updated_at),
        itemOnClick = itemOnClick,
        longClicker = longClicker,
    )
}
