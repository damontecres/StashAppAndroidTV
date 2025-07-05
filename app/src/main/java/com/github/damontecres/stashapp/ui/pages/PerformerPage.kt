package com.github.damontecres.stashapp.ui.pages

import android.content.res.Configuration
import android.os.Build
import android.util.Log
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Info
import androidx.compose.material.icons.filled.Person
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.getValue
import androidx.compose.runtime.livedata.observeAsState
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.drawWithContent
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.SpanStyle
import androidx.compose.ui.text.buildAnnotatedString
import androidx.compose.ui.text.withStyle
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.DialogProperties
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import androidx.lifecycle.viewmodel.CreationExtras
import androidx.lifecycle.viewmodel.MutableCreationExtras
import androidx.lifecycle.viewmodel.compose.LocalViewModelStoreOwner
import androidx.lifecycle.viewmodel.initializer
import androidx.lifecycle.viewmodel.viewModelFactory
import androidx.tv.material3.Button
import androidx.tv.material3.MaterialTheme
import androidx.tv.material3.ProvideTextStyle
import androidx.tv.material3.Text
import coil3.compose.AsyncImage
import coil3.request.ImageRequest
import coil3.request.crossfade
import com.apollographql.apollo.api.Optional
import com.github.damontecres.stashapp.R
import com.github.damontecres.stashapp.StashApplication
import com.github.damontecres.stashapp.api.fragment.PerformerData
import com.github.damontecres.stashapp.api.fragment.StudioData
import com.github.damontecres.stashapp.api.fragment.TagData
import com.github.damontecres.stashapp.api.type.CircumisedEnum
import com.github.damontecres.stashapp.api.type.CriterionModifier
import com.github.damontecres.stashapp.api.type.GalleryFilterType
import com.github.damontecres.stashapp.api.type.GroupFilterType
import com.github.damontecres.stashapp.api.type.HierarchicalMultiCriterionInput
import com.github.damontecres.stashapp.api.type.ImageFilterType
import com.github.damontecres.stashapp.api.type.MultiCriterionInput
import com.github.damontecres.stashapp.api.type.PerformerFilterType
import com.github.damontecres.stashapp.api.type.SceneFilterType
import com.github.damontecres.stashapp.api.type.SceneMarkerFilterType
import com.github.damontecres.stashapp.api.type.StringCriterionInput
import com.github.damontecres.stashapp.api.type.StudioFilterType
import com.github.damontecres.stashapp.data.DataType
import com.github.damontecres.stashapp.navigation.Destination
import com.github.damontecres.stashapp.navigation.NavigationListener
import com.github.damontecres.stashapp.navigation.NavigationManager
import com.github.damontecres.stashapp.suppliers.FilterArgs
import com.github.damontecres.stashapp.ui.AppTheme
import com.github.damontecres.stashapp.ui.ComposeUiConfig
import com.github.damontecres.stashapp.ui.FontAwesome
import com.github.damontecres.stashapp.ui.GlobalContext
import com.github.damontecres.stashapp.ui.LocalGlobalContext
import com.github.damontecres.stashapp.ui.components.DialogItem
import com.github.damontecres.stashapp.ui.components.DialogPopup
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
import com.github.damontecres.stashapp.ui.components.createTabFunc
import com.github.damontecres.stashapp.ui.components.tabFindFilter
import com.github.damontecres.stashapp.ui.performerPreview
import com.github.damontecres.stashapp.ui.tagPreview
import com.github.damontecres.stashapp.ui.titleCount
import com.github.damontecres.stashapp.ui.uiConfigPreview
import com.github.damontecres.stashapp.util.LoggingCoroutineExceptionHandler
import com.github.damontecres.stashapp.util.MutationEngine
import com.github.damontecres.stashapp.util.PageFilterKey
import com.github.damontecres.stashapp.util.QueryEngine
import com.github.damontecres.stashapp.util.StashServer
import com.github.damontecres.stashapp.util.ageInYears
import com.github.damontecres.stashapp.util.getUiTabs
import com.github.damontecres.stashapp.util.isNotNullOrBlank
import com.github.damontecres.stashapp.util.showSetRatingToast
import kotlinx.coroutines.launch
import kotlin.math.floor
import kotlin.math.round
import kotlin.math.roundToInt

private const val TAG = "PerformerPage"

class PerformerDetailsViewModel(
    server: StashServer,
    val performerId: String,
) : ViewModel() {
    private val queryEngine = QueryEngine(server)
    private val mutationEngine = MutationEngine(server)
    private val exceptionHandler = LoggingCoroutineExceptionHandler(server, viewModelScope)

    private var performer: PerformerData? = null

    val loadingState = MutableLiveData<PerformerLoadingState>(PerformerLoadingState.Loading)
    val tags = MutableLiveData<List<TagData>>(listOf())
    val studios = MutableLiveData<List<StudioData>>(listOf())

    val favorite = MutableLiveData(false)
    val rating100 = MutableLiveData(0)

    fun init(): PerformerDetailsViewModel {
        viewModelScope.launch(exceptionHandler.with("Error fetching performer")) {
            try {
                val performer = queryEngine.getPerformer(performerId)
                if (performer != null) {
                    refresh(performer)
                } else {
                    loadingState.value = PerformerLoadingState.Error
                }
            } catch (ex: Exception) {
                loadingState.value = PerformerLoadingState.Error
            }
        }
        return this
    }

    private suspend fun refresh(performer: PerformerData) {
        rating100.value = performer.rating100 ?: 0
        favorite.value = performer.favorite
        this@PerformerDetailsViewModel.performer = performer

        loadingState.value = PerformerLoadingState.Success(performer)
        if (performer.tags.isNotEmpty()) {
            tags.value =
                queryEngine.getTags(performer.tags.map { it.slimTagData.id })
            Log.v(TAG, "Got ${tags.value?.size} tags")
        }
        studios.value =
            queryEngine.findStudios(
                studioFilter =
                    StudioFilterType(
                        scenes_filter =
                            Optional.present(
                                SceneFilterType(
                                    performers =
                                        Optional.present(
                                            MultiCriterionInput(
                                                value = Optional.present(listOf(performerId)),
                                                modifier = CriterionModifier.INCLUDES_ALL,
                                                excludes = Optional.absent(),
                                            ),
                                        ),
                                ),
                            ),
                    ),
            )
        Log.v(TAG, "Got ${studios.value?.size} studios")
    }

    fun addTag(id: String) = mutateTags { add(id) }

    fun removeTag(id: String) = mutateTags { remove(id) }

    private fun mutateTags(mutator: MutableList<String>.() -> Unit) {
        val ids = tags.value?.map { it.id }
        ids?.let {
            val mutable = it.toMutableList()
            mutator.invoke(mutable)
            viewModelScope.launch(exceptionHandler) {
                val newPerformer = mutationEngine.updatePerformer(performerId, tagIds = mutable)
                if (newPerformer != null) {
                    refresh(newPerformer)
                }
            }
        }
    }

    fun updateRating(rating100: Int) {
        viewModelScope.launch(exceptionHandler) {
            val newRating =
                mutationEngine.updatePerformer(performerId, rating100 = rating100)?.rating100 ?: 0
            this@PerformerDetailsViewModel.rating100.value = newRating
            showSetRatingToast(StashApplication.getApplication(), newRating)
        }
    }

    fun toggleFavorite() {
        viewModelScope.launch(exceptionHandler) {
            val newFavorite =
                mutationEngine
                    .updatePerformer(
                        performerId,
                        favorite = !favorite.value!!,
                    )?.favorite
            this@PerformerDetailsViewModel.favorite.value = newFavorite
        }
    }

    companion object {
        val SERVER_KEY = object : CreationExtras.Key<StashServer> {}
        val PERFORMER_ID_KEY = object : CreationExtras.Key<String> {}
        val Factory: ViewModelProvider.Factory =
            viewModelFactory {
                initializer {
                    val server = this[SERVER_KEY]!!
                    val performerId = this[PERFORMER_ID_KEY]!!
                    PerformerDetailsViewModel(server, performerId).init()
                }
            }
    }
}

sealed class PerformerLoadingState {
    data object Loading : PerformerLoadingState()

    data object Error : PerformerLoadingState()

    data class Success(
        val performer: PerformerData,
    ) : PerformerLoadingState()
}

@Composable
fun PerformerPage(
    server: StashServer,
    id: String,
    itemOnClick: ItemOnClicker<Any>,
    longClicker: LongClicker<Any>,
    uiConfig: ComposeUiConfig,
    modifier: Modifier = Modifier,
) {
    val viewModel =
        ViewModelProvider.create(
            LocalViewModelStoreOwner.current!!,
            PerformerDetailsViewModel.Factory,
            MutableCreationExtras().apply {
                set(PerformerDetailsViewModel.SERVER_KEY, server)
                set(PerformerDetailsViewModel.PERFORMER_ID_KEY, id)
            },
        )[PerformerDetailsViewModel::class]
    val loadingState by viewModel.loadingState.observeAsState()
    val tags by viewModel.tags.observeAsState(listOf())
    val studios by viewModel.studios.observeAsState(listOf())
    val favorite by viewModel.favorite.observeAsState(false)
    val rating100 by viewModel.rating100.observeAsState(0)

    when (val state = loadingState) {
        PerformerLoadingState.Error ->
            Text(
                "Error",
                style = MaterialTheme.typography.displayLarge,
                color = MaterialTheme.colorScheme.onBackground,
            )

        PerformerLoadingState.Loading ->
            Text(
                "Loading...",
                style = MaterialTheme.typography.displayLarge,
                color = MaterialTheme.colorScheme.onBackground,
            )

        is PerformerLoadingState.Success ->
            PerformerDetailsPage(
                server = server,
                perf = state.performer,
                tags = tags,
                studios = studios,
                uiConfig = uiConfig,
                favorite = favorite,
                onFavoriteClick = viewModel::toggleFavorite,
                rating100 = rating100,
                onRatingChange = viewModel::updateRating,
                itemOnClick = itemOnClick,
                longClicker = longClicker,
                modifier = Modifier.fillMaxSize(),
            )

        null -> {}
    }
}

@Composable
fun PerformerDetailsPage(
    server: StashServer,
    perf: PerformerData,
    tags: List<TagData>,
    studios: List<StudioData>,
    uiConfig: ComposeUiConfig,
    favorite: Boolean,
    onFavoriteClick: () -> Unit,
    rating100: Int,
    onRatingChange: (Int) -> Unit,
    itemOnClick: ItemOnClicker<Any>,
    longClicker: LongClicker<Any>,
    modifier: Modifier = Modifier,
) {
    var dialogParams by remember { mutableStateOf<DialogParams?>(null) }

    val performers =
        Optional.present(
            MultiCriterionInput(
                value = Optional.present(listOf(perf.id)),
                modifier = CriterionModifier.INCLUDES_ALL,
            ),
        )
    val uiTabs = getUiTabs(LocalContext.current, DataType.PERFORMER)
    val createTab = createTabFunc(server, itemOnClick, longClicker, uiConfig)
    val tabs =
        listOf(
            TabProvider(stringResource(R.string.stashapp_details)) {
                PerformerDetails(
                    modifier = Modifier.fillMaxSize(),
                    perf = perf,
                    tags = tags,
                    studios = studios,
                    uiConfig = uiConfig,
                    favorite = favorite,
                    favoriteClick = onFavoriteClick,
                    rating100 = rating100,
                    rating100Click = onRatingChange,
                    itemOnClick = itemOnClick,
                    longClicker = longClicker,
                    onShowDialog = { dialogParams = it },
                )
            },
            createTab(
                FilterArgs(
                    dataType = DataType.SCENE,
                    findFilter = tabFindFilter(server, PageFilterKey.PERFORMER_SCENES),
                    objectFilter = SceneFilterType(performers = performers),
                ),
            ),
            createTab(
                FilterArgs(
                    dataType = DataType.GALLERY,
                    findFilter = tabFindFilter(server, PageFilterKey.PERFORMER_GALLERIES),
                    objectFilter = GalleryFilterType(performers = performers),
                ),
            ),
            createTab(
                FilterArgs(
                    dataType = DataType.IMAGE,
                    findFilter = tabFindFilter(server, PageFilterKey.PERFORMER_IMAGES),
                    objectFilter = ImageFilterType(performers = performers),
                ),
            ),
            createTab(
                FilterArgs(
                    dataType = DataType.GROUP,
                    findFilter = tabFindFilter(server, PageFilterKey.PERFORMER_GROUPS),
                    objectFilter = GroupFilterType(performers = performers),
                ),
            ),
            createTab(
                FilterArgs(
                    dataType = DataType.MARKER,
                    findFilter = null,
                    objectFilter = SceneMarkerFilterType(performers = performers),
                ),
            ),
            TabProvider(stringResource(R.string.stashapp_appears_with)) {
                val context = LocalContext.current
                val navigationManager = LocalGlobalContext.current.navigationManager
                StashGridTab(
                    name = stringResource(R.string.stashapp_appears_with),
                    server = server,
                    initialFilter =
                        FilterArgs(
                            dataType = DataType.PERFORMER,
                            findFilter =
                                tabFindFilter(
                                    server,
                                    PageFilterKey.PERFORMER_APPEARS_WITH,
                                ),
                            objectFilter = PerformerFilterType(performers = performers),
                        ),
                    itemOnClick = itemOnClick,
                    longClicker = { item, _ ->
                        item as PerformerData
                        val dialogItems =
                            listOf(
                                DialogItem(context.getString(R.string.go_to), Icons.Default.Info) {
                                    itemOnClick.onClick(item, null)
                                },
                                DialogItem(
                                    context.getString(R.string.scenes_together),
                                    Icons.Default.Person,
                                ) {
                                    val performerIds = listOf(perf.id, item.id)
                                    val name = "${perf.name} & ${item.name}"
                                    val filter =
                                        FilterArgs(
                                            dataType = DataType.SCENE,
                                            name = name,
                                            objectFilter =
                                                SceneFilterType(
                                                    performers =
                                                        Optional.present(
                                                            MultiCriterionInput(
                                                                value =
                                                                    Optional.present(
                                                                        performerIds,
                                                                    ),
                                                                modifier = CriterionModifier.INCLUDES_ALL,
                                                            ),
                                                        ),
                                                ),
                                        )
                                    navigationManager.navigate(
                                        Destination.Filter(
                                            filterArgs = filter,
                                            false,
                                        ),
                                    )
                                },
                            )
                        dialogParams = DialogParams(true, item.name, dialogItems)
                    },
                    composeUiConfig = uiConfig,
                    onFilterChange = {},
                    modifier = Modifier,
                )
            },
        ).filter { it.name in uiTabs }
    val title =
        buildAnnotatedString {
            withStyle(SpanStyle(color = Color.White, fontSize = 40.sp)) {
                append(perf.name)
            }
            if (perf.disambiguation.isNotNullOrBlank()) {
                withStyle(SpanStyle(color = Color.LightGray, fontSize = 24.sp)) {
                    append(" ")
                    append(perf.disambiguation)
                }
            }
        }
    TabPage(title, tabs, DataType.PERFORMER, modifier)
    dialogParams?.let {
        DialogPopup(
            showDialog = true,
            title = it.title,
            dialogItems = it.items,
            onDismissRequest = { dialogParams = null },
            dismissOnClick = true,
            waitToLoad = true,
            properties = DialogProperties(),
        )
    }
}

fun stringCriterion(
    value: String,
    modifier: CriterionModifier = CriterionModifier.EQUALS,
) = Optional.present(
    StringCriterionInput(
        value = value,
        modifier = modifier,
    ),
)

@Composable
fun PerformerDetails(
    perf: PerformerData,
    tags: List<TagData>,
    studios: List<StudioData>,
    favorite: Boolean,
    favoriteClick: () -> Unit,
    rating100: Int,
    rating100Click: (rating100: Int) -> Unit,
    uiConfig: ComposeUiConfig,
    itemOnClick: ItemOnClicker<Any>,
    longClicker: LongClicker<Any>,
    onShowDialog: (DialogParams) -> Unit,
    modifier: Modifier = Modifier,
) {
    val navigationManager = LocalGlobalContext.current.navigationManager
    val context = LocalContext.current
    val navigateTo =
        { stringId: Int, value: String, perfFilter: PerformerFilterType ->
            navigationManager.navigate(
                Destination.Filter(
                    filterArgs =
                        FilterArgs(
                            DataType.PERFORMER,
                            name = context.getString(stringId) + ": " + value,
                            objectFilter = perfFilter,
                        ),
                ),
            )
        }

    val tableRows =
        buildList {
            if (perf.alias_list.isNotEmpty()) {
                add(
                    TableRow.from(
                        R.string.stashapp_aliases,
                        perf.alias_list.joinToString(", "),
                    ),
                )
            }
            if (!perf.birthdate.isNullOrBlank()) {
                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
                    val age = perf.ageInYears.toString()
                    add(TableRow.from(R.string.stashapp_age, "$age (${perf.birthdate})"))
                }
            }
            add(TableRow.from(R.string.stashapp_death_date, perf.death_date))
            add(
                TableRow.from(R.string.stashapp_country, perf.country) {
                    navigateTo(
                        R.string.stashapp_country,
                        perf.country!!,
                        PerformerFilterType(country = stringCriterion(perf.country!!)),
                    )
                },
            )
            add(
                TableRow.from(R.string.stashapp_ethnicity, perf.ethnicity) {
                    navigateTo(
                        R.string.stashapp_ethnicity,
                        perf.ethnicity!!,
                        PerformerFilterType(ethnicity = stringCriterion(perf.ethnicity!!)),
                    )
                },
            )
            add(
                TableRow.from(R.string.stashapp_hair_color, perf.hair_color) {
                    navigateTo(
                        R.string.stashapp_hair_color,
                        perf.hair_color!!,
                        PerformerFilterType(hair_color = stringCriterion(perf.hair_color!!)),
                    )
                },
            )
            add(
                TableRow.from(R.string.stashapp_eye_color, perf.eye_color) {
                    navigateTo(
                        R.string.stashapp_eye_color,
                        perf.eye_color!!,
                        PerformerFilterType(eye_color = stringCriterion(perf.eye_color!!)),
                    )
                },
            )
            if (perf.height_cm != null) {
                val feet = floor(perf.height_cm / 30.48).toInt()
                val inches = (perf.height_cm / 2.54 - feet * 12).roundToInt()
                add(
                    TableRow.from(
                        R.string.stashapp_height,
                        "${perf.height_cm} cm ($feet'$inches\")",
                    ),
                )
            }
            if (perf.weight != null) {
                val pounds = (perf.weight * 2.2).roundToInt()
                add(TableRow.from(R.string.stashapp_weight, "${perf.weight} kg ($pounds lbs)"))
            }
            if (perf.penis_length != null) {
                val inches = round(perf.penis_length / 2.54 * 100) / 100
                add(
                    TableRow.from(
                        R.string.stashapp_penis_length,
                        "${perf.penis_length} cm ($inches\")",
                    ),
                )
            }
            val circString =
                when (perf.circumcised) {
                    CircumisedEnum.CUT -> stringResource(R.string.stashapp_circumcised_types_CUT)
                    CircumisedEnum.UNCUT -> stringResource(R.string.stashapp_circumcised_types_UNCUT)
                    CircumisedEnum.UNKNOWN__, null -> null
                }
            add(TableRow.from(R.string.stashapp_circumcised, circString))

            add(TableRow.from(R.string.stashapp_tattoos, perf.tattoos))
            add(TableRow.from(R.string.stashapp_piercings, perf.piercings))
            add(TableRow.from(R.string.stashapp_career_length, perf.career_length))
        }.filterNotNull()
    Row(
        modifier =
            modifier
                .fillMaxSize(),
    ) {
        if (perf.image_path.isNotNullOrBlank()) {
            val gradientColor = MaterialTheme.colorScheme.background
            AsyncImage(
                modifier =
                    Modifier
                        .padding(8.dp)
                        .fillMaxWidth(.4f)
                        .fillMaxHeight()
                        .drawWithContent {
                            drawContent()
                            drawRect(
                                Brush.verticalGradient(
                                    colors = listOf(Color.Transparent, gradientColor),
                                    startY = 200f,
                                ),
                            )
                            drawRect(
                                Brush.horizontalGradient(
                                    colors = listOf(gradientColor, Color.Transparent),
                                    endX = 200f,
                                    startX = 50f,
                                ),
                            )
                        },
                model =
                    ImageRequest
                        .Builder(LocalContext.current)
                        .data(perf.image_path)
                        .crossfade(true)
                        .build(),
                contentDescription = null,
                contentScale = ContentScale.FillHeight,
            )
        }
        val topPadding = 12.dp
        LazyColumn(
            modifier =
                Modifier
                    .padding(8.dp)
                    .fillMaxWidth(),
        ) {
            val color = if (favorite) Color.Red else Color.LightGray
            item {
                ProvideTextStyle(MaterialTheme.typography.displayLarge.copy(color = color)) {
                    Button(
                        onClick = favoriteClick,
                    ) {
                        Text(text = stringResource(R.string.fa_heart), fontFamily = FontAwesome)
                    }
                }
            }

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

            items(tableRows) { row ->
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
                        modifier = Modifier.padding(top = topPadding),
                    )
                }
            }
            if (studios.isNotEmpty()) {
                item {
                    ItemsRow(
                        title = titleCount(R.string.stashapp_studios, studios),
                        items = studios,
                        uiConfig = uiConfig,
                        itemOnClick = itemOnClick,
                        longClicker = { item, _ ->
                            item as StudioData
                            val dialogItems =
                                listOf(
                                    DialogItem(
                                        context.getString(R.string.go_to),
                                        Icons.Default.Info,
                                    ) {
                                        itemOnClick.onClick(item, null)
                                    },
                                    DialogItem(
                                        context.getString(R.string.stashapp_scenes),
                                        Icons.Default.PlayArrow,
                                    ) {
                                        val title = "${item.name} & ${perf.name}"
                                        val filter =
                                            FilterArgs(
                                                dataType = DataType.SCENE,
                                                name = title,
                                                findFilter = null,
                                                objectFilter =
                                                    SceneFilterType(
                                                        performers =
                                                            Optional.present(
                                                                MultiCriterionInput(
                                                                    value =
                                                                        Optional.presentIfNotNull(
                                                                            listOf(
                                                                                perf.id,
                                                                            ),
                                                                        ),
                                                                    modifier = CriterionModifier.INCLUDES,
                                                                ),
                                                            ),
                                                        studios =
                                                            Optional.present(
                                                                HierarchicalMultiCriterionInput(
                                                                    value =
                                                                        Optional.present(
                                                                            listOf(
                                                                                item.id,
                                                                            ),
                                                                        ),
                                                                    modifier = CriterionModifier.EQUALS,
                                                                ),
                                                            ),
                                                    ),
                                            )
                                        navigationManager.navigate(
                                            Destination.Filter(
                                                filterArgs = filter,
                                                false,
                                            ),
                                        )
                                    },
                                )
                            onShowDialog.invoke(DialogParams(true, item.name, dialogItems))
                        },
                        modifier = Modifier.padding(top = topPadding),
                    )
                }
            }

            item {
                ItemDetailsFooter(
                    id = perf.id,
                    createdAt = perf.created_at.toString(),
                    updatedAt = perf.updated_at.toString(),
                    modifier =
                        Modifier
                            .padding(top = topPadding)
                            .fillMaxWidth(),
                )
            }
        }
    }
}

@Preview(device = "id:tv_1080p", uiMode = Configuration.UI_MODE_TYPE_TELEVISION)
@Composable
private fun PerformerDetailsPreview() {
    val performer = performerPreview
    val itemOnClick =
        ItemOnClicker<Any> { item, filterAndPosition ->
        }
    val longClicker =
        LongClicker<Any> { item, filterAndPosition ->
        }

    CompositionLocalProvider(
        LocalGlobalContext provides
            GlobalContext(
                StashServer("http://0.0.0.0", null),
                object : NavigationManager {
                    override var previousDestination: Destination?
                        get() = TODO("Not yet implemented")
                        set(value) {}

                    override fun navigate(destination: Destination) {
                        TODO("Not yet implemented")
                    }

                    override fun goBack() {
                        TODO("Not yet implemented")
                    }

                    override fun goToMain() {
                        TODO("Not yet implemented")
                    }

                    override fun clearPinFragment() {
                        TODO("Not yet implemented")
                    }

                    override fun addListener(listener: NavigationListener) {
                        TODO("Not yet implemented")
                    }
                },
            ),
    ) {
        AppTheme {
            PerformerDetails(
                perf = performer,
                tags = listOf(tagPreview, tagPreview.copy(id = "723")),
                studios = listOf(),
                favorite = performer.favorite,
                favoriteClick = {},
                rating100 = performer.rating100 ?: 0,
                rating100Click = {},
                uiConfig = uiConfigPreview,
                itemOnClick = itemOnClick,
                longClicker = longClicker,
                onShowDialog = {},
                modifier =
                    Modifier
                        .fillMaxSize()
                        .background(color = MaterialTheme.colorScheme.background),
            )
        }
    }
}
